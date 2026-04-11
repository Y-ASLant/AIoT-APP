
package compose.iot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import compose.iot.AiotApp
import compose.iot.data.preferences.PreferencesManager
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.HomeAssistantManager
import compose.iot.mqtt.SensorHistoryManager
import compose.iot.mqtt.ServerType
import compose.iot.mqtt.SubscriptionCard
import compose.iot.mqtt.cardId
import compose.iot.mqtt.extractEntityId
import compose.iot.ui.theme.page.DeviceSubscriptionController
import compose.iot.ui.theme.page.SubscriptionCardStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

/**
 * IndexPage 的 ViewModel
 *
 * 职责：
 * - 集中管理所有 UI 状态（卡片列表、设备值、对话框状态等）
 * - 路由设备指令到 MQTT 或 HomeAssistant
 * - 管理订阅生命周期
 */
class IndexViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val app = application as AiotApp
    val mqttManager = app.mqttManager
    private val prefsManager: PreferencesManager = app.preferencesManager

    private val haManager = HomeAssistantManager(context)
    private val historyManager = SensorHistoryManager(context)

    // region ── State ──

    private val _uiState = MutableStateFlow(IndexUiState())
    val uiState: StateFlow<IndexUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastToastTime = 0L
    private var topicSubscriptionCount = mapOf<String, Int>()

    // endregion

    // region ── Subscription Controller ──

    private val subscriptionController =
        DeviceSubscriptionController(
            context = context,
            mqttManager = mqttManager,
            haManager = haManager,
            historyManager = historyManager,
            scope = viewModelScope,
            getCards = { _uiState.value.subscriptionCards },
            updateCardValue = { cardId, value ->
                _uiState.update { it.copy(cardValues = it.cardValues + (cardId to value)) }
            },
        )

    // endregion

    // region ── Init ──

    init {
        // 配置 Home Assistant
        val serverUrl = prefsManager.haServerUrl
        val accessToken = prefsManager.haAccessToken
        if (serverUrl.isNotBlank() && accessToken.isNotBlank()) {
            Timber.d("从配置中读取到 HA 配置，初始化连接")
            haManager.setServerConfig(serverUrl, accessToken)
            haManager.setPollingInterval(prefsManager.haPollingInterval)
        }

        // 加载卡片及初始状态
        viewModelScope.launch {
            val cards = app.appDatabase.subscriptionCardDao().getAllCards()
            val actuatorValues = SubscriptionCardStorage.loadActuatorCardValues(context, cards)
            topicSubscriptionCount = cards.groupingBy { it.topic }.eachCount()

            Timber.d("开始初始化执行器状态")
            _uiState.update {
                it.copy(
                    subscriptionCards = cards,
                    cardValues = actuatorValues,
                    selectedDeviceType = prefsManager.selectedDeviceType,
                    continuousSliderMode = prefsManager.sliderContinuousUpdate,
                )
            }

            // 订阅所有主题
            subscriptionController.subscribeAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("ViewModel 销毁，断开 HA 连接")
        subscriptionController.dispose()
    }

    // endregion

    // region ── 设备类型切换 ──

    fun selectDeviceType(type: DeviceType) {
        prefsManager.selectedDeviceType = type
        _uiState.update { it.copy(selectedDeviceType = type) }
    }

    // endregion

    // region ── 对话框管理 ──

    fun showAddDialog() {
        _uiState.update { it.copy(editingCard = null, showSubscribeDialog = true) }
    }

    fun editCard(card: SubscriptionCard) {
        _uiState.update { it.copy(editingCard = card, showSubscribeDialog = true) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showSubscribeDialog = false, editingCard = null) }
    }

    // endregion

    // region ── 历史记录 ──

    fun showHistory(card: SubscriptionCard) {
        viewModelScope.launch {
            val data = historyManager.getHistoryData(card.cardId)
            _uiState.update {
                it.copy(
                    selectedSensorCard = card,
                    sensorHistoryData = data,
                    showHistoryBottomSheet = true,
                )
            }
        }
    }

    fun dismissHistory() {
        _uiState.update { it.copy(showHistoryBottomSheet = false) }
    }

    fun clearHistory() {
        val card = _uiState.value.selectedSensorCard ?: return
        historyManager.clearHistory(card.cardId)
        _uiState.update { it.copy(sensorHistoryData = emptyList()) }
        emitSnackbar("历史记录已清除")
    }

    // endregion

    // region ── 开关控制 ──

    fun toggleSwitch(
        card: SubscriptionCard,
        newState: Boolean,
    ) {
        val cid = card.cardId
        when (card.serverType) {
            ServerType.EMQX -> {
                _uiState.update { it.copy(loadingCards = it.loadingCards + cid) }
                val value = if (newState) card.switchOnValue else card.switchOffValue
                val json = JSONObject().apply { put(card.jsonParam, value) }
                mqttManager.publish(
                    topic = card.topic,
                    message = json.toString(),
                    onComplete = {
                        _uiState.update { s ->
                            s.copy(
                                loadingCards = s.loadingCards - cid,
                                cardValues = s.cardValues + (cid to value),
                            )
                        }
                        prefsManager.saveSwitchState(cid, newState)
                        emitSnackbar("发送成功")
                    },
                    onError = { error ->
                        _uiState.update { it.copy(loadingCards = it.loadingCards - cid) }
                        emitSnackbar("发送失败: $error")
                    },
                )
            }
            ServerType.HomeAssistant -> {
                // 乐观更新
                val storeValue = if (newState) "on" else "off"
                _uiState.update { it.copy(cardValues = it.cardValues + (cid to storeValue)) }
                val entityId = card.extractEntityId()
                haManager.callService(
                    domain = getSwitchDomain(entityId),
                    service = getSwitchService(entityId, newState),
                    entityId = entityId,
                    data =
                        JSONObject().apply {
                            if (entityId.startsWith("number.")) put("value", if (newState) 1 else 0)
                        },
                    onComplete = {
                        prefsManager.saveSwitchState(cid, newState)
                        emitSnackbar("发送成功")
                    },
                    onError = { error ->
                        val revert = if (!newState) "on" else "off"
                        _uiState.update { it.copy(cardValues = it.cardValues + (cid to revert)) }
                        emitSnackbar("发送失败: $error")
                    },
                )
            }
        }
    }

    // endregion

    // region ── 滑块控制 ──

    fun changeSlider(
        card: SubscriptionCard,
        value: Float,
    ) {
        val cid = card.cardId
        when (card.serverType) {
            ServerType.EMQX -> {
                val json = JSONObject().apply { put(card.jsonParam, value) }
                mqttManager.publish(
                    topic = card.topic,
                    message = json.toString(),
                    onComplete = {
                        prefsManager.saveSliderState(cid, value)
                        emitThrottledSnackbar("发送成功")
                    },
                    onError = { error -> emitThrottledSnackbar("发送失败: $error") },
                )
            }
            ServerType.HomeAssistant -> {
                val entityId = card.extractEntityId()
                haManager.callService(
                    domain = getSliderDomain(entityId),
                    service = getSliderService(entityId),
                    entityId = entityId,
                    data = buildSliderData(entityId, value),
                    onComplete = {
                        prefsManager.saveSliderState(cid, value)
                        emitThrottledSnackbar("发送成功")
                    },
                    onError = { error -> emitThrottledSnackbar("发送失败: $error") },
                )
            }
        }
    }

    fun toggleContinuousMode(enabled: Boolean) {
        prefsManager.sliderContinuousUpdate = enabled
        _uiState.update { it.copy(continuousSliderMode = enabled) }
    }

    // endregion

    // region ── 按钮控制 ──

    fun pressButton(
        card: SubscriptionCard,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    ) {
        when (card.serverType) {
            ServerType.EMQX -> {
                val json = JSONObject().apply { put(card.jsonParam, card.buttonValue) }
                mqttManager.publish(
                    topic = card.topic,
                    message = json.toString(),
                    onComplete = {
                        onComplete()
                        emitSnackbar("发送成功")
                    },
                    onError = { error ->
                        onError(error)
                        emitSnackbar("发送失败: $error")
                    },
                )
            }
            ServerType.HomeAssistant -> {
                val entityId = card.extractEntityId()
                haManager.callService(
                    domain = getButtonDomain(entityId),
                    service = "press",
                    entityId = entityId,
                    data = JSONObject(),
                    onComplete = {
                        onComplete()
                        emitSnackbar("发送成功")
                    },
                    onError = { error ->
                        onError(error)
                        emitSnackbar("发送失败: $error")
                    },
                )
            }
        }
    }

    // endregion

    // region ── 输入控制 ──

    fun sendInput(
        card: SubscriptionCard,
        text: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    ) {
        when (card.serverType) {
            ServerType.EMQX -> {
                val json = JSONObject().apply { put(card.jsonParam, text) }
                mqttManager.publish(
                    topic = card.topic,
                    message = json.toString(),
                    onComplete = {
                        onComplete()
                        emitSnackbar("发送成功")
                    },
                    onError = { error ->
                        onError(error)
                        emitSnackbar("发送失败: $error")
                    },
                )
            }
            ServerType.HomeAssistant -> {
                val entityId = card.extractEntityId()
                haManager.callService(
                    domain = getInputDomain(entityId),
                    service = "set_value",
                    entityId = entityId,
                    data = JSONObject().apply { put("value", text) },
                    onComplete = {
                        onComplete()
                        emitSnackbar("发送成功")
                    },
                    onError = { error ->
                        onError(error)
                        emitSnackbar("发送失败: $error")
                    },
                )
            }
        }
    }

    // endregion

    // region ── 卡片 CRUD ──

    fun saveCard(
        card: SubscriptionCard,
        previousCard: SubscriptionCard?,
    ) {
        val state = _uiState.value
        var cards = state.subscriptionCards
        val previousTopic = previousCard?.topic

        // 编辑模式：移除旧卡片
        if (previousCard != null) {
            cards = cards.filter { it != previousCard }
            if (previousTopic != null && previousTopic != card.topic) {
                val count = topicSubscriptionCount[previousTopic] ?: 1
                if (count <= 1) {
                    subscriptionController.unsubscribeTopic(previousTopic)
                    topicSubscriptionCount = topicSubscriptionCount - previousTopic
                } else {
                    topicSubscriptionCount = topicSubscriptionCount + (previousTopic to (count - 1))
                }
            }
        }

        // 重复检查（仅新增传感器时）
        if (previousCard == null &&
            card.deviceType == DeviceType.SENSOR &&
            cards.any { it.topic == card.topic && it.jsonParam == card.jsonParam && it.deviceType == DeviceType.SENSOR }
        ) {
            emitSnackbar("该参数已经被监控")
            return
        }

        // 添加新卡片
        val newCards = cards + card

        // 保存到数据库
        viewModelScope.launch {
            if (previousCard != null) {
                app.appDatabase.subscriptionCardDao().deleteCardById(previousCard.topic, previousCard.jsonParam)
            }
            app.appDatabase.subscriptionCardDao().insertCards(listOf(card))
        }

        // 订阅新主题
        if (!topicSubscriptionCount.containsKey(card.topic)) {
            subscriptionController.subscribeTopic(card.topic)
        }
        val currentCount = topicSubscriptionCount[card.topic] ?: 0
        topicSubscriptionCount = topicSubscriptionCount + (card.topic to (currentCount + 1))

        _uiState.update {
            it.copy(
                subscriptionCards = newCards,
                showSubscribeDialog = false,
                editingCard = null,
            )
        }
        emitSnackbar(if (previousCard != null) "已更新监控参数" else "已添加监控参数")
    }

    fun deleteCard(card: SubscriptionCard) {
        val count = topicSubscriptionCount[card.topic] ?: 1
        if (count <= 1) {
            subscriptionController.unsubscribeTopic(card.topic)
            topicSubscriptionCount = topicSubscriptionCount - card.topic
        } else {
            topicSubscriptionCount = topicSubscriptionCount + (card.topic to (count - 1))
        }

        val cid = card.cardId
        val newCards = _uiState.value.subscriptionCards.filter { it != card }

        viewModelScope.launch {
            app.appDatabase.subscriptionCardDao().deleteCard(card)
        }

        _uiState.update {
            it.copy(
                subscriptionCards = newCards,
                cardValues = it.cardValues - cid,
                showSubscribeDialog = false,
                editingCard = null,
            )
        }
        emitSnackbar("已删除监控卡片")
    }

    // endregion

    // region ── HA Domain/Service 辅助 ──

    private fun getSwitchDomain(entityId: String) =
        when {
            entityId.startsWith("switch.") -> "switch"
            entityId.startsWith("light.") -> "light"
            entityId.startsWith("number.") -> "number"
            entityId.startsWith("button.") -> "button"
            else -> "switch"
        }

    private fun getSwitchService(
        entityId: String,
        newState: Boolean,
    ) =
        when {
            entityId.startsWith("number.") -> "set_value"
            entityId.startsWith("button.") -> "press"
            else -> if (newState) "turn_on" else "turn_off"
        }

    private fun getSliderDomain(entityId: String) =
        when {
            entityId.startsWith("number.") -> "number"
            entityId.startsWith("light.") -> "light"
            entityId.startsWith("input_number.") -> "input_number"
            else -> "number"
        }

    private fun getSliderService(entityId: String) =
        when {
            entityId.startsWith("light.") -> "turn_on"
            else -> "set_value"
        }

    private fun buildSliderData(
        entityId: String,
        value: Float,
    ) =
        JSONObject().apply {
            if (entityId.startsWith("light.")) {
                put("brightness", (value * 255).toInt())
            } else {
                put("value", value)
            }
        }

    private fun getButtonDomain(entityId: String) =
        when {
            entityId.startsWith("button.") -> "button"
            entityId.startsWith("input_button.") -> "input_button"
            else -> "button"
        }

    private fun getInputDomain(entityId: String) =
        when {
            entityId.startsWith("number.") -> "number"
            entityId.startsWith("input_text.") -> "input_text"
            entityId.startsWith("input_number.") -> "input_number"
            else -> "input_text"
        }

    // endregion

    // region ── Snackbar 工具 ──

    private fun emitSnackbar(message: String) {
        _events.trySend(UiEvent.ShowSnackbar(message))
    }

    private fun emitThrottledSnackbar(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastToastTime > 1000) {
            _events.trySend(UiEvent.ShowSnackbar(message))
            lastToastTime = now
        }
    }

    // endregion
}
