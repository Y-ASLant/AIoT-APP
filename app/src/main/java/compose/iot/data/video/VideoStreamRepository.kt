package compose.iot.data.video

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import compose.iot.ui.theme.page.video.WebSocketClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import timber.log.Timber
import java.util.concurrent.TimeUnit

data class VideoStreamConfig(
    val serverUrl: String = "",
    val isSecure: Boolean = false,
    val username: String = "",
    val password: String = "",
    val port: String = "8080",
    val path: String = "/video",
) {
    val fullWebSocketUrl: String
        get() =
            buildString {
                append(if (isSecure) "wss://" else "ws://")
                append(serverUrl)
                if (port.isNotEmpty()) {
                    append(":")
                    append(port)
                }
                if (path.isNotEmpty() && !path.startsWith("/")) {
                    append("/")
                }
                append(path)
            }
}

sealed interface VideoStreamEvent {
    data class Frame(
        val bitmap: Bitmap,
        val messageCount: Int,
    ) : VideoStreamEvent

    data class Waiting(
        val messageCount: Int,
        val lastMessage: String,
        val errorMessage: String,
    ) : VideoStreamEvent

    data class Failed(
        val message: String,
    ) : VideoStreamEvent
}

class VideoStreamRepository(
    private val prefs: SharedPreferences,
) {
    fun loadConfig(): VideoStreamConfig =
        VideoStreamConfig(
            serverUrl = prefs.getString("server_url", "") ?: "",
            isSecure = prefs.getBoolean("is_secure", false),
            username = prefs.getString("username", "") ?: "",
            password = prefs.getString("password", "") ?: "",
            port = prefs.getString("port", "8080") ?: "8080",
            path = prefs.getString("path", "/video") ?: "/video",
        )

    fun saveConfig(config: VideoStreamConfig) {
        with(prefs.edit()) {
            putString("server_url", config.serverUrl)
            putBoolean("is_secure", config.isSecure)
            putString("username", config.username)
            putString("password", config.password)
            putString("port", config.port)
            putString("path", config.path)
            apply()
        }
    }

    suspend fun testConnection(url: String): Pair<Boolean, String> = WebSocketClient.testConnection(url)

    fun stream(url: String): Flow<VideoStreamEvent> =
        callbackFlow {
            val client =
                OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()
            var messageCount = 0
            var lastMessage = ""
            var errorMessage = ""
            var lastBitmap: Bitmap? = null

            fun emitWaiting() {
                trySend(VideoStreamEvent.Waiting(messageCount, lastMessage, errorMessage))
            }

            fun updateBitmap(decodedBitmap: Bitmap?) {
                if (decodedBitmap == null) return
                val oldBitmap = lastBitmap
                lastBitmap = decodedBitmap
                oldBitmap?.recycle()
                trySend(VideoStreamEvent.Frame(decodedBitmap, messageCount))
            }

            val listener =
                object : WebSocketListener() {
                    override fun onOpen(
                        webSocket: WebSocket,
                        response: Response,
                    ) {
                        webSocket.send("start")
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {
                        messageCount += 1
                        lastMessage = if (text.length > 100) text.take(100) + "..." else text
                        val bitmap = decodeBitmap(text)
                        if (bitmap != null) {
                            updateBitmap(bitmap)
                        } else {
                            errorMessage = "Unsupported frame format"
                            emitWaiting()
                        }
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        bytes: ByteString,
                    ) {
                        messageCount += 1
                        val byteArray = bytes.toByteArray()
                        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                        if (bitmap != null) {
                            updateBitmap(bitmap)
                        } else {
                            errorMessage = "Unsupported binary frame"
                            emitWaiting()
                        }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?,
                    ) {
                        trySend(VideoStreamEvent.Failed(t.message ?: "Connection failed"))
                    }
                }

            val webSocket = client.newWebSocket(Request.Builder().url(url).build(), listener)
            emitWaiting()

            awaitClose {
                webSocket.close(1000, "Dispose stream")
                lastBitmap?.recycle()
                client.dispatcher.executorService.shutdown()
            }
        }

    private fun decodeBitmap(text: String): Bitmap? {
        return try {
            when {
                text.startsWith("data:image") -> {
                    val base64Data = text.substring(text.indexOf(',') + 1)
                    decodeBase64(base64Data)
                }
                text.startsWith("{") && text.contains("\"image\"") -> {
                    val marker = "\"image\":\""
                    val start = text.indexOf(marker)
                    if (start >= 0) {
                        val base64Data = text.substring(start + marker.length, text.lastIndexOf("\""))
                        decodeBase64(base64Data)
                    } else {
                        null
                    }
                }
                else -> decodeBase64(text)
            }
        } catch (error: Exception) {
            Timber.e(error, "Failed to decode video frame")
            null
        }
    }

    private fun decodeBase64(content: String): Bitmap? {
        val imageBytes = Base64.decode(content, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
