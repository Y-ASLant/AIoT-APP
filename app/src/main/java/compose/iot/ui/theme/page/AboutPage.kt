package compose.iot.ui.theme.page

import android.annotation.SuppressLint
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import compose.iot.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import kotlin.math.log10
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val packageInfo =
        remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) {
                null
            }
        }

    @Suppress("DEPRECATION")
    val versionCode =
        remember {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode?.toString() ?: "-"
            } else {
                packageInfo?.versionCode?.toString() ?: "-"
            }
        }

    val currentVersionCode =
        remember {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode?.toInt() ?: 0
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.versionCode ?: 0
            }
        }

    val currentVersionName = packageInfo?.versionName ?: "0.0.0"

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    compose.iot.ui.components.AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        title = "关于",
        navController = navController,
        snackbarHostState = snackbarHostState,
        showBackButton = false,
        scrollBehavior = scrollBehavior
    ) { _ ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 应用标题
            Text(
                text = "AIOT Compose",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "物联网数据监控平台",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            // 作者信息卡片
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape = MaterialTheme.shapes.large,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Y-ASLant/AIoT-APP".toUri())
                    context.startActivity(intent)
                },
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "作者：ASLant",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "github.com/Y-ASLant/AIoT-APP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.github),
                        contentDescription = "GitHub",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 关于软件卡片
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape = MaterialTheme.shapes.large,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "关于此工具",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text =
                            "✨ Jetpack Compose + Kotlin\n" +
                                "💫 Material Design 3\n" +
                                "🍀 MQTT & Home Assistant\n" +
                                "🔍 实时数据监控与可视化\n" +
                                "🗼 丰富的 Card 自定义选项\n" +
                                "🛠️ 手动添加传感 / 执行器设备",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 更新日志卡片
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape = MaterialTheme.shapes.large,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                onClick = {
                    navController.navigate("changelog") {
                        launchSingleTop = true
                    }
                },
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "更新日志",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "查看更新日志",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // 版本信息卡片
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                shape = MaterialTheme.shapes.large,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "AIOT Version $currentVersionName",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "更新日期: $versionCode · © ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} ASLant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = {
                            if (!isCheckingUpdate) {
                                isCheckingUpdate = true
                                if (!isNetworkAvailable(context)) {
                                    updateMessage = "网络连接不可用，请检查网络设置"
                                    isCheckingUpdate = false
                                    return@FilledTonalButton
                                }

                                coroutineScope.launch {
                                    try {
                                        Timber.d("开始检查更新，当前版本: $currentVersionName")
                                        val result = checkForUpdates(currentVersionName, currentVersionCode)
                                        if (result != null) {
                                            Timber.d("发现新版本: ${result.versionName}")
                                            updateInfo = result
                                            showUpdateDialog = true
                                        } else {
                                            Timber.d("没有发现新版本")
                                            updateMessage = "已是最新版本"
                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e, "检查更新失败")
                                        updateMessage = "检查更新失败: ${e.message ?: "未知错误"}"
                                    } finally {
                                        isCheckingUpdate = false
                                    }
                                }
                            }
                        },
                        enabled = !isCheckingUpdate,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.download),
                                contentDescription = "检查更新",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isCheckingUpdate) "正在检查…" else "检查更新")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 更新提示对话框
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.download),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = {
                Text(
                    text = "发现新版本",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 版本信息行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "当前版本",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "$currentVersionName ($currentVersionCode)",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "最新版本",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${updateInfo?.versionName ?: ""} (${updateInfo?.versionCode ?: ""})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 安装包大小
                    Text(
                        text = "安装包大小: ${updateInfo?.apkSize ?: "未知"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 更新内容标题
                    Text(
                        text = "更新内容",
                        style = MaterialTheme.typography.titleSmall,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 更新内容列表
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                    ) {
                        val lines =
                            (updateInfo?.description ?: "")
                                .split("\n")
                                .filter { it.isNotBlank() }

                        lines.forEachIndexed { index, line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                Text(
                                    text = line.trimStart { it.isDigit() || it == '.' || it == ' ' },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (index < lines.lastIndex) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_VIEW, "https://aslant-api.cn/d/YASLant/apk/release/latest.apk".toUri())
                        context.startActivity(intent)
                        showUpdateDialog = false
                    },
                ) {
                    Text("立即更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("稍后再说")
                }
            },
        )
    }

    // 无更新或出错提示
    if (updateMessage.isNotEmpty()) {
        LaunchedEffect(updateMessage) {
            snackbarHostState.showSnackbar(updateMessage, duration = SnackbarDuration.Short)
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
    // APK大小，默认为"未知"
    val apkSize: String = "未知",
)

// 检查更新函数
suspend fun checkForUpdates(
    currentVersion: String,
    currentVersionCode: Int,
): UpdateInfo? =
    withContext(Dispatchers.IO) {
        Timber.d("进入checkForUpdates函数，当前版本: $currentVersion, 版本号: $currentVersionCode")
        return@withContext try {
            // 实际API调用
            val url = URL("https://aslant.top/version.json")
            Timber.d("请求URL: $url")

            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val inputStream = connection.getInputStream()
            val response = inputStream.bufferedReader().use { it.readText() }
            Timber.d("API响应: $response")

            val jsonObject = JSONObject(response)
            val latestVersion = jsonObject.getString("versionName")
            val versionCode = jsonObject.getInt("versionCode")
            val description = jsonObject.getString("description")
            val downloadUrl = jsonObject.getString("downloadUrl")

            // 获取APK文件大小
            val apkSize =
                try {
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
                    Timber.e(e, "获取APK大小失败")
                    "未知"
                }

            Timber.d(
                "解析结果 - 最新版本: $latestVersion, 当前版本: $currentVersion, 最新版本号: $versionCode, 当前版本号: $currentVersionCode, APK大小: $apkSize",
            )

            // 比较版本号和版本Code
            val isNewer = isNewerVersion(currentVersion, latestVersion) || versionCode > currentVersionCode
            Timber.d("版本比较结果: $isNewer")

            if (isNewer) {
                UpdateInfo(latestVersion, versionCode, description, downloadUrl, apkSize)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "检查更新出错")
            throw e
        }
    }

// 版本比较函数
fun isNewerVersion(
    currentVersion: String,
    latestVersion: String,
): Boolean {
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
    val connectivityManager =
        context.getSystemService(
            android.content.Context.CONNECTIVITY_SERVICE,
        ) as ConnectivityManager

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
