package compose.iot.ui.theme.page.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page_VideoStream(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("video_stream_prefs", Context.MODE_PRIVATE)
    }

    var serverUrl by remember { mutableStateOf(sharedPreferences.getString("server_url", "") ?: "") }
    var isSecure by remember { mutableStateOf(sharedPreferences.getBoolean("is_secure", false)) }
    var username by remember { mutableStateOf(sharedPreferences.getString("username", "") ?: "") }
    var password by remember { mutableStateOf(sharedPreferences.getString("password", "") ?: "") }
    var port by remember { mutableStateOf(sharedPreferences.getString("port", "8080") ?: "8080") }
    var path by remember { mutableStateOf(sharedPreferences.getString("path", "/video") ?: "/video") }
    var isUrlTested by remember { mutableStateOf(false) }
    var isConnectionSuccessful by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionErrorMessage by remember { mutableStateOf("") }
    var isStreamingActive by remember { mutableStateOf(false) }

    // 计算完整的WebSocket URL
    val fullWebSocketUrl = buildString {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("视频流配置") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isStreamingActive) {
                // 显示视频流
                VideoStreamView(
                    websocketUrl = fullWebSocketUrl,
                    onError = { error ->
                        isStreamingActive = false
                        connectionErrorMessage = error
                        isUrlTested = true
                        isConnectionSuccessful = false
                    }
                )
            } else {
                // 配置部分
                // 协议选择开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WebSocket协议:",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isSecure,
                        onCheckedChange = { isSecure = it }
                    )
                    Text(
                        text = if (isSecure) "WSS (安全)" else "WS (标准)",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // 服务器地址输入
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        isUrlTested = false
                    },
                    label = { Text("服务器地址") },
                    placeholder = { Text("例如: example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 端口输入
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it
                        isUrlTested = false
                    },
                    label = { Text("端口") },
                    placeholder = { Text("例如: 8080") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 路径输入
                OutlinedTextField(
                    value = path,
                    onValueChange = {
                        path = it
                        isUrlTested = false
                    },
                    label = { Text("路径(可选)") },
                    placeholder = { Text("例如: /stream") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 可选的认证信息
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        isUrlTested = false
                    },
                    label = { Text("用户名 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isUrlTested = false
                    },
                    label = { Text("密码 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                // 显示完整的WebSocket URL预览
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "WebSocket URL 预览:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = fullWebSocketUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // 连接状态显示（如果已测试）
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "正在测试连接...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (isUrlTested) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isConnectionSuccessful)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isConnectionSuccessful) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isConnectionSuccessful)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (isConnectionSuccessful)
                                        "连接成功"
                                    else
                                        "连接失败",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (!isConnectionSuccessful && connectionErrorMessage.isNotEmpty()) {
                                    Text(
                                        text = connectionErrorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // 实际测试WebSocket连接
                            if (serverUrl.isEmpty() || port.isEmpty()) {
                                isUrlTested = true
                                isConnectionSuccessful = false
                                connectionErrorMessage = "服务器地址和端口不能为空"
                                return@OutlinedButton
                            }
                            
                            isTestingConnection = true
                            isUrlTested = false
                            
                            // 使用WebSocketClient测试连接
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    // 调用辅助类进行测试
                                    val (success, errorMsg) = WebSocketClient.testConnection(fullWebSocketUrl)
                                    
                                    // 更新UI状态
                                    withContext(Dispatchers.Main) {
                                        isTestingConnection = false
                                        isUrlTested = true
                                        isConnectionSuccessful = success
                                        connectionErrorMessage = errorMsg
                                    }
                                } catch (e: Exception) {
                                    Log.e("VideoStream", "连接测试异常", e)
                                    // 更新UI状态 - 处理异常
                                    withContext(Dispatchers.Main) {
                                        isTestingConnection = false
                                        isUrlTested = true
                                        isConnectionSuccessful = false
                                        connectionErrorMessage = e.message ?: "未知错误"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isTestingConnection
                    ) {
                        Text("测试连接")
                    }
                    
                    if (isConnectionSuccessful) {
                        Button(
                            onClick = { isStreamingActive = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("开始接收流")
                        }
                    } else {
                        Button(
                            onClick = {
                                // 保存配置
                                with(sharedPreferences.edit()) {
                                    putString("server_url", serverUrl)
                                    putBoolean("is_secure", isSecure)
                                    putString("username", username)
                                    putString("password", password)
                                    putString("port", port)
                                    putString("path", path)
                                    apply()
                                }
                                navController.navigateUp()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("保存配置")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoStreamView(
    websocketUrl: String,
    onError: (String) -> Unit
) {
    val TAG = "VideoStreamView"
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lastMessage by remember { mutableStateOf("") }
    var messageCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf("") }
    
    val webSocketClient = remember { OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build() 
    }
    var webSocket by remember { mutableStateOf<WebSocket?>(null) }
    
    // 在组件挂载时连接WebSocket
    DisposableEffect(websocketUrl) {
        val request = Request.Builder()
            .url(websocketUrl)
            .build()
            
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket连接已打开")
                // 连接建立后可发送一个初始消息来请求数据流开始
                webSocket.send("start")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                messageCount++
                Log.d(TAG, "收到文本消息[$messageCount]: ${if (text.length > 100) text.substring(0, 100) + "..." else text}")
                lastMessage = if (text.length > 100) text.substring(0, 100) + "..." else text
                
                try {
                    // 1. 标准Base64图像格式: data:image/jpeg;base64,...
                    if (text.startsWith("data:image")) {
                        val base64Data = text.substring(text.indexOf(",") + 1)
                        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        if (decodedBitmap != null) {
                            bitmap = decodedBitmap
                            return
                        }
                    } 
                    
                    // 2. JSON格式: {"image": "base64data"}
                    if (text.startsWith("{") && text.contains("\"image\"")) {
                        try {
                            val base64Data = text.substring(text.indexOf("\"image\":\"") + 9, text.lastIndexOf("\""))
                            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                            val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            if (decodedBitmap != null) {
                                bitmap = decodedBitmap
                                return
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON格式解析失败", e)
                        }
                    }
                    
                    // 3. 纯Base64字符串
                    try {
                        val imageBytes = Base64.decode(text, Base64.DEFAULT)
                        val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        if (decodedBitmap != null) {
                            bitmap = decodedBitmap
                            return
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "纯Base64解析失败", e)
                    }
                    
                    // 4. 其他格式处理 - 可根据实际服务器输出格式调整
                    errorMessage = "接收到文本消息，但无法解析为图像：$lastMessage"
                    Log.w(TAG, errorMessage)
                    
                } catch (e: Exception) {
                    errorMessage = "图像解析异常: ${e.message}"
                    Log.e(TAG, "解析图像数据失败", e)
                }
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                messageCount++
                Log.d(TAG, "收到二进制消息[$messageCount]: ${bytes.size}字节")
                
                try {
                    // 直接尝试解析为图像
                    val imageBytes = bytes.toByteArray()
                    val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (decodedBitmap != null) {
                        bitmap = decodedBitmap
                    } else {
                        // 如果直接解析失败，查看是否为JPEG/PNG格式的二进制数据
                        if (isJPEG(imageBytes) || isPNG(imageBytes)) {
                            errorMessage = "收到图像格式数据但解码失败，可能是格式不支持"
                            Log.w(TAG, errorMessage)
                        } else {
                            errorMessage = "收到二进制数据，但不是支持的图像格式"
                            Log.w(TAG, errorMessage)
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "二进制数据解析异常: ${e.message}"
                    Log.e(TAG, "解析二进制图像数据失败", e)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket连接失败", t)
                errorMessage = "连接失败: ${t.message}"
                onError(t.message ?: "连接失败")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket连接已关闭: code=$code, reason=$reason")
            }
        }
        
        webSocket = webSocketClient.newWebSocket(request, listener)
        
        // 组件销毁时关闭WebSocket
        onDispose {
            Log.d(TAG, "关闭WebSocket连接")
            webSocket?.close(1000, "用户离开页面")
            webSocket = null
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (bitmap != null) {
            // 显示视频流图像
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "视频流",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            // 添加消息计数器
            Text(
                text = "已接收 $messageCount 帧画面",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // 显示加载中
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("等待视频流数据...")
                
                if (messageCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "已接收 $messageCount 条消息",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (lastMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "最后消息: $lastMessage",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        )
                    }
                    
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "错误: $errorMessage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .background(MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
        
        // 添加一个关闭按钮
        IconButton(
            onClick = { 
                webSocket?.close(1000, "用户手动关闭")
                webSocket = null
                onError("用户已断开连接")
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), shape = RoundedCornerShape(24.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭视频流",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 调试按钮 - 请求发送一帧测试图像
        IconButton(
            onClick = { 
                webSocket?.send("request_test_frame")
                Log.d(TAG, "已发送测试帧请求")
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), shape = RoundedCornerShape(24.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "请求测试帧",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// 检查字节数组是否为JPEG格式
private fun isJPEG(bytes: ByteArray): Boolean {
    return bytes.size >= 2 && bytes[0].toInt() == 0xFF && bytes[1].toInt() == 0xD8
}

// 检查字节数组是否为PNG格式
private fun isPNG(bytes: ByteArray): Boolean {
    return bytes.size >= 8 && 
           bytes[0].toInt() == 0x89 && 
           bytes[1].toInt() == 0x50 && // P
           bytes[2].toInt() == 0x4E && // N
           bytes[3].toInt() == 0x47 && // G
           bytes[4].toInt() == 0x0D && // CR
           bytes[5].toInt() == 0x0A && // LF
           bytes[6].toInt() == 0x1A && // Ctrl+Z
           bytes[7].toInt() == 0x0A    // LF
}