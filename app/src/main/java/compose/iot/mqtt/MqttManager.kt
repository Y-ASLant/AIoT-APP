package compose.iot.mqtt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import timber.log.Timber
import java.nio.charset.StandardCharsets

class MqttManager() {
    private var mqttClient: MqttClient? = null
    private var serverUri = "tcp://mqtt.aslant.top:1883" // 默认服务器地址
    private var clientId = "ComposeApplication" + System.currentTimeMillis() // 默认Client ID
    private val topic = "aslant"
    private val subscriptionCallbacks = mutableMapOf<String, (String) -> Unit>()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clientMutex = Mutex()

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

    fun connect(
        onConnectComplete: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        ioScope.launch {
            try {
                clientMutex.withLock {
                    if (mqttClient?.isConnected == true) {
                        withContext(Dispatchers.Main) {
                            onConnectComplete()
                        }
                        return@withLock
                    }

                    mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())
                    val options =
                        MqttConnectOptions().apply {
                            isCleanSession = true
                            connectionTimeout = 60
                            keepAliveInterval = 60
                            userName = username
                            password = this@MqttManager.password?.toCharArray()
                        }

                    mqttClient?.setCallback(
                        object : MqttCallback {
                            override fun connectionLost(cause: Throwable?) {
                                Timber.e(cause, "Connection lost")
                                ioScope.launch {
                                    withContext(Dispatchers.Main) {
                                        onError("连接丢失: ${cause?.message ?: "未知错误"}")
                                    }
                                }
                            }

                            override fun messageArrived(
                                topic: String?,
                                message: MqttMessage?,
                            ) {
                                message?.payload?.let { payload ->
                                    val messageStr = String(payload, StandardCharsets.UTF_8)
                                    topic?.let { subscriptionCallbacks[it] }?.let { callback ->
                                        ioScope.launch {
                                            callback(messageStr)
                                        }
                                    }
                                }
                            }

                            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                                Timber.d("Message delivered")
                            }
                        },
                    )

                    mqttClient?.connect(options)
                    mqttClient?.subscribe(topic)
                }

                withContext(Dispatchers.Main) {
                    onConnectComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("连接失败: ${e.message}")
                }
            }
        }
    }

    fun disconnect() {
        ioScope.launch {
            try {
                clientMutex.withLock {
                    if (mqttClient?.isConnected == true) {
                        mqttClient?.disconnect()
                    }
                    mqttClient?.close()
                    mqttClient = null
                }
            } catch (e: Exception) {
                Timber.e(e, "Disconnect error")
            }
        }
    }

    fun isConnected(): Boolean = mqttClient?.isConnected == true

    fun subscribe(
        topic: String,
        onMessageReceived: (String) -> Unit,
    ) {
        ioScope.launch {
            try {
                if (mqttClient?.isConnected != true) {
                    connect(
                        onConnectComplete = {
                            performSubscribe(topic, onMessageReceived)
                        },
                        onError = { _ ->
                            // 处理连接错误
                        },
                    )
                } else {
                    performSubscribe(topic, onMessageReceived)
                }
            } catch (e: Exception) {
                Timber.e(e, "Subscribe error")
            }
        }
    }

    private fun performSubscribe(
        topic: String,
        onMessageReceived: (String) -> Unit,
    ) {
        mqttClient?.subscribe(topic, 0) { _, message ->
            val messageStr = String(message.payload, StandardCharsets.UTF_8)
            ioScope.launch {
                onMessageReceived(messageStr)
            }
        }
        subscriptionCallbacks[topic] = onMessageReceived
    }

    fun unsubscribe(topic: String) {
        ioScope.launch {
            try {
                mqttClient?.unsubscribe(topic)
                subscriptionCallbacks.remove(topic)
            } catch (e: Exception) {
                Timber.e(e, "Unsubscribe error")
            }
        }
    }

    fun publish(
        topic: String,
        message: String,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        ioScope.launch {
            try {
                if (mqttClient?.isConnected != true) {
                    connect(
                        onConnectComplete = {
                            performPublish(topic, message, onComplete, onError)
                        },
                        onError = { error ->
                            onError(error)
                        },
                    )
                } else {
                    performPublish(topic, message, onComplete, onError)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("发布失败: ${e.message}")
                }
            }
        }
    }

    private fun performPublish(
        topic: String,
        message: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    ) {
        ioScope.launch {
            try {
                val mqttMessage = MqttMessage(message.toByteArray())
                mqttMessage.qos = 0
                mqttClient?.publish(topic, mqttMessage)
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("发布失败: ${e.message}")
                }
            }
        }
    }

    fun release() {
        disconnect()
        ioScope.cancel()
    }
}
