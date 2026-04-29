package compose.iot.mqtt

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MqttManager {
    private var mqtt3Client: Mqtt3AsyncClient? = null
    private var mqtt5Client: Mqtt5AsyncClient? = null

    private var serverUri = "tcp://broker.emqx.io:1883"
    private var clientId = "ComposeApplication_" + UUID.randomUUID().toString().substring(0, 8)
    private var mqttVersion = 3

    private val topic = "aslant"
    private val subscriptionCallbacks = ConcurrentHashMap<String, (String) -> Unit>()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clientMutex = Mutex()

    @Volatile
    private var intentionalDisconnect = false

    @Volatile
    private var disconnectRequested = false

    private var username: String? = null
    private var password: String? = null

    fun setServerUri(uri: String) {
        serverUri = uri
    }

    fun setClientId(id: String) {
        clientId = id.ifBlank { "ComposeApp_" + UUID.randomUUID().toString().substring(0, 8) }
    }

    fun setUsername(username: String?) {
        this.username = username
    }

    fun setPassword(password: String?) {
        this.password = password
    }

    fun setMqttVersion(version: Int) {
        if (version == 3 || version == 5) {
            mqttVersion = version
        }
    }

    fun connect(
        onConnectComplete: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        ioScope.launch {
            try {
                clientMutex.withLock {
                    if (isConnected() && !disconnectRequested) {
                        withContext(Dispatchers.Main) { onConnectComplete() }
                        return@withLock
                    }

                    disconnectRequested = false
                    mqtt3Client?.takeIf { it.state.isConnected }?.disconnect()
                    mqtt5Client?.takeIf { it.state.isConnected }?.disconnect()

                    val uriString = if (!serverUri.contains("://")) "tcp://$serverUri" else serverUri
                    val uri =
                        try {
                            URI(uriString)
                        } catch (e: Exception) {
                            URI("tcp://broker.emqx.io:1883")
                        }
                    val host = uri.host ?: "broker.emqx.io"
                    val port = if (uri.port != -1) uri.port else 1883

                    val disconnectedListener = { context: MqttClientDisconnectedContext ->
                        if (intentionalDisconnect) {
                            intentionalDisconnect = false
                        } else {
                            val cause = context.cause
                            Timber.e(cause, "Connection lost")
                            ioScope.launch {
                                withContext(Dispatchers.Main) {
                                    onError("连接丢失: ${cause.message ?: "未知错误"}")
                                }
                            }
                        }
                        Unit
                    }

                    if (mqttVersion == 5) {
                        val client =
                            MqttClient.builder()
                                .useMqttVersion5()
                                .identifier(clientId)
                                .serverHost(host)
                                .serverPort(port)
                                .addDisconnectedListener(disconnectedListener)
                                .buildAsync()
                        mqtt5Client = client

                        client.connectWith()
                            .cleanStart(true)
                            .keepAlive(60).let { builder ->
                                val currentUsername = username
                                if (!currentUsername.isNullOrBlank()) {
                                    builder.simpleAuth()
                                        .username(currentUsername)
                                        .password(password?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0))
                                        .applySimpleAuth()
                                } else {
                                    builder
                                }
                            }
                            .send()
                            .whenComplete { _, throwable -> handleConnectResponse(throwable, onConnectComplete, onError) }
                    } else {
                        val client =
                            MqttClient.builder()
                                .useMqttVersion3()
                                .identifier(clientId)
                                .serverHost(host)
                                .serverPort(port)
                                .addDisconnectedListener(disconnectedListener)
                                .buildAsync()
                        mqtt3Client = client

                        client.connectWith()
                            .cleanSession(true)
                            .keepAlive(60).let { builder ->
                                val currentUsername = username
                                if (!currentUsername.isNullOrBlank()) {
                                    builder.simpleAuth()
                                        .username(currentUsername)
                                        .password(password?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0))
                                        .applySimpleAuth()
                                } else {
                                    builder
                                }
                            }
                            .send()
                            .whenComplete { _, throwable -> handleConnectResponse(throwable, onConnectComplete, onError) }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("连接异常: ${e.message}")
                }
            }
        }
    }

    private fun handleConnectResponse(
        throwable: Throwable?,
        onConnectComplete: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (throwable != null) {
            ioScope.launch(Dispatchers.Main) {
                onError("连接失败: ${throwable.message}")
            }
        } else {
            ioScope.launch(Dispatchers.Main) {
                onConnectComplete()
            }
            restoreTopicSubscriptions()
            if (mqttVersion == 5) {
                mqtt5Client?.subscribeWith()
                    ?.topicFilter(topic)
                    ?.callback { publish ->
                        handleGlobalMessage(publish.topic.toString(), publish.payloadAsBytes)
                    }
                    ?.send()
            } else {
                mqtt3Client?.subscribeWith()
                    ?.topicFilter(topic)
                    ?.callback { publish ->
                        handleGlobalMessage(publish.topic.toString(), publish.payloadAsBytes)
                    }
                    ?.send()
            }
        }
    }

    private fun restoreTopicSubscriptions() {
        val subscriptions = subscriptionCallbacks.toMap()
        subscriptions.forEach { (subscribedTopic, callback) ->
            if (subscribedTopic != topic) {
                performSubscribe(subscribedTopic, callback)
            }
        }
    }

    private fun handleGlobalMessage(
        publishTopic: String,
        payload: ByteArray?,
    ) {
        if (payload == null) return
        val messageStr = String(payload, StandardCharsets.UTF_8)
        subscriptionCallbacks[publishTopic]?.let { callback ->
            ioScope.launch { callback(messageStr) }
            return
        }

        subscriptionCallbacks[topic]?.let { callback ->
            ioScope.launch { callback(messageStr) }
        }
    }

    fun disconnect() {
        disconnectRequested = true
        ioScope.launch {
            try {
                clientMutex.withLock {
                    intentionalDisconnect = true
                    mqtt5Client?.takeIf { it.state.isConnected }?.disconnect()
                    mqtt5Client = null

                    mqtt3Client?.takeIf { it.state.isConnected }?.disconnect()
                    mqtt3Client = null
                    disconnectRequested = false
                }
            } catch (e: Exception) {
                intentionalDisconnect = false
                disconnectRequested = false
                Timber.e(e, "Disconnect error")
            }
        }
    }

    fun isConnected(): Boolean {
        return (mqtt5Client?.state?.isConnected == true) || (mqtt3Client?.state?.isConnected == true)
    }

    fun subscribe(
        topic: String,
        onMessageReceived: (String) -> Unit,
    ) {
        ioScope.launch {
            try {
                if (!isConnected()) {
                    connect(
                        onConnectComplete = {
                            performSubscribe(topic, onMessageReceived)
                        },
                        onError = { _ -> },
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
        subscriptionCallbacks[topic] = onMessageReceived

        if (mqttVersion == 5) {
            mqtt5Client?.subscribeWith()
                ?.topicFilter(topic)
                ?.qos(MqttQos.AT_MOST_ONCE)
                ?.callback { publish ->
                    val payload = publish.payloadAsBytes
                    ioScope.launch { onMessageReceived(String(payload, StandardCharsets.UTF_8)) }
                }
                ?.send()
        } else {
            mqtt3Client?.subscribeWith()
                ?.topicFilter(topic)
                ?.qos(MqttQos.AT_MOST_ONCE)
                ?.callback { publish ->
                    val payload = publish.payloadAsBytes
                    ioScope.launch { onMessageReceived(String(payload, StandardCharsets.UTF_8)) }
                }
                ?.send()
        }
    }

    fun unsubscribe(topic: String) {
        ioScope.launch {
            try {
                if (mqttVersion == 5) {
                    mqtt5Client?.unsubscribeWith()?.topicFilter(topic)?.send()
                } else {
                    mqtt3Client?.unsubscribeWith()?.topicFilter(topic)?.send()
                }
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
                if (!isConnected()) {
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
                if (mqttVersion == 5) {
                    mqtt5Client?.publishWith()
                        ?.topic(topic)
                        ?.payload(message.toByteArray(StandardCharsets.UTF_8))
                        ?.qos(MqttQos.AT_MOST_ONCE)
                        ?.send()
                        ?.whenComplete { _, throwable ->
                            if (throwable != null) {
                                ioScope.launch(Dispatchers.Main) { onError("发布失败: ${throwable.message}") }
                            } else {
                                ioScope.launch(Dispatchers.Main) { onComplete() }
                            }
                        }
                } else {
                    mqtt3Client?.publishWith()
                        ?.topic(topic)
                        ?.payload(message.toByteArray(StandardCharsets.UTF_8))
                        ?.qos(MqttQos.AT_MOST_ONCE)
                        ?.send()
                        ?.whenComplete { _, throwable ->
                            if (throwable != null) {
                                ioScope.launch(Dispatchers.Main) { onError("发布失败: ${throwable.message}") }
                            } else {
                                ioScope.launch(Dispatchers.Main) { onComplete() }
                            }
                        }
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
