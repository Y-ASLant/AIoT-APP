package compose.iot.ui.theme.page.video

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * WebSocket客户端辅助类，用于测试WebSocket连接
 */
class WebSocketClient {
    companion object {
        private const val TAG = "WebSocketClient"
        private const val TIMEOUT_MS = 5000L // 5秒超时
        
        /**
         * 测试WebSocket连接
         * @param url WebSocket URL
         * @return 包含连接结果和错误信息的Pair
         */
        suspend fun testConnection(url: String): Pair<Boolean, String> {
            return try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .writeTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .build()
                
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                // 使用协程包装WebSocket连接过程
                val result = withTimeoutOrNull(TIMEOUT_MS) {
                    suspendCancellableCoroutine { continuation ->
                        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
                            override fun onOpen(webSocket: WebSocket, response: Response) {
                                Log.d(TAG, "WebSocket连接成功: $url")
                                webSocket.close(1000, "测试完成")
                                continuation.resume(Pair(true, ""))
                            }
                            
                            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                                val errorMsg = t.message ?: "未知错误"
                                Log.e(TAG, "WebSocket连接失败: $errorMsg", t)
                                continuation.resume(Pair(false, errorMsg))
                            }
                            
                            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                                Log.d(TAG, "WebSocket连接已关闭: code=$code, reason=$reason")
                            }
                        })
                        
                        continuation.invokeOnCancellation {
                            Log.d(TAG, "取消WebSocket连接")
                            webSocket.cancel()
                        }
                    }
                }
                
                result ?: Pair(false, "连接超时")
            } catch (e: Exception) {
                Log.e(TAG, "测试WebSocket连接时发生异常", e)
                Pair(false, e.message ?: "连接异常")
            }
        }
    }
} 