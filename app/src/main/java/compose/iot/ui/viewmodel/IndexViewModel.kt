
package compose.iot.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import compose.iot.R
import compose.iot.data.preferences.PreferencesManager
import compose.iot.data.room.SubscriptionCardDao
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.HomeAssistantManager
import compose.iot.mqtt.MqttManager
import compose.iot.mqtt.SensorHistoryManager
import compose.iot.mqtt.ServerType
import compose.iot.mqtt.SubscriptionCard
import compose.iot.ui.theme.page.DeviceSubscriptionController
import compose.iot.ui.theme.page.SubscriptionCardStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

/**
 * IndexPage 的 ViewModel
 *
 * 职责：
 * - 集中管理所有 UI 状态（卡片列表、设备值、对话框状态等）
 * - 路由设备指令到 MQTT 或 HomeAssistant
 * - 管理订阅生命周期
 */
@HiltViewModel
class IndexViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mqttManager: MqttManager,
    private val prefsManager: PreferencesManager,
    private val haManager: HomeAssistantManager,
    private val historyManager: SensorHistoryManager,
    private val subscriptionCardDao: SubscriptionCardDao,
) : ViewModel() {

    // region ── State ──

    private val _uiState =
        MutableStateFlow(
            IndexUiState(
                selectedDeviceType = prefsManager.selectedDeviceType,
                continuousSliderMode = prefsManager.sliderContinuousUpdate,
            ),
        )
    val uiState: StateFlow<IndexUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastSnackbarTime = 0L
    private var lastSnackbarMessage: String? = null
    private var topicSubscriptionCount = mapOf<String, Int>()

    fun isMqttConnected(): Boolean = mqttManager.isConnected()

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

        viewModelScope.launch {
            syncCards(subscriptionCardDao.getAllCards())
            _uiState.update { it.copy(isInitialDataReady = true) }
            subscriptionCardDao.getAllCardsStream().collectLatest(::syncCards)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("ViewModel 销毁，断开相关连接")
        subscriptionController.dispose()
        historyManager.dispose()
    }

    // endregion

    private fun syncCards(cards: List<SubscriptionCard>) {
        val newTopicCounts = cards.groupingBy { it.topic }.eachCount()

        topicSubscriptionCount.keys
            .filter { it !in newTopicCounts }
            .forEach(subscriptionController::unsubscribeTopic)

        newTopicCounts.keys
            .filter { it !in topicSubscriptionCount }
            .forEach(subscriptionController::subscribeTopic)

        topicSubscriptionCount = newTopicCounts

        val currentCardIds = cards.mapTo(mutableSetOf()) { buildCardId(it) }
        val actuatorValues = SubscriptionCardStorage.loadActuatorCardValues(context, cards)

        Timber.d("同步卡片列表，数量=%d", cards.size)
        _uiState.update { state ->
            state.copy(
                subscriptionCards = cards,
                cardValues = state.cardValues.filterKeys { it in currentCardIds } + actuatorValues,
                selectedDeviceType = prefsManager.selectedDeviceType,
                continuousSliderMode = prefsManager.sliderContinuousUpdate,
            )
        }
    }

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
            val data = historyManager.getHistoryData(buildCardId(card))
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
        historyManager.clearHistory(buildCardId(card))
        _uiState.update { it.copy(sensorHistoryData = emptyList()) }
        emitSnackbar(context.getString(R.string.snackbar_history_cleared))
    }

    // endregion

    // region ── 开关控制 ──

    fun toggleSwitch(
        card: SubscriptionCard,
        newState: Boolean,
    ) {
        val cid = buildCardId(card)
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
                        emitSnackbar(context.getString(R.string.snackbar_send_success))
                    },
                    onError = { error ->
                        _uiState.update { it.copy(loadingCards = it.loadingCards - cid) }
                        emitSnackbar(context.getString(R.string.snackbar_send_failed, error))
                    },
                )
            }
            ServerType.HomeAssistant -> {
                // 乐观更新
                val storeValue = if (newState) "on" else "off"
                _uiState.update { it.copy(cardValues = it.cardValues + (cid to storeValue)) }
                val entityId = extractEntityId(card)
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
                        emitSnackbar(context.getString(R.string.snackbar_send_success))
                    },
                    onError = { error ->
                        val revert =
                            when (card.serverType) {
                                ServerType.EMQX -> if (newState) card.switchOffValue else card.switchOnValue
                                ServerType.HomeAssistant -> if (newState) "off" else "on"
                        }
                        _uiState.update { it.copy(cardValues = it.cardValues + (cid to revert)) }
                        emitSnackbar(context.getString(R.string.snackbar_send_failed, error))
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
        val cid = buildCardId(card)
        val shouldNotifySuccess = !_uiState.value.continuousSliderMode
        when (card.serverType) {
            ServerType.EMQX -> {
                val json = JSONObject().apply { put(card.jsonParam, value) }
                mqttManager.publish(
                    topic = card.topic,
                    message = json.toString(),
                    onComplete = {
                        prefsManager.saveSliderState(cid, value)
                        if (shouldNotifySuccess) {
                            emitSnackbar(context.getString(R.string.snackbar_send_success))
                        }
                    },
                    onError = { error -> emitThrottledSnackbar(context.getString(R.string.snackbar_send_failed, error)) },
                )
            }
            ServerType.HomeAssistant -> {
                val entityId = extractEntityId(card)
                haManager.callService(
                    domain = getSliderDomain(entityId),
                    service = getSliderService(entityId),
                    entityId = entityId,
                    data = buildSliderData(entityId, value),
                    onComplete = {
                        prefsManager.saveSliderState(cid, value)
                        if (shouldNotifySuccess) {
                            emitSnackbar(context.getString(R.string.snackbar_send_success))
                        }
                    },
                    onError = { error -> emitThrottledSnackbar(context.getString(R.string.snackbar_send_failed, error)) },
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
                        emitSnackbar(context.getString(R.string.snackbar_send_success))
                    },
                    onError = { error ->
                        onError(error)
                        emitSnackbar(context.getString(R.string.snackbar_send_failed, error))
                    },
                )
            }
            ServerType.HomeAssistant -> {
                val entityId = extractEntityId(card)
                haManager.callService(
                    domain = getButtonDomain(entityId),
                    service = "press",
                    entityId = entityId,
                    data = JSONObject(),
                    onComplete = {
                        onComplete()
                        emitSnackbar(context.getString(R.string.snackbar_send_success))
                    },
                    onError = { error ->
                        onError(error)
                        emitSnackbar(context.getString(R.string.snackbar_send_failed, error))
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
                        emitSnackbar(context.getString(R.string.snackbar_send_success))
                    },
                    onError = { error ->
                        onError(error)
                        emitSnackbar(context.getString(R.string.snackbar_send_failed, error))
                    },
                )
            }
            ServerType.HomeAssistant -> {
                val entityId = extractEntityId(card)
                haManager.callService(
                    domain = getInputDomain(entityId),
                    service = "set_value",
                    entityId = entityId,
                    data = JSONObject().apply { put("value", text) },
                    onComplete = {
                        onComplete()
                        emitSnackbar(context.getString(R.string.snackbar_send_success))
                    },
                    onError = { error ->
                        onError(error)
                        emitSnackbar(context.getString(R.string.snackbar_send_failed, error))
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
        val cards =
            if (previousCard != null) {
                _uiState.value.subscriptionCards.filter { it != previousCard }
            } else {
                _uiState.value.subscriptionCards
            }

        // 重复检查（仅新增传感器时）
        if (previousCard == null &&
            card.deviceType == DeviceType.SENSOR &&
            cards.any { it.topic == card.topic && it.jsonParam == card.jsonParam && it.deviceType == DeviceType.SENSOR }
        ) {
            emitSnackbar(context.getString(R.string.snackbar_param_already_monitored))
            return
        }

        viewModelScope.launch {
            if (previousCard != null) {
                subscriptionCardDao.deleteCardById(previousCard.topic, previousCard.jsonParam)
            }
            subscriptionCardDao.insertCards(listOf(card))
        }

        _uiState.update {
            it.copy(
                showSubscribeDialog = false,
                editingCard = null,
            )
        }
        emitSnackbar(
            context.getString(
                if (previousCard != null) {
                    R.string.snackbar_card_updated
                } else {
                    R.string.snackbar_card_added
                },
            ),
        )
    }

    fun deleteCard(card: SubscriptionCard) {
        viewModelScope.launch {
            subscriptionCardDao.deleteCard(card)
        }

        _uiState.update {
            it.copy(
                showSubscribeDialog = false,
                editingCard = null,
            )
        }
        emitSnackbar(context.getString(R.string.snackbar_card_deleted))
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
        if (shouldSuppressSnackbar(message)) {
            return
        }
        _events.trySend(UiEvent.ShowSnackbar(message))
    }

    private fun emitThrottledSnackbar(message: String) {
        if (shouldSuppressSnackbar(message)) {
            return
        }
        _events.trySend(UiEvent.ShowSnackbar(message))
    }

    private fun shouldSuppressSnackbar(message: String): Boolean {
        val now = System.currentTimeMillis()
        val shouldSuppress = lastSnackbarMessage == message && now - lastSnackbarTime <= 1000
        if (!shouldSuppress) {
            lastSnackbarMessage = message
            lastSnackbarTime = now
        }
        return shouldSuppress
    }

    private fun buildCardId(card: SubscriptionCard): String = "${card.topic}:${card.jsonParam}"

    private fun extractEntityId(card: SubscriptionCard): String =
        card.topic.removePrefix("homeassistant/").removeSuffix("/state")

    // endregion
}
