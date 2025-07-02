package compose.iot.ui.theme.page

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.iot.R
import androidx.core.net.toUri
import compose.iot.ui.theme.function.standardEnterTransition
import compose.iot.ui.theme.function.standardExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.util.Log
import android.widget.Toast
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun Page_About(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isVisible by remember { mutableStateOf(true) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
    }
    
    @Suppress("DEPRECATION")
    val versionCode = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString() ?: "-"
        } else {
            packageInfo?.versionCode?.toString() ?: "-"
        }
    }
    
    val currentVersionCode = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toInt() ?: 0
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode ?: 0
        }
    }
    
    val currentVersionName = packageInfo?.versionName ?: "1.2.0"

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = standardEnterTransition(initialOffsetY = -50),
            exit = standardExitTransition(targetOffsetY = -50)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .padding(top = 40.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(60.dp))
                
                // 应用标题
                Text(
                    text = "AIOT Compose",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "物联网数据监控平台",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // 作者信息卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "App作者",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "作者：ASLant\n专业：物联网工程技术\n学校：山东工程职业技术大学",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 关于软件卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = {
                        navController.navigate("changelog") {
                            launchSingleTop = true
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "关于此工具",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• ✨使用 Jetpack Compose + Kotlin 开发\n• 💫UI 采用 Material Design 3 设计语言\n• 🍀支持 MQTT 和 Home Assistant 协议\n• 🔍支持实时数据监控和可视化\n• 🗼提供丰富的Card自定义选项\n• 🛠️支持手动添加传感/执行器设备\n• 🎉点击进入历史版本更新记录",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://aslant.top/Cloud/OneDrive/?login=ASLant".toUri())
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.upload),
                                    contentDescription = "云盘",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://aslant.top".toUri())
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.data),
                                    contentDescription = "主页",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://aslant.top/Cloud/".toUri())
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.download),
                                    contentDescription = "下载",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                // 版本信息
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "AIOT Version $currentVersionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "更新日期: $versionCode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} ASLant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (!isCheckingUpdate) {
                                isCheckingUpdate = true
                                // 检查网络连接
                                if (!isNetworkAvailable(context)) {
                                    updateMessage = "网络连接不可用，请检查网络设置"
                                    isCheckingUpdate = false
                                    return@Button
                                }
                                
                                coroutineScope.launch {
                                    try {
                                        Log.d("UpdateCheck", "开始检查更新，当前版本: $currentVersionName")
                                        val result = checkForUpdates(currentVersionName, currentVersionCode)
                                        if (result != null) {
                                            Log.d("UpdateCheck", "发现新版本: ${result.versionName}")
                                            updateInfo = result
                                            showUpdateDialog = true
                                        } else {
                                            Log.d("UpdateCheck", "没有发现新版本")
                                            updateMessage = "已是最新版本"
                                        }
                                    } catch (e: Exception) {
                                        Log.e("UpdateCheck", "检查更新失败", e)
                                        updateMessage = "检查更新失败: ${e.message ?: "未知错误"}"
                                    } finally {
                                        isCheckingUpdate = false
                                    }
                                }
                            }
                        },
                        enabled = !isCheckingUpdate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.download),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isCheckingUpdate) "检查中..." else "检查更新")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // 更新提示对话框
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("发现新版本！") },
            text = { 
                Column {
                    Text("当前版本: $currentVersionName (版本号: $currentVersionCode)")
                    Text("最新版本: ${updateInfo?.versionName ?: ""} (版本号: ${updateInfo?.versionCode ?: ""})")
                    Text("安装包大小: ${updateInfo?.apkSize ?: "未知"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("更新内容:")
                    Text(updateInfo?.description ?: "")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://aslant-api.cn/d/YASLant/apk/release/latest.apk".toUri())
                        context.startActivity(intent)
                        showUpdateDialog = false
                    }
                ) {
                    Text("立即更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("稍后再说")
                }
            }
        )
    }
    
    // 无更新或出错提示
    if (updateMessage.isNotEmpty()) {
        LaunchedEffect(updateMessage) {
            Toast.makeText(context, updateMessage, Toast.LENGTH_SHORT).show()
            updateMessage = ""
        }
    }
}

// 更新信息数据类
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val description: String,
    val downloadUrl: String,
    val apkSize: String = "未知" // APK大小，默认为"未知"
)

// 检查更新函数
suspend fun checkForUpdates(currentVersion: String, currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
    Log.d("UpdateCheck", "进入checkForUpdates函数，当前版本: $currentVersion, 版本号: $currentVersionCode")
    return@withContext try {
        // 实际API调用
        val url = URL("https://aslant.top/version.json")
        Log.d("UpdateCheck", "请求URL: $url")
        
        val connection = url.openConnection()
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        
        val inputStream = connection.getInputStream()
        val response = inputStream.bufferedReader().use { it.readText() }
        Log.d("UpdateCheck", "API响应: $response")
        
        val jsonObject = JSONObject(response)
        val latestVersion = jsonObject.getString("versionName")
        val versionCode = jsonObject.getInt("versionCode")
        val description = jsonObject.getString("description")
        val downloadUrl = jsonObject.getString("downloadUrl")
        
        // 获取APK文件大小
        val apkSize = try {
            val apkUrl = URL(downloadUrl)
            val apkConnection = apkUrl.openConnection()
            apkConnection.connectTimeout = 5000
            apkConnection.readTimeout = 5000
            apkConnection.setRequestProperty("Accept-Encoding", "identity")
            apkConnection.connect()
            
            val contentLength = apkConnection.contentLength
            if (contentLength > 0) {
                formatFileSize(contentLength.toLong())
            } else {
                "未知"
            }
        } catch (e: Exception) {
            Log.e("UpdateCheck", "获取APK大小失败", e)
            "未知"
        }
        
        Log.d("UpdateCheck", "解析结果 - 最新版本: $latestVersion, 当前版本: $currentVersion, 最新版本号: $versionCode, 当前版本号: $currentVersionCode, APK大小: $apkSize")
        
        // 比较版本号和版本Code
        val isNewer = isNewerVersion(currentVersion, latestVersion) || versionCode > currentVersionCode
        Log.d("UpdateCheck", "版本比较结果: $isNewer")
        
        if (isNewer) {
            UpdateInfo(latestVersion, versionCode, description, downloadUrl, apkSize)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("UpdateCheck", "检查更新出错", e)
        throw e
    }
}

// 版本比较函数
fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
    val current = currentVersion.split(".").mapNotNull { it.toIntOrNull() }
    val latest = latestVersion.split(".").mapNotNull { it.toIntOrNull() }
    
    for (i in 0 until minOf(current.size, latest.size)) {
        if (latest[i] > current[i]) return true
        if (latest[i] < current[i]) return false
    }
    
    return latest.size > current.size
}

// 检查网络连接
fun isNetworkAvailable(context: android.content.Context): Boolean {
    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

// 格式化文件大小
@SuppressLint("DefaultLocale")
fun formatFileSize(size: Long): String {
    if (size <= 0) return "0B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return String.format("%.2f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}
