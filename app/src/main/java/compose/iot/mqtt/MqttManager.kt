package compose.iot.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets

class MqttManager() {
    private var mqttClient: MqttClient? = null
    private var serverUri = "tcp://mqtt.aslant.top:1883" // 默认服务器地址
    private var clientId = "ComposeApplication" + System.currentTimeMillis() // 默认Client ID
    private val topic = "aslant"
    private val subscriptionCallbacks = mutableMapOf<String, (String) -> Unit>()

    private var username: String? = null
    private var password: String? = null

    fun setServerUri(uri: String) {
        serverUri = uri
    }

    fun setClientId(id: String) {
        clientId = id
    }

    fun setUsername(username: String?) {
        this.username = username
    }

    fun setPassword(password: String?) {
        this.password = password
    }

    fun connect(onConnectComplete: () -> Unit = {}, onError: (String) -> Unit = {}) {
        try {
            if (mqttClient?.isConnected == true) {
                onConnectComplete()
                return
            }

            mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())
            val options = MqttConnectOptions()
            options.isCleanSession = true
            options.connectionTimeout = 60
            options.keepAliveInterval = 60
            options.userName = username
            options.password = password?.toCharArray()

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "Connection lost", cause)
                    onError("连接丢失: ${cause?.message ?: "未知错误"}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    message?.payload?.let { payload ->
                        val messageStr = String(payload, StandardCharsets.UTF_8)
                        subscriptionCallbacks[topic]?.invoke(messageStr)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MQTT", "Message delivered")
                }
            })

            mqttClient?.connect(options)
            mqttClient?.subscribe(topic)
            onConnectComplete()
        } catch (e: Exception) {
            onError("连接失败: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            if (mqttClient?.isConnected == true) {
                mqttClient?.disconnect()
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Disconnect error", e)
        }
    }

    fun isConnected(): Boolean = mqttClient?.isConnected == true

    fun subscribe(topic: String, onMessageReceived: (String) -> Unit) {
        try {
            if (mqttClient?.isConnected != true) {
                connect(
                    onConnectComplete = {
                        performSubscribe(topic, onMessageReceived)
                    },
                    onError = { error ->
                        // 处理连接错误
                    }
                )
            } else {
                performSubscribe(topic, onMessageReceived)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun performSubscribe(topic: String, onMessageReceived: (String) -> Unit) {
        mqttClient?.subscribe(topic, 0) { _, message ->
            val messageStr = String(message.payload, StandardCharsets.UTF_8)
            onMessageReceived(messageStr)
        }
        subscriptionCallbacks[topic] = onMessageReceived
    }

    fun unsubscribe(topic: String) {
        try {
            mqttClient?.unsubscribe(topic)
            subscriptionCallbacks.remove(topic)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun publish(
        topic: String,
        message: String,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            if (mqttClient?.isConnected != true) {
                connect(
                    onConnectComplete = {
                        performPublish(topic, message, onComplete, onError)
                    },
                    onError = { error ->
                        onError(error)
                    }
                )
            } else {
                performPublish(topic, message, onComplete, onError)
            }
        } catch (e: Exception) {
            onError("发布失败: ${e.message}")
        }
    }

    private fun performPublish(
        topic: String,
        message: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttMessage.qos = 0
            mqttClient?.publish(topic, mqttMessage)
            onComplete()
        } catch (e: Exception) {
            onError("发布失败: ${e.message}")
        }
    }
} 