package compose.iot.ui.theme.page

import android.content.Context
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.HomeAssistantManager
import compose.iot.mqtt.MqttManager
import compose.iot.mqtt.SensorHistoryManager
import compose.iot.mqtt.SubscriptionCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

internal class DeviceSubscriptionController(
    private val context: Context,
    private val mqttManager: MqttManager,
    private val haManager: HomeAssistantManager,
    private val historyManager: SensorHistoryManager,
    private val scope: CoroutineScope,
    private val getCards: () -> List<SubscriptionCard>,
    private val updateCardValue: (String, String) -> Unit,
) {
    fun subscribeAll() {
        Timber.d("开始订阅所有主题")
        getCards().map { it.topic }.distinct().forEach(::subscribeTopic)
    }

    fun subscribeTopic(topic: String) {
        Timber.d("处理主题: $topic")
        if (topic.startsWith("homeassistant/")) {
            subscribeHomeAssistantTopic(topic)
        } else {
            subscribeMqttTopic(topic)
        }
    }

    fun unsubscribeTopic(topic: String) {
        if (topic.startsWith("homeassistant/")) {
            val entityId = topic.removePrefix("homeassistant/").removeSuffix("/state")
            haManager.unsubscribe(entityId)
        } else {
            mqttManager.unsubscribe(topic)
        }
    }

    fun dispose() {
        haManager.disconnect()
    }

    private fun subscribeHomeAssistantTopic(topic: String) {
        Timber.d("发现 HA 主题，使用 HA 管理器订阅")
        val entityId = topic.removePrefix("homeassistant/").removeSuffix("/state")

        scope.launch {
            try {
                val initialState = haManager.fetchEntityState(entityId)
                if (initialState != null) {
                    Timber.d("获取到 $entityId 的初始状态: $initialState")
                    getCards()
                        .filter { it.topic == topic }
                        .forEach { card ->
                            val cardId = "${card.topic}:${card.jsonParam}"
                            updateCardValue(cardId, initialState)
                            SubscriptionCardStorage.persistActuatorValue(context, card, cardId, initialState)
                        }
                }
            } catch (e: Exception) {
                Timber.e(e, "获取 $entityId 初始状态失败")
            }
        }

        haManager.subscribe(entityId) { message ->
            handleJsonMessage(
                topic = topic,
                message = message,
                errorTag = "HA_REST",
                errorMessage = "处理 HA 消息失败",
            )
        }
    }

    private fun subscribeMqttTopic(topic: String) {
        Timber.d("发现 MQTT 主题，使用 MQTT 管理器订阅")
        mqttManager.subscribe(topic) { message ->
            handleJsonMessage(
                topic = topic,
                message = message,
                errorTag = "MQTT_Error",
                errorMessage = "处理MQTT消息失败",
            )
        }
    }

    private fun handleJsonMessage(
        topic: String,
        message: String,
        errorTag: String,
        errorMessage: String,
    ) {
        try {
            val json = JSONObject(message)
            getCards()
                .filter { it.topic == topic }
                .forEach { card ->
                    if (json.has(card.jsonParam)) {
                        val value = json.optString(card.jsonParam)
                        val cardId = "${card.topic}:${card.jsonParam}"
                        updateCardValue(cardId, value)

                        if (card.deviceType == DeviceType.SENSOR && value != "NULL") {
                            historyManager.addData(cardId, value, card.unitSuffix)
                        }

                        SubscriptionCardStorage.persistActuatorValue(context, card, cardId, value)
                    }
                }
        } catch (e: Exception) {
            Timber.e(e, errorMessage)
        }
    }
}
