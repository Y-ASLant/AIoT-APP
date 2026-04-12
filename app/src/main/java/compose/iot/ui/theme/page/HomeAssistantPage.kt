package compose.iot.ui.theme.page

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import compose.iot.mqtt.CardStyle
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.ServerType
import compose.iot.mqtt.SubscriptionCard
import compose.iot.ui.theme.function.standardEnterTransition
import compose.iot.ui.theme.function.standardExitTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

data class HADevice(
    val entityId: String,
    val friendlyName: String,
    val state: String,
    val deviceClass: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAssistantPage(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("homeassistant_config", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 添加焦点管理器
    val focusManager = LocalFocusManager.current

    var serverUrl by remember { mutableStateOf(prefs.getString("server_url", "") ?: "") }
    var accessToken by remember { mutableStateOf(prefs.getString("access_token", "") ?: "") }
    var pollingInterval by remember { mutableStateOf(prefs.getInt("polling_interval", 5).toString()) }
    var isLoading by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<HADevice>>(emptyList()) }
    var showDeviceList by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // 添加LaunchedEffect以监视设备列表状态变化，当切换到设备列表时清除焦点
    LaunchedEffect(showDeviceList) {
        // 当切换到设备列表视图时，确保焦点被清除
        if (showDeviceList) {
            focusManager.clearFocus()
        }
    }

    // 根据搜索关键词过滤设备列表
    val filteredDevices =
        remember(devices, searchQuery) {
            if (searchQuery.isBlank()) {
                devices
            } else {
                devices.filter {
                    it.friendlyName.contains(searchQuery, ignoreCase = true) ||
                        it.entityId.contains(searchQuery, ignoreCase = true)
                }
            }
        }

    compose.iot.ui.components.AppScaffold(
        title = "Home Assistant 配置",
        navController = navController,
        snackbarHostState = snackbarHostState,
        actions = {
            if (showDeviceList) {
                IconButton(
                    onClick = {
                        focusManager.clearFocus() // 刷新时清除焦点
                        scope.launch {
                            fetchHADevices(serverUrl, accessToken) { newDevices ->
                                devices = newDevices
                            }
                        }
                    },
                ) {
                    Icon(TablerIcons.Refresh, contentDescription = "刷新设备列表")
                }
            }
        },
    ) { _ ->
        AnimatedVisibility(
            visible = isVisible,
            enter = standardEnterTransition(initialOffsetY = -50),
            exit = standardExitTransition(targetOffsetY = -50),
        ) {
            if (!showDeviceList) {
                // 配置页面
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "网关详情",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("服务器地址") },
                        placeholder = { Text("例如: http://homeassistant.local:8123") },
                        leadingIcon = { Icon(TablerIcons.Home, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )

                    OutlinedTextField(
                        value = accessToken,
                        onValueChange = { accessToken = it },
                        label = { Text("长期访问令牌 (Token)") },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(TablerIcons.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )

                    OutlinedTextField(
                        value = pollingInterval,
                        onValueChange = {
                            if (it.isEmpty() || it.toIntOrNull() != null) {
                                pollingInterval = it
                            }
                        },
                        label = { Text("轮询间隔（秒）") },
                        placeholder = { Text("默认为5秒") },
                        leadingIcon = { Icon(TablerIcons.InfoCircle, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = MaterialTheme.shapes.small,
                    )

                    Text(
                        text = "轮询间隔决定了设备状态更新的频率，建议设置在3-30秒之间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (serverUrl.isBlank() || accessToken.isBlank() || pollingInterval.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("请填写所有必填项", duration = SnackbarDuration.Short) }
                                return@Button
                            }

                            val intervalValue = pollingInterval.toIntOrNull()
                            if (intervalValue == null || intervalValue < 1) {
                                scope.launch { snackbarHostState.showSnackbar("轮询间隔必须是大于0的整数", duration = SnackbarDuration.Short) }
                                return@Button
                            }

                            isLoading = true
                            scope.launch {
                                // 测试连接
                                val testResult = testHAConnection(serverUrl, accessToken)
                                if (testResult) {
                                    // 保存配置
                                    context.getSharedPreferences("homeassistant_config", Context.MODE_PRIVATE)
                                        .edit {
                                            putString("server_url", serverUrl)
                                                .putString("access_token", accessToken)
                                                .putInt("polling_interval", pollingInterval.toInt())
                                        }

                                    // 获取设备列表
                                    fetchHADevices(serverUrl, accessToken) { newDevices ->
                                        devices = newDevices
                                        showDeviceList = true
                                    }

                                    scope.launch { snackbarHostState.showSnackbar("连接成功", duration = SnackbarDuration.Short) }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("连接失败，请检查配置", duration = SnackbarDuration.Short) }
                                }
                                isLoading = false
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = TablerIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "连接并获取设备",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else {
                // 设备列表页面
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                ) {
                    // 搜索框 - 减小高度
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp) // 减小外边距
                                .height(48.dp),
                        // 减小输入框高度
                        placeholder = {
                            Text(
                                "搜索设备...",
                                // 使用较小的字体
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        // 使用较小的字体
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                TablerIcons.Search,
                                contentDescription = "搜索",
                                // 减小图标尺寸
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        focusManager.clearFocus() // 清除文本时也清除焦点
                                    },
                                    // 减小按钮尺寸
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        TablerIcons.X,
                                        contentDescription = "清除",
                                        // 减小图标尺寸
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                    )

                    // 显示设备总数和过滤后的数量
                    if (devices.isNotEmpty()) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                            // 减小外边距
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text =
                                    "共 ${devices.size} 个设备" +
                                        if (searchQuery.isNotEmpty()) ", 已过滤 ${filteredDevices.size} 个" else "",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    // 设备列表
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp) // 减小外边距
                                .padding(bottom = 12.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        focusManager.clearFocus()
                                    }
                                },
                        // 减小项目间距
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(
                            items = filteredDevices,
                            key = { device -> device.entityId },
                        ) { device ->
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                onClick = {
                                    focusManager.clearFocus() // 点击卡片时清除焦点

                                    // 添加设备到监控列表
                                    val card =
                                        SubscriptionCard(
                                            // 添加正确的topic前缀和后缀
                                            topic = "homeassistant/${device.entityId}/state",
                                            displayName = device.friendlyName,
                                            jsonParam = "state",
                                            cardStyle = CardStyle.MINIMAL,
                                            serverType = ServerType.HomeAssistant,
                                            deviceType =
                                                when {
                                                    device.entityId.startsWith("number.") -> DeviceType.ACTUATOR
                                                    device.entityId.startsWith("switch.") -> DeviceType.ACTUATOR
                                                    device.entityId.startsWith("button.") -> DeviceType.ACTUATOR
                                                    device.entityId.startsWith("input_button.") -> DeviceType.ACTUATOR
                                                    device.entityId.startsWith(
                                                        "light.",
                                                    ) -> DeviceType.ACTUATOR // 添加light类型设备为执行器
                                                    // 保留基于 deviceClass 的传感器判断逻辑
                                                    device.deviceClass in listOf("temperature", "humidity", "pressure", "illuminance") -> DeviceType.SENSOR
                                                    // 对于其他执行器类型，如果需要，也可以添加判断
                                                    device.deviceClass in listOf("light", "fan", "cover", "button") -> DeviceType.ACTUATOR // 示例：保留其他执行器判断
                                                    else -> DeviceType.SENSOR // 默认类型
                                                },
                                            unitSuffix =
                                                when (device.deviceClass) { // 单位后缀通常与设备类别相关，暂时保留
                                                    "temperature" -> "°C"
                                                    "humidity" -> "%"
                                                    else -> ""
                                                },
                                            // 将light也设为按钮样式
                                            isButtonStyle = device.entityId.startsWith("switch.") || device.entityId.startsWith("light."),
                                            isSliderStyle =
                                                device.entityId.startsWith(
                                                    "number.",
                                                ),
                                            // 如果 ID 以 "number." 开头，则为滑块样式
                                            isPushButtonStyle =
                                                device.entityId.startsWith(
                                                    "button.",
                                                ),
                                            // 如果 ID 以 "button." 开头，则为按钮样式
                                            // 默认按钮值
                                            buttonValue = "1",
                                        )

                                    // 使用 Room Database 保存卡片（需要协程上下文）
                                    scope.launch {
                                        val dao = compose.iot.data.room.AppDatabase.getDatabase(context).subscriptionCardDao()
                                        val existingCards = dao.getAllCards()

                                        // 检查是否已经添加过
                                        if (existingCards.any { it.topic == card.topic }) {
                                            snackbarHostState.showSnackbar("该设备已添加", duration = SnackbarDuration.Short)
                                            return@launch
                                        }

                                        // 添加新卡片到数据库
                                        dao.insertCards(listOf(card))

                                        // 如果是执行器，保存当前状态
                                        if (card.deviceType == DeviceType.ACTUATOR) {
                                            val cardId = "${card.topic}:${card.jsonParam}"

                                            when (card.serverType) {
                                                ServerType.HomeAssistant -> {
                                                    // 对于Home Assistant设备，使用当前状态
                                                    val isOn = device.state == "on"

                                                    if (card.isSliderStyle) {
                                                        // 如果是滑块类型，保存到slider_states
                                                        context.getSharedPreferences("slider_states", Context.MODE_PRIVATE)
                                                            .edit {
                                                                putFloat(cardId, if (isOn) 1.0f else 0.0f)
                                                            }
                                                    } else {
                                                        // 如果是开关类型，保存到switch_states
                                                        context.getSharedPreferences("switch_states", Context.MODE_PRIVATE)
                                                            .edit {
                                                                putBoolean(cardId, isOn)
                                                            }
                                                    }
                                                }
                                                else -> {
                                                    // 对于其他设备，使用默认值
                                                    if (card.isSliderStyle) {
                                                        context.getSharedPreferences("slider_states", Context.MODE_PRIVATE)
                                                            .edit {
                                                                putFloat(cardId, 0.0f)
                                                            }
                                                    } else {
                                                        context.getSharedPreferences("switch_states", Context.MODE_PRIVATE)
                                                            .edit {
                                                                putBoolean(cardId, false)
                                                            }
                                                    }
                                                }
                                            }
                                        }

                                        snackbarHostState.showSnackbar("已添加设备：${device.friendlyName}", duration = SnackbarDuration.Short)
                                    }
                                    // 不再自动返回首页
                                    // navController.navigateUp()
                                },
                            ) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(6.dp),
                                    // 减小内边距
                                    // 减小项目间距
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    Text(
                                        text = device.friendlyName,
                                        // 使用更小的标题字体
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "ID: ${device.entityId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "状态: ${device.state}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (device.deviceClass != null) {
                                        Text(
                                            text = "类型: ${device.deviceClass}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun testHAConnection(
    serverUrl: String,
    accessToken: String,
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // 确保URL格式正确
            var finalUrl = serverUrl.trim()
            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                finalUrl = "http://$finalUrl"
            }
            if (finalUrl.endsWith("/")) {
                finalUrl = finalUrl.dropLast(1)
            }

            val url = URL("$finalUrl/api/")
            val connection =
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 5000
                    readTimeout = 5000
                    // 允许重定向
                    instanceFollowRedirects = true
                }

            try {
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    return@withContext true
                } else {
                    val errorStream = connection.errorStream
                    val errorMessage = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Timber.w("HA连接失败: HTTP $responseCode - $errorMessage")
                    return@withContext false
                }
            } catch (e: Exception) {
                Timber.e(e, "HA连接异常")
                return@withContext false
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Timber.e(e, "HA连接异常")
            return@withContext false
        }
    }
}

private suspend fun fetchHADevices(
    serverUrl: String,
    accessToken: String,
    onDevicesFetched: (List<HADevice>) -> Unit,
) {
    withContext(Dispatchers.IO) {
        try {
            // 确保URL格式正确
            var finalUrl = serverUrl.trim()
            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                finalUrl = "http://$finalUrl"
            }
            if (finalUrl.endsWith("/")) {
                finalUrl = finalUrl.dropLast(1)
            }

            val url = URL("$finalUrl/api/states")
            val connection =
                (url.openConnection() as HttpURLConnection).apply {
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
                    val devices = mutableListOf<HADevice>()

                    JSONArray(response).let { array ->
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            val entityId = item.getString("entity_id")
                            val attributes = item.getJSONObject("attributes")
                            val friendlyName = attributes.optString("friendly_name", entityId)
                            val deviceClass = attributes.optString("device_class").takeIf { it.isNotEmpty() }
                            val state = item.getString("state")

                            devices.add(HADevice(entityId, friendlyName, state, deviceClass))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        onDevicesFetched(devices)
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorMessage = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Timber.w("HA获取设备失败: HTTP $responseCode - $errorMessage")
                    withContext(Dispatchers.Main) {
                        onDevicesFetched(emptyList())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "HA获取设备异常")
                withContext(Dispatchers.Main) {
                    onDevicesFetched(emptyList())
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Timber.e(e, "HA获取设备异常")
            withContext(Dispatchers.Main) {
                onDevicesFetched(emptyList())
            }
        }
    }
}
