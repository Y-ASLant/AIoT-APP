package compose.iot.mqtt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeAssistantManager(private val context: Context) {
    private var serverUrl: String = ""
    private var accessToken: String = ""
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val pollingJobs = mutableMapOf<String, Job>()
    private var pollingInterval: Long = 500 // 默认0.5秒
    
    // 添加状态变化监听器集合
    private val stateChangeListeners = mutableMapOf<String, MutableList<(String, String) -> Unit>>()
    
    // 添加状态变化监听器
    fun addStateChangeListener(entityId: String, listener: (String, String) -> Unit) {
        if (!stateChangeListeners.containsKey(entityId)) {
            stateChangeListeners[entityId] = mutableListOf()
        }
        stateChangeListeners[entityId]?.add(listener)
    }
    
    // 移除状态变化监听器
    fun removeStateChangeListener(entityId: String, listener: (String, String) -> Unit) {
        stateChangeListeners[entityId]?.remove(listener)
        if (stateChangeListeners[entityId]?.isEmpty() == true) {
            stateChangeListeners.remove(entityId)
        }
    }
    
    // 通知状态变化
    private fun notifyStateChange(entityId: String, state: String) {
        val cardId = "homeassistant/$entityId/state:state"
        stateChangeListeners[entityId]?.forEach { listener ->
            listener(cardId, state)
        }
    }

    fun setPollingInterval(seconds: Int) {
        pollingInterval = seconds * 1000L
    }

    fun setServerConfig(url: String, token: String) {
        serverUrl = url.trim()
        accessToken = token
        
        // 确保URL格式正确
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            serverUrl = "http://$serverUrl"
        }
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.dropLast(1)
        }
    }

    fun subscribe(entityId: String, onMessageReceived: (String) -> Unit) {
        Log.d("HA_REST", "开始订阅实体: $entityId")
        
        // 如果已经存在轮询任务，先检查其状态
        pollingJobs[entityId]?.let { existingJob ->
            if (existingJob.isActive) {
                Log.d("HA_REST", "实体 $entityId 已有活跃的订阅，跳过重复订阅")
                return
            }
        }
        
        // 创建新的轮询任务
        val job = scope.launch {
            var lastState: String? = null
            var retryCount = 0
            val maxRetries = 3
            
            while (isActive) {
                try {
                    val state = fetchEntityState(entityId)
                    if (state != null) {
                        // 重置重试计数
                        retryCount = 0
                        
                        // 只有当状态发生变化时才通知UI
                        if (state != lastState) {
                            Log.d("HA_REST", "实体 $entityId 状态变化: $lastState -> $state")
                            val json = JSONObject().put("state", state)
                            onMessageReceived(json.toString())
                            lastState = state
                            
                            // 更新本地存储的状态
                            val cardId = "homeassistant/$entityId/state:state"
                            // 根据实体类型决定存储位置
                            if (entityId.startsWith("number.") || entityId.startsWith("input_number.") || 
                                entityId.startsWith("light.") && state.toFloatOrNull() != null) {
                                // 对于数值类型实体，存储为浮点数
                                try {
                                    val floatValue = state.toFloatOrNull() ?: (if (state == "on") 1.0f else 0.0f)
                                    context.getSharedPreferences("slider_states", Context.MODE_PRIVATE)
                                        .edit()
                                        .apply {
                                            putFloat(cardId, floatValue)
                                            apply()
                                        }
                                    
                                    // 通知状态变化
                                    notifyStateChange(entityId, floatValue.toString())
                                } catch (e: Exception) {
                                    Log.e("HA_REST", "无法将状态转换为浮点数: $state", e)
                                }
                            } else {
                                // 对于开关类型实体，存储为布尔值
                                val boolState = state == "on"
                                context.getSharedPreferences("switch_states", Context.MODE_PRIVATE)
                                    .edit()
                                    .apply {
                                        putBoolean(cardId, boolState)
                                        apply()
                                    }
                                
                                // 通知状态变化
                                notifyStateChange(entityId, state)
                            }
                        }
                    } else {
                        // 状态获取失败，增加重试计数
                        retryCount++
                        Log.w("HA_REST", "获取实体 $entityId 状态失败，重试次数: $retryCount")
                        
                        // 如果重试次数超过最大值，发送一个默认状态
                        if (retryCount >= maxRetries) {
                            Log.e("HA_REST", "获取实体 $entityId 状态失败，已达到最大重试次数")
                            val defaultState = "unknown"
                            val json = JSONObject().put("state", defaultState)
                            onMessageReceived(json.toString())
                            lastState = defaultState
                            retryCount = 0  // 重置重试计数，继续尝试
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HA_REST", "获取实体状态异常: $entityId", e)
                    retryCount++
                    
                    // 如果重试次数超过最大值，发送一个默认状态
                    if (retryCount >= maxRetries) {
                        Log.e("HA_REST", "获取实体 $entityId 状态异常，已达到最大重试次数")
                        val defaultState = "unknown"
                        val json = JSONObject().put("state", defaultState)
                        onMessageReceived(json.toString())
                        lastState = defaultState
                        retryCount = 0  // 重置重试计数，继续尝试
                    }
                }
                
                // 根据重试次数调整轮询间隔
                val currentInterval = if (retryCount > 0) {
                    // 重试时使用更短的间隔
                    minOf(pollingInterval / 2, 1000L)
                } else {
                    pollingInterval
                }
                
                delay(currentInterval) // 按设定的间隔轮询
            }
        }
        
        pollingJobs[entityId] = job
    }

    suspend fun fetchEntityState(entityId: String): String? = withContext(Dispatchers.IO) {
        if (serverUrl.isBlank() || accessToken.isBlank()) {
            Log.e("HA_REST", "服务器配置无效")
            return@withContext null
        }

        try {
            val url = URL("$serverUrl/api/states/$entityId")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = true
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val state = json.getString("state")
                    Log.d("HA_REST", "成功获取实体 $entityId 的状态: $state")
                    return@withContext state
                } else {
                    val errorStream = connection.errorStream
                    val errorMessage = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("HA_REST", "获取实体状态失败: HTTP $responseCode - $errorMessage")
                    return@withContext null
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e("HA_REST", "获取实体状态异常: ${e.message}", e)
            return@withContext null
        }
    }

    fun disconnect() {
        Log.d("HA_REST", "正在断开所有连接")
        pollingJobs.values.forEach { it.cancel() }
        pollingJobs.clear()
        scope.cancel()
    }

    fun callService(
        domain: String,
        service: String,
        entityId: String,
        data: JSONObject? = null,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // 验证服务器配置
        if (serverUrl.isBlank() || accessToken.isBlank()) {
            onError("Home Assistant 服务器未配置")
            return
        }

        // 为每个请求创建新的 scope
        val requestScope = CoroutineScope(Dispatchers.IO + Job())
        requestScope.launch {
            try {
                val url = URL("$serverUrl/api/services/$domain/$service")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                try {
                    // 构建请求数据
                    val requestData = JSONObject().apply {
                        put("entity_id", entityId)
                        data?.let { dataObj ->
                            val keys = dataObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                put(key, dataObj.get(key))
                            }
                        }
                    }

                    // 发送请求
                    connection.outputStream.use { os ->
                        os.write(requestData.toString().toByteArray())
                    }

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        withContext(Dispatchers.Main) {
                            onComplete()
                        }
                    } else {
                        val errorStream = connection.errorStream
                        val errorMessage = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                        Log.e("HA_REST", "调用服务失败: HTTP $responseCode - $errorMessage")
                        withContext(Dispatchers.Main) {
                            onError("HTTP $responseCode - $errorMessage")
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Log.e("HA_REST", "调用服务异常", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            } finally {
                requestScope.cancel()
            }
        }
    }
}