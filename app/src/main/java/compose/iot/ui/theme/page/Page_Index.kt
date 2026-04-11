package compose.iot.ui.theme.page

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import compose.iot.R
import compose.iot.mqtt.CardStyle
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.HomeAssistantManager
import compose.iot.mqtt.MqttManager
import compose.iot.mqtt.SensorHistoryData
import compose.iot.mqtt.SensorHistoryManager
import compose.iot.mqtt.ServerType
import compose.iot.mqtt.SubscriptionCard
import compose.iot.ui.theme.function.MqttSubscribeDialog
import compose.iot.ui.theme.function.SensorHistoryBottomSheet
import compose.iot.ui.theme.function.standardEnterTransition
import compose.iot.ui.theme.function.standardExitTransition
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

@SuppressLint("DefaultLocale", "CommitPrefEdits", "AutoboxingStateCreation")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Page_Index(mqttManager: MqttManager) {
    val context = LocalContext.current
    var showSubscribeDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<SubscriptionCard?>(null) }
    var selectedDeviceType by remember {
        mutableStateOf(
            DeviceType.valueOf(
                context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    .getString("selected_device_type", DeviceType.SENSOR.name) ?: DeviceType.SENSOR.name,
            ),
        )
    }

    // 添加历史数据相关的状态
    var showHistoryBottomSheet by remember { mutableStateOf(false) }
    var selectedSensorCard by remember { mutableStateOf<SubscriptionCard?>(null) }
    var sensorHistoryData by remember { mutableStateOf<List<SensorHistoryData>>(emptyList()) }
    val historyManager = remember { SensorHistoryManager(context) }

    var subscriptionCards by remember {
        mutableStateOf(SubscriptionCardStorage.loadCards(context))
    }
    var cardValues by remember { mutableStateOf(mapOf<String, String>()) }
    var topicSubscriptionCount by remember {
        mutableStateOf(subscriptionCards.groupingBy { it.topic }.eachCount())
    }

    // 添加Snackbar状态
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 创建 Home Assistant 管理器
    val haManager = remember { HomeAssistantManager(context) }
    val subscriptionController =
        remember(context, mqttManager, haManager, historyManager, scope) {
            DeviceSubscriptionController(
                context = context,
                mqttManager = mqttManager,
                haManager = haManager,
                historyManager = historyManager,
                scope = scope,
                getCards = { subscriptionCards },
                updateCardValue = { cardId, value ->
                    cardValues = cardValues + (cardId to value)
                },
            )
        }

    // 添加网格滚动状态
    val gridState = rememberLazyGridState()
    var previousFirstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var isTitleVisible by remember { mutableStateOf(true) }

    // 监听滚动状态
    LaunchedEffect(remember { derivedStateOf { gridState.firstVisibleItemIndex } }) {
        if (gridState.firstVisibleItemIndex > previousFirstVisibleItemIndex) {
            // 向下滚动，隐藏标题
            isTitleVisible = false
        } else if (gridState.firstVisibleItemIndex < previousFirstVisibleItemIndex) {
            // 向上滚动，显示标题
            isTitleVisible = true
        }
        previousFirstVisibleItemIndex = gridState.firstVisibleItemIndex
    }

    // 在组件首次加载时初始化 Home Assistant 连接
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("homeassistant_config", Context.MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", "") ?: ""
        val accessToken = prefs.getString("access_token", "") ?: ""
        val pollingInterval = prefs.getInt("polling_interval", 3)

        if (serverUrl.isNotBlank() && accessToken.isNotBlank()) {
            Timber.d("从配置中读取到 HA 配置，初始化连接")
            haManager.setServerConfig(serverUrl, accessToken)
            haManager.setPollingInterval(pollingInterval)
        }

        Timber.d("开始初始化执行器状态")
        cardValues = cardValues + SubscriptionCardStorage.loadActuatorCardValues(context, subscriptionCards)
    }

    // 在组件首次加载时重新订阅所有主题
    LaunchedEffect(Unit) {
        subscriptionController.subscribeAll()
    }

    // 在组件销毁时断开连接
    DisposableEffect(Unit) {
        onDispose {
            Timber.d("组件销毁，断开 HA 连接")
            subscriptionController.dispose()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 36.dp),
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                ) {
                    AnimatedVisibility(
                        visible = isTitleVisible,
                        enter = standardEnterTransition(initialOffsetY = -50),
                        exit = standardExitTransition(targetOffsetY = -50),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "设备中心",
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth(0.98f),
                        ) {
                            SegmentedButton(
                                selected = selectedDeviceType == DeviceType.SENSOR,
                                onClick = {
                                    selectedDeviceType = DeviceType.SENSOR
                                    // 保存选择到 SharedPreferences
                                    context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                                        .edit {
                                            putString(
                                                "selected_device_type",
                                                DeviceType.SENSOR.name,
                                            )
                                        }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            ) {
                                Text(
                                    text = "传感器",
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }
                            SegmentedButton(
                                selected = selectedDeviceType == DeviceType.ACTUATOR,
                                onClick = {
                                    selectedDeviceType = DeviceType.ACTUATOR
                                    // 保存选择到 SharedPreferences
                                    context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                                        .edit {
                                            putString(
                                                "selected_device_type",
                                                DeviceType.ACTUATOR.name,
                                            )
                                        }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            ) {
                                Text(
                                    text = "执行器",
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }
                        }
                    }
                }
            }

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = rememberLazyStaggeredGridState(),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
            ) {
                if (subscriptionCards.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.data),
                                    contentDescription = "暂无设备",
                                    modifier = Modifier.size(96.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "暂无设备\n点击右下角按钮添加设备",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = subscriptionCards.filter { it.deviceType == selectedDeviceType },
                        span = { card ->
                            if (card.deviceType == DeviceType.ACTUATOR) {
                                StaggeredGridItemSpan.FullLine
                            } else {
                                StaggeredGridItemSpan.SingleLane
                            }
                        },
                    ) { card ->
                        val cardId = "${card.topic}:${card.jsonParam}"
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 120.dp) // 设置最小高度
                                    .clip(MaterialTheme.shapes.medium)
                                    .combinedClickable(
                                        onClick = {
                                            if (card.deviceType == DeviceType.SENSOR) {
                                                selectedSensorCard = card
                                                val cardId = "${card.topic}:${card.jsonParam}"
                                                sensorHistoryData = historyManager.getHistoryData(cardId)
                                                showHistoryBottomSheet = true
                                            }
                                        },
                                        onLongClick = {
                                            editingCard = card
                                            showSubscribeDialog = true
                                        },
                                    ),
                            elevation =
                                when (card.cardStyle) {
                                    CardStyle.FILLED -> CardDefaults.cardElevation(0.dp)
                                    else -> CardDefaults.cardElevation(4.dp)
                                },
                            colors =
                                when (card.cardStyle) {
                                    CardStyle.HIGHLIGHT ->
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        )
                                    CardStyle.MINIMAL ->
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                        )
                                    CardStyle.FILLED ->
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                },
                            border =
                                if (card.cardStyle == CardStyle.MINIMAL) {
                                    BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                    )
                                } else {
                                    null
                                },
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = card.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            AssistChip(
                                                onClick = { },
                                                modifier = Modifier.weight(1f),
                                                label = {
                                                    Text(
                                                        when (card.deviceType) {
                                                            DeviceType.SENSOR -> "传感器"
                                                            DeviceType.ACTUATOR -> "执行器"
                                                        },
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth(),
                                                    )
                                                },
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            AssistChip(
                                                onClick = { },
                                                modifier = Modifier.weight(1f),
                                                label = {
                                                    Text(
                                                        when (card.serverType) {
                                                            ServerType.EMQX -> "EMQX"
                                                            ServerType.HomeAssistant -> "HA"
                                                        },
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth(),
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (card.deviceType == DeviceType.ACTUATOR) {
                                        if (card.serverType == ServerType.EMQX) {
                                            if (card.isButtonStyle) {
                                                val cardId = "${card.topic}:${card.jsonParam}"
                                                var isOn by remember {
                                                    mutableStateOf(
                                                        context.getSharedPreferences(
                                                            "switch_states",
                                                            Context.MODE_PRIVATE,
                                                        )
                                                            .getBoolean(cardId, false),
                                                    )
                                                }
                                                var isLoading by remember { mutableStateOf(false) }

                                                // 同时监听卡片ID和状态值变化
                                                LaunchedEffect(card.topic, cardValues[cardId]) {
                                                    // 先从cardValues获取最新值，如果没有则使用SharedPreferences中存储的值
                                                    val value = cardValues[cardId]
                                                    if (value != null) {
                                                        val newState = value == "on" || value == card.switchOnValue
                                                        if (newState != isOn && !isLoading) {
                                                            isOn = newState
                                                            // 保存新状态
                                                            context.getSharedPreferences(
                                                                "switch_states",
                                                                Context.MODE_PRIVATE,
                                                            )
                                                                .edit()
                                                                .apply {
                                                                    putBoolean(cardId, newState)
                                                                    apply()
                                                                }
                                                        }
                                                    }
                                                    // 如果没有cardValues，则不更改当前状态（保持从SharedPreferences加载的状态）
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text(
                                                        text = "关闭",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color =
                                                            if (!isOn) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                            },
                                                    )
                                                    Switch(
                                                        checked = isOn,
                                                        enabled = !isLoading,
                                                        onCheckedChange = { newState ->
                                                            isLoading = true
                                                            val value = if (newState) card.switchOnValue else card.switchOffValue
                                                            val jsonData =
                                                                JSONObject().apply {
                                                                    put(card.jsonParam, value)
                                                                }
                                                            mqttManager.publish(
                                                                topic = card.topic,
                                                                message = jsonData.toString(),
                                                                onComplete = {
                                                                    isOn = newState
                                                                    isLoading = false
                                                                    // 保存状态到SharedPreferences
                                                                    context.getSharedPreferences(
                                                                        "switch_states",
                                                                        Context.MODE_PRIVATE,
                                                                    )
                                                                        .edit()
                                                                        .apply {
                                                                            putBoolean(cardId, newState)
                                                                        }
                                                                    Toast.makeText(
                                                                        context,
                                                                        "发送成功",
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                },
                                                                onError = { error ->
                                                                    isLoading = false
                                                                    Toast.makeText(
                                                                        context,
                                                                        "发送失败: $error",
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                },
                                                            )
                                                        },
                                                    )
                                                    Text(
                                                        text = "开启",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color =
                                                            if (isOn) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                            },
                                                    )
                                                }
                                            } else if (card.isSliderStyle) {
                                                val cardId = "${card.topic}:${card.jsonParam}"
                                                var sliderValue by remember {
                                                    mutableFloatStateOf(
                                                        context.getSharedPreferences(
                                                            "slider_states",
                                                            Context.MODE_PRIVATE,
                                                        )
                                                            .getFloat(cardId, card.sliderMin),
                                                    )
                                                }
                                                var lastToastTime by remember { mutableLongStateOf(0L) }
                                                // 添加变量用于防止循环发送
                                                var isChangingFromMQTT by remember { mutableStateOf(false) }

                                                // 获取滑动控制模式设置
                                                val continuousUpdateMode =
                                                    remember {
                                                        context.getSharedPreferences(
                                                            "app_settings",
                                                            Context.MODE_PRIVATE,
                                                        )
                                                            .getBoolean("slider_continuous_update", false)
                                                    }
                                                var isContinuousUpdateMode by remember {
                                                    mutableStateOf(
                                                        continuousUpdateMode,
                                                    )
                                                }

                                                // 添加状态变化监听
                                                LaunchedEffect(card.topic, cardValues[cardId]) {
                                                    val value = cardValues[cardId]
                                                    if (value != null && value != "unknown") {
                                                        try {
                                                            val floatValue = value.toFloatOrNull()
                                                            if (floatValue != null && floatValue != sliderValue) {
                                                                Timber.d(
                                                                    "滑块值从cardValues更新: $cardId = $floatValue (原值: $sliderValue)",
                                                                )
                                                                isChangingFromMQTT = true // 标记这是从MQTT消息导致的改变
                                                                sliderValue = floatValue

                                                                // 保存到SharedPreferences
                                                                context.getSharedPreferences(
                                                                    "slider_states",
                                                                    Context.MODE_PRIVATE,
                                                                )
                                                                    .edit()
                                                                    .apply {
                                                                        putFloat(cardId, floatValue)
                                                                        apply()
                                                                    }
                                                                // 延迟重置标记，允许足够时间让UI更新
                                                                kotlinx.coroutines.delay(100)
                                                                isChangingFromMQTT = false
                                                            }
                                                        } catch (e: Exception) {
                                                            Timber.e(e, "无法解析滑块值: $value")
                                                        }
                                                    }
                                                }

                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    // 将滑块值和控制开关放在同一行
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text(
                                                            text = String.format("%.1f", sliderValue) + card.unitSuffix,
                                                            style = MaterialTheme.typography.titleLarge,
                                                        )

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Text(
                                                                text = "实时控制",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color =
                                                                    MaterialTheme.colorScheme.onSurface.copy(
                                                                        alpha = 0.7f,
                                                                    ),
                                                            )
                                                            Switch(
                                                                checked = isContinuousUpdateMode,
                                                                onCheckedChange = { isChecked ->
                                                                    isContinuousUpdateMode = isChecked
                                                                    // 保存到 SharedPreferences
                                                                    context.getSharedPreferences(
                                                                        "app_settings",
                                                                        Context.MODE_PRIVATE,
                                                                    )
                                                                        .edit {
                                                                            putBoolean(
                                                                                "slider_continuous_update",
                                                                                isChecked,
                                                                            )
                                                                        }
                                                                },
                                                                modifier = Modifier.scale(0.7f),
                                                            )
                                                        }
                                                    }

                                                    Slider(
                                                        value = sliderValue,
                                                        onValueChange = { newValue ->
                                                            sliderValue = newValue

                                                            // 只有非MQTT触发且实时控制模式时才发送
                                                            if (isContinuousUpdateMode && !isChangingFromMQTT) {
                                                                val formattedValue =
                                                                    String.format(
                                                                        "%.1f",
                                                                        newValue,
                                                                    ).toFloat()
                                                                val jsonData =
                                                                    JSONObject().apply {
                                                                        put(card.jsonParam, formattedValue)
                                                                    }
                                                                mqttManager.publish(
                                                                    topic = card.topic,
                                                                    message = jsonData.toString(),
                                                                    onComplete = {
                                                                        // 保存状态到SharedPreferences
                                                                        context.getSharedPreferences(
                                                                            "slider_states",
                                                                            Context.MODE_PRIVATE,
                                                                        )
                                                                            .edit()
                                                                            .apply {
                                                                                putFloat(cardId, formattedValue)
                                                                            }
                                                                        val currentTime = System.currentTimeMillis()
                                                                        if (currentTime - lastToastTime > 1000) {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "发送成功",
                                                                                Toast.LENGTH_SHORT,
                                                                            ).show()
                                                                            lastToastTime = currentTime
                                                                        }
                                                                    },
                                                                    onError = { error ->
                                                                        val currentTime = System.currentTimeMillis()
                                                                        if (currentTime - lastToastTime > 1000) {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "发送失败: $error",
                                                                                Toast.LENGTH_SHORT,
                                                                            ).show()
                                                                            lastToastTime = currentTime
                                                                        }
                                                                    },
                                                                )
                                                            }
                                                        },
                                                        onValueChangeFinished = {
                                                            // 只有在非实时控制模式下，且不是MQTT触发的变化时才发送
                                                            if (!isContinuousUpdateMode && !isChangingFromMQTT) {
                                                                val formattedValue =
                                                                    String.format(
                                                                        "%.1f",
                                                                        sliderValue,
                                                                    ).toFloat()
                                                                val jsonData =
                                                                    JSONObject().apply {
                                                                        put(card.jsonParam, formattedValue)
                                                                    }
                                                                mqttManager.publish(
                                                                    topic = card.topic,
                                                                    message = jsonData.toString(),
                                                                    onComplete = {
                                                                        // 保存状态到SharedPreferences
                                                                        context.getSharedPreferences(
                                                                            "slider_states",
                                                                            Context.MODE_PRIVATE,
                                                                        )
                                                                            .edit()
                                                                            .apply {
                                                                                putFloat(cardId, formattedValue)
                                                                            }
                                                                        val currentTime = System.currentTimeMillis()
                                                                        if (currentTime - lastToastTime > 1000) {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "发送成功",
                                                                                Toast.LENGTH_SHORT,
                                                                            ).show()
                                                                            lastToastTime = currentTime
                                                                        }
                                                                    },
                                                                    onError = { error ->
                                                                        val currentTime = System.currentTimeMillis()
                                                                        if (currentTime - lastToastTime > 1000) {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "发送失败: $error",
                                                                                Toast.LENGTH_SHORT,
                                                                            ).show()
                                                                            lastToastTime = currentTime
                                                                        }
                                                                    },
                                                                )
                                                            }
                                                        },
                                                        valueRange = card.sliderMin..card.sliderMax,
                                                        steps = ((card.sliderMax - card.sliderMin) / card.sliderStep).toInt() - 1,
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                    ) {
                                                        Text(
                                                            text = String.format("%.1f", card.sliderMin),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color =
                                                                MaterialTheme.colorScheme.onSurface.copy(
                                                                    alpha = 0.6f,
                                                                ),
                                                        )
                                                        Text(
                                                            text = String.format("%.1f", card.sliderMax),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color =
                                                                MaterialTheme.colorScheme.onSurface.copy(
                                                                    alpha = 0.6f,
                                                                ),
                                                        )
                                                    }
                                                }
                                            } else if (card.isPushButtonStyle) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    var isLoading by remember { mutableStateOf(false) }

                                                    Button(
                                                        onClick = {
                                                            isLoading = true
                                                            val jsonData =
                                                                JSONObject().apply {
                                                                    put(card.jsonParam, card.buttonValue)
                                                                }

                                                            mqttManager.publish(
                                                                topic = card.topic,
                                                                message = jsonData.toString(),
                                                                onComplete = {
                                                                    // 延迟状态重置，让动画有更好的显示效果
                                                                    scope.launch {
                                                                        kotlinx.coroutines.delay(1000)
                                                                        isLoading = false
                                                                    }
                                                                    Toast.makeText(
                                                                        context,
                                                                        "发送成功",
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                },
                                                                onError = { error ->
                                                                    // 延迟状态重置，让动画有更好的显示效果
                                                                    scope.launch {
                                                                        kotlinx.coroutines.delay(1000)
                                                                        isLoading = false
                                                                    }
                                                                    Toast.makeText(
                                                                        context,
                                                                        "发送失败: $error",
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                },
                                                            )
                                                        },
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .height(48.dp),
                                                        enabled = !isLoading,
                                                    ) {
                                                        if (isLoading) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(24.dp),
                                                                color = MaterialTheme.colorScheme.onPrimary,
                                                                strokeWidth = 2.dp,
                                                            )
                                                        } else {
                                                            Text("执行")
                                                        }
                                                    }
                                                }
                                            } else {
                                                var inputValue by remember { mutableStateOf("") }
                                                var isLoading by remember { mutableStateOf(false) }
                                                OutlinedTextField(
                                                    value = inputValue,
                                                    onValueChange = { inputValue = it },
                                                    modifier =
                                                        Modifier
                                                            .weight(1f)
                                                            .padding(end = 8.dp),
                                                    placeholder = { Text("请输入要发送的内容") },
                                                    singleLine = true,
                                                )
                                                Button(
                                                    onClick = {
                                                        isLoading = true
                                                        val jsonData =
                                                            JSONObject().apply {
                                                                put(card.jsonParam, inputValue)
                                                            }
                                                        mqttManager.publish(
                                                            topic = card.topic,
                                                            message = jsonData.toString(),
                                                            onComplete = {
                                                                // 延迟状态重置，让动画有更好的显示效果
                                                                scope.launch {
                                                                    kotlinx.coroutines.delay(1000)
                                                                    isLoading = false
                                                                    inputValue = ""
                                                                }
                                                                Toast.makeText(
                                                                    context,
                                                                    "发送成功",
                                                                    Toast.LENGTH_SHORT,
                                                                ).show()
                                                            },
                                                            onError = { error ->
                                                                // 延迟状态重置，让动画有更好的显示效果
                                                                scope.launch {
                                                                    kotlinx.coroutines.delay(1000)
                                                                    isLoading = false
                                                                }
                                                                Toast.makeText(
                                                                    context,
                                                                    "发送失败: $error",
                                                                    Toast.LENGTH_SHORT,
                                                                ).show()
                                                            },
                                                        )
                                                    },
                                                    enabled = inputValue.isNotBlank() && !isLoading,
                                                ) {
                                                    if (isLoading) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            strokeWidth = 2.dp,
                                                        )
                                                    } else {
                                                        Text("发送")
                                                    }
                                                }
                                            }
                                        } else if (card.serverType == ServerType.HomeAssistant) {
                                            if (card.isButtonStyle) {
                                                val cardId = "${card.topic}:${card.jsonParam}"
                                                var isOn by remember {
                                                    mutableStateOf(
                                                        context.getSharedPreferences(
                                                            "switch_states",
                                                            Context.MODE_PRIVATE,
                                                        )
                                                            .getBoolean(cardId, false),
                                                    )
                                                }

                                                // 同时监听卡片ID和状态值变化
                                                LaunchedEffect(card.topic, cardValues[cardId]) {
                                                    // 先从cardValues获取最新值，如果没有则使用SharedPreferences中存储的值
                                                    val value = cardValues[cardId]
                                                    if (value != null) {
                                                        val newState = value == "on"
                                                        if (newState != isOn) {
                                                            isOn = newState
                                                            // 保存新状态
                                                            context.getSharedPreferences(
                                                                "switch_states",
                                                                Context.MODE_PRIVATE,
                                                            )
                                                                .edit()
                                                                .apply {
                                                                    putBoolean(cardId, newState)
                                                                    apply()
                                                                }
                                                        }
                                                    }
                                                    // 如果没有cardValues，则不更改当前状态（保持从SharedPreferences加载的状态）
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text(
                                                        text = "关闭",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color =
                                                            if (!isOn) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                            },
                                                    )
                                                    Switch(
                                                        checked = isOn,
                                                        onCheckedChange = { newState ->
                                                            isOn = newState
                                                            val entityId =
                                                                card.topic.removePrefix(
                                                                    "homeassistant/",
                                                                ).removeSuffix("/state")
                                                            haManager.callService(
                                                                domain =
                                                                    when {
                                                                        entityId.startsWith("switch.") -> "switch"
                                                                        entityId.startsWith("light.") -> "light"
                                                                        entityId.startsWith("number.") -> "number"
                                                                        entityId.startsWith("button.") -> "button"
                                                                        else -> "switch" // 默认使用switch
                                                                    },
                                                                service =
                                                                    when {
                                                                        entityId.startsWith("number.") -> "set_value"
                                                                        entityId.startsWith("button.") -> "press"
                                                                        else -> if (newState) "turn_on" else "turn_off"
                                                                    },
                                                                entityId = entityId,
                                                                data =
                                                                    JSONObject().apply {
                                                                        if (entityId.startsWith("number.")) {
                                                                            put("value", if (newState) 1 else 0)
                                                                        }
                                                                    },
                                                                onComplete = {
                                                                    // 保存状态到SharedPreferences
                                                                    context.getSharedPreferences(
                                                                        "switch_states",
                                                                        Context.MODE_PRIVATE,
                                                                    )
                                                                        .edit()
                                                                        .apply {
                                                                            putBoolean(cardId, newState)
                                                                        }
                                                                    Toast.makeText(
                                                                        context,
                                                                        "发送成功",
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                },
                                                                onError = { error ->
                                                                    isOn = !newState // 发送失败时恢复状态
                                                                    Toast.makeText(
                                                                        context,
                                                                        "发送失败: $error",
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                },
                                                            )
                                                        },
                                                    )
                                                    Text(
                                                        text = "开启",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color =
                                                            if (isOn) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                            },
                                                    )
                                                }
                                            } else if (card.isSliderStyle) {
                                                val cardId = "${card.topic}:${card.jsonParam}"
                                                var sliderValue by remember {
                                                    mutableFloatStateOf(
                                                        context.getSharedPreferences(
                                                            "slider_states",
                                                            Context.MODE_PRIVATE,
                                                        )
                                                            .getFloat(cardId, card.sliderMin),
                                                    )
                                                }
                                                var lastToastTime by remember { mutableLongStateOf(0L) }
                                                // 添加变量用于防止循环发送
                                                var isChangingFromHA by remember { mutableStateOf(false) }

                                                // 获取滑动控制模式设置
                                                val continuousUpdateMode =
                                                    remember {
                                                        context.getSharedPreferences(
                                                            "app_settings",
                                                            Context.MODE_PRIVATE,
                                                        )
                                                            .getBoolean("slider_continuous_update", false)
                                                    }
                                                var isContinuousUpdateMode by remember {
                                                    mutableStateOf(
                                                        continuousUpdateMode,
                                                    )
                                                }

                                                // 添加状态变化监听
                                                LaunchedEffect(card.topic, cardValues[cardId]) {
                                                    val value = cardValues[cardId]
                                                    if (value != null && value != "unknown") {
                                                        try {
                                                            val floatValue = value.toFloatOrNull()
                                                            if (floatValue != null && floatValue != sliderValue) {
                                                                Timber.d(
                                                                    "滑块值从cardValues更新: $cardId = $floatValue (原值: $sliderValue)",
                                                                )
                                                                isChangingFromHA = true // 标记这是从HA消息导致的改变
                                                                sliderValue = floatValue

                                                                // 保存到SharedPreferences
                                                                context.getSharedPreferences(
                                                                    "slider_states",
                                                                    Context.MODE_PRIVATE,
                                                                )
                                                                    .edit()
                                                                    .apply {
                                                                        putFloat(cardId, floatValue)
                                                                        apply()
                                                                    }
                                                                // 延迟重置标记
                                                                kotlinx.coroutines.delay(100)
                                                                isChangingFromHA = false
                                                            }
                                                        } catch (e: Exception) {
                                                            Timber.e(e, "无法解析滑块值: $value")
                                                        }
                                                    }
                                                }

                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    // 将滑块值和控制开关放在同一行
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text(
                                                            text = String.format("%.1f", sliderValue) + card.unitSuffix,
                                                            style = MaterialTheme.typography.titleLarge,
                                                        )

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Text(
                                                                text = "实时控制",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color =
                                                                    MaterialTheme.colorScheme.onSurface.copy(
                                                                        alpha = 0.7f,
                                                                    ),
                                                            )
                                                            Switch(
                                                                checked = isContinuousUpdateMode,
                                                                onCheckedChange = { isChecked ->
                                                                    isContinuousUpdateMode = isChecked
                                                                    // 保存到 SharedPreferences
                                                                    context.getSharedPreferences(
                                                                        "app_settings",
                                                                        Context.MODE_PRIVATE,
                                                                    )
                                                                        .edit {
                                                                            putBoolean(
                                                                                "slider_continuous_update",
                                                                                isChecked,
                                                                            )
                                                                        }
                                                                },
                                                                modifier = Modifier.scale(0.7f),
                                                            )
                                                        }
                                                    }

                                                    Slider(
                                                        value = sliderValue,
                                                        onValueChange = { newValue ->
                                                            sliderValue = newValue

                                                            // 只有在非HA触发且实时控制模式下才立即发送
                                                            if (isContinuousUpdateMode && !isChangingFromHA) {
                                                                val formattedValue =
                                                                    String.format(
                                                                        "%.1f",
                                                                        newValue,
                                                                    ).toFloat()
                                                                val entityId =
                                                                    card.topic.removePrefix(
                                                                        "homeassistant/",
                                                                    ).removeSuffix("/state")
                                                                haManager.callService(
                                                                    domain =
                                                                        when {
                                                                            entityId.startsWith("number.") -> "number"
                                                                            entityId.startsWith("light.") -> "light"
                                                                            entityId.startsWith(
                                                                                "input_number.",
                                                                            ) -> "input_number"
                                                                            else -> "number" // 默认使用number，因为这里是滑块控件
                                                                        },
                                                                    service =
                                                                        when {
                                                                            entityId.startsWith("light.") -> "turn_on"
                                                                            else -> "set_value"
                                                                        },
                                                                    entityId = entityId,
                                                                    data =
                                                                        JSONObject().apply {
                                                                            if (entityId.startsWith("light.")) {
                                                                                put(
                                                                                    "brightness",
                                                                                    (formattedValue * 255).toInt(),
                                                                                )
                                                                            } else {
                                                                                put("value", formattedValue)
                                                                            }
                                                                        },
                                                                    onComplete = {
                                                                        // 保存状态到SharedPreferences
                                                                        context.getSharedPreferences(
                                                                            "slider_states",
                                                                            Context.MODE_PRIVATE,
                                                                        )
                                                                            .edit()
                                                                            .apply {
                                                                                putFloat(cardId, formattedValue)
                                                                            }
                                                                        val currentTime = System.currentTimeMillis()
                                                                        if (currentTime - lastToastTime > 1000) {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "发送成功",
                                                                                Toast.LENGTH_SHORT,
                                                                            ).show()
                                                                            lastToastTime = currentTime
                                                                        }
                                                                    },
                                                                    onError = { error ->
                                                                        val currentTime = System.currentTimeMillis()
                                                                        if (currentTime - lastToastTime > 1000) {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "发送失败: $error",
                                                                                Toast.LENGTH_SHORT,
                                                                            ).show()
                                                                            lastToastTime = currentTime
                                                                        }
                                                                    },
                                                                )
                                                            }
                                                        },
                                                        onValueChangeFinished = {
                                                            // 只有在非实时控制模式下，且不是HA触发的变化时才发送
                                                            if (!isContinuousUpdateMode && !isChangingFromHA) {
                                                                val formattedValue =
                                                                    String.format(
                                                                        "%.1f",
                                                                        sliderValue,
                                                                    ).toFloat()
                                                                val entityId =
                                                                    card.topic.removePrefix(
                                                                        "homeassistant/",
                                                                    ).removeSuffix("/state")
                                                                haManager.callService(
                                                                    domain =
                                                                        when {
                                                                            entityId.startsWith("number.") -> "number"
                                                                            entityId.startsWith("light.") -> "light"
                                                                            entityId.startsWith(
                                                                                "input_number.",
                                                                            ) -> "input_number"
                                                                            else -> "number" // 默认使用number，因为这里是滑块控件
                                                                        },
                                                                    service =
                                                                        when {
                                                                            entityId.startsWith("light.") -> "turn_on"
                                                                            else -> "set_value"
                                                                        },
                                                                    entityId = entityId,
                                                                    data =
                                                                        JSONObject().apply {
                                                                            if (entityId.startsWith("light.")) {
                                                                                put(
                                                                                    "brightness",
                                                                                    (formattedValue * 255).toInt(),
                                                                                )
                                                                            } else {
                                                                                put("value", formattedValue)
                                                                            }
                                                                        },
                                                                    onComplete = {
                                                                        // 保存状态到SharedPreferences
                                                                        context.getSharedPreferences(
                                                                            "slider_states",
                                                                            Context.MODE_PRIVATE,
                                                                        )
                                                                            .edit()
                                                                            .apply {
                                                                                putFloat(cardId, formattedValue)
                                                                            }
                                                                        val currentTime = System.currentTimeMillis()
                                                                        if (currentTime - lastToastTime > 1000) {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "发送成功",
                                                                                Toast.LENGTH_SHORT,
                                                                            ).show()
                                                                            lastToastTime = currentTime
                                                                        }
                                                                    },
                                                                    onError = { error ->
                                                                        val currentTime = System.currentTimeMillis()
                                                                        if (currentTime - lastToastTime > 1000) {
                                                                            Toast.makeText(
                                                                                context,
                                                                                "发送失败: $error",
                                                                                Toast.LENGTH_SHORT,
                                                                            ).show()
                                                                            lastToastTime = currentTime
                                                                        }
                                                                    },
                                                                )
                                                            }
                                                        },
                                                        valueRange = card.sliderMin..card.sliderMax,
                                                        steps = ((card.sliderMax - card.sliderMin) / card.sliderStep).toInt() - 1,
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                    ) {
                                                        Text(
                                                            text = String.format("%.1f", card.sliderMin),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color =
                                                                MaterialTheme.colorScheme.onSurface.copy(
                                                                    alpha = 0.6f,
                                                                ),
                                                        )
                                                        Text(
                                                            text = String.format("%.1f", card.sliderMax),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color =
                                                                MaterialTheme.colorScheme.onSurface.copy(
                                                                    alpha = 0.6f,
                                                                ),
                                                        )
                                                    }
                                                }
                                            } else if (card.isPushButtonStyle) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    var isLoading by remember { mutableStateOf(false) }

                                                    Button(
                                                        onClick = {
                                                            isLoading = true
                                                            val entityId =
                                                                card.topic.removePrefix(
                                                                    "homeassistant/",
                                                                ).removeSuffix("/state")

                                                            haManager.callService(
                                                                domain =
                                                                    when {
                                                                        entityId.startsWith("button.") -> "button"
                                                                        entityId.startsWith(
                                                                            "input_button.",
                                                                        ) -> "input_button"
                                                                        else -> "button" // 默认使用button
                                                                    },
                                                                service = "press",
                                                                entityId = entityId,
                                                                data = JSONObject(),
                                                                onComplete = {
                                                                    // 延迟状态重置，让动画有更好的显示效果
                                                                    scope.launch {
                                                                        kotlinx.coroutines.delay(1000)
                                                                        isLoading = false
                                                                    }
                                                                    Toast.makeText(
                                                                        context,
                                                                        "发送成功",
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                },
                                                                onError = { error ->
                                                                    // 延迟状态重置，让动画有更好的显示效果
                                                                    scope.launch {
                                                                        kotlinx.coroutines.delay(1000)
                                                                        isLoading = false
                                                                    }
                                                                    Toast.makeText(
                                                                        context,
                                                                        "发送失败: $error",
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                },
                                                            )
                                                        },
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .height(48.dp),
                                                        enabled = !isLoading,
                                                    ) {
                                                        if (isLoading) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(24.dp),
                                                                color = MaterialTheme.colorScheme.onPrimary,
                                                                strokeWidth = 2.dp,
                                                            )
                                                        } else {
                                                            Text("执行")
                                                        }
                                                    }
                                                }
                                            } else {
                                                var inputValue by remember { mutableStateOf("") }
                                                var isLoading by remember { mutableStateOf(false) }
                                                OutlinedTextField(
                                                    value = inputValue,
                                                    onValueChange = { inputValue = it },
                                                    modifier =
                                                        Modifier
                                                            .weight(1f)
                                                            .padding(end = 8.dp),
                                                    placeholder = { Text("请输入要发送的内容") },
                                                    singleLine = true,
                                                )
                                                Button(
                                                    onClick = {
                                                        isLoading = true
                                                        val entityId =
                                                            card.topic.removePrefix(
                                                                "homeassistant/",
                                                            ).removeSuffix("/state")
                                                        haManager.callService(
                                                            domain =
                                                                when {
                                                                    entityId.startsWith("number.") -> "number"
                                                                    entityId.startsWith("input_text.") -> "input_text"
                                                                    entityId.startsWith(
                                                                        "input_number.",
                                                                    ) -> "input_number"
                                                                    else -> "input_text" // 默认使用input_text
                                                                },
                                                            service =
                                                                when {
                                                                    entityId.startsWith("number.") -> "set_value"
                                                                    entityId.startsWith("input_number.") -> "set_value"
                                                                    entityId.startsWith("input_text.") -> "set_value"
                                                                    else -> "set_value"
                                                                },
                                                            entityId = entityId,
                                                            data =
                                                                JSONObject().apply {
                                                                    put("value", inputValue)
                                                                },
                                                            onComplete = {
                                                                // 延迟状态重置，让动画有更好的显示效果
                                                                scope.launch {
                                                                    kotlinx.coroutines.delay(1000)
                                                                    isLoading = false
                                                                    inputValue = ""
                                                                }
                                                                Toast.makeText(
                                                                    context,
                                                                    "发送成功",
                                                                    Toast.LENGTH_SHORT,
                                                                ).show()
                                                            },
                                                            onError = { error ->
                                                                // 延迟状态重置，让动画有更好的显示效果
                                                                scope.launch {
                                                                    kotlinx.coroutines.delay(1000)
                                                                    isLoading = false
                                                                }
                                                                Toast.makeText(
                                                                    context,
                                                                    "发送失败: $error",
                                                                    Toast.LENGTH_SHORT,
                                                                ).show()
                                                            },
                                                        )
                                                    },
                                                    enabled = inputValue.isNotBlank() && !isLoading,
                                                ) {
                                                    if (isLoading) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            strokeWidth = 2.dp,
                                                        )
                                                    } else {
                                                        Text("发送")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                text =
                                                    when (val value = cardValues[cardId]) {
                                                        null -> "等待数据..."
                                                        "unknown" -> "状态未知"
                                                        else -> "$value${card.unitSuffix}"
                                                    },
                                                style = MaterialTheme.typography.headlineMedium,
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

        FloatingActionButton(
            onClick = {
                if (mqttManager.isConnected()) {
                    editingCard = null
                    showSubscribeDialog = true
                } else {
                    // 显示Snackbar提示用户先连接MQTT服务器
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "请先连接EMQX服务器，然后才可以新增加设备",
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加监控参数")
        }

        // 添加SnackbarHost
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp), // 让Snackbar显示在FAB上方的位置
        )
    }

    // 添加历史数据底部弹出菜单
    if (showHistoryBottomSheet && selectedSensorCard != null) {
        SensorHistoryBottomSheet(
            sensorName = selectedSensorCard!!.displayName,
            unitSuffix = selectedSensorCard!!.unitSuffix,
            historyData = sensorHistoryData,
            onDismiss = { showHistoryBottomSheet = false },
            onClearHistory = {
                val cardId = "${selectedSensorCard!!.topic}:${selectedSensorCard!!.jsonParam}"
                historyManager.clearHistory(cardId)
                sensorHistoryData = emptyList()
                Toast.makeText(context, "历史记录已清除", Toast.LENGTH_SHORT).show()
            },
        )
    }

    if (showSubscribeDialog) {
        MqttSubscribeDialog(
            onDismissRequest = {
                showSubscribeDialog = false
                editingCard = null
            },
            editingCard = editingCard,
            onDelete =
                editingCard?.let { card ->
                    {
                        val currentCount = topicSubscriptionCount[card.topic] ?: 1
                        if (currentCount <= 1) {
                            subscriptionController.unsubscribeTopic(card.topic)
                            topicSubscriptionCount = topicSubscriptionCount - card.topic
                        } else {
                            topicSubscriptionCount = topicSubscriptionCount + (card.topic to (currentCount - 1))
                        }

                        val cardId = "${card.topic}:${card.jsonParam}"
                        val newCards = subscriptionCards.filter { it != card }
                        subscriptionCards = newCards
                        cardValues = cardValues - cardId
                        SubscriptionCardStorage.saveCards(context, newCards)

                        showSubscribeDialog = false
                        editingCard = null

                        Toast.makeText(context, "已删除监控卡片", Toast.LENGTH_SHORT).show()
                    }
                },
            onSubscribe = { card ->
                val previousCard = editingCard
                val previousTopic = previousCard?.topic

                // 如果是编辑模式，先移除旧卡片
                if (previousCard != null) {
                    subscriptionCards = subscriptionCards.filter { it != previousCard }

                    if (previousTopic != null && previousTopic != card.topic) {
                        val previousCount = topicSubscriptionCount[previousTopic] ?: 1
                        if (previousCount <= 1) {
                            subscriptionController.unsubscribeTopic(previousTopic)
                            topicSubscriptionCount = topicSubscriptionCount - previousTopic
                        } else {
                            topicSubscriptionCount = topicSubscriptionCount + (previousTopic to (previousCount - 1))
                        }
                    }
                }

                // 检查是否已经订阅了相同的主题和参数（仅在新增时检查）
                if (editingCard == null &&
                    card.deviceType == DeviceType.SENSOR &&
                    subscriptionCards.any {
                        it.topic == card.topic &&
                            it.jsonParam == card.jsonParam &&
                            it.deviceType == DeviceType.SENSOR
                    }
                ) {
                    Toast.makeText(context, "该参数已经被监控", Toast.LENGTH_SHORT).show()
                    return@MqttSubscribeDialog
                }

                // 添加新卡片
                val newCards = subscriptionCards + card
                subscriptionCards = newCards
                SubscriptionCardStorage.saveCards(context, newCards)

                // 如果是新主题，进行订阅
                if (!topicSubscriptionCount.containsKey(card.topic)) {
                    subscriptionController.subscribeTopic(card.topic)
                }

                // 更新主题订阅计数
                val currentCount = topicSubscriptionCount[card.topic] ?: 0
                topicSubscriptionCount = topicSubscriptionCount + (card.topic to (currentCount + 1))

                Toast.makeText(context, if (editingCard != null) "已更新监控参数" else "已添加监控参数", Toast.LENGTH_SHORT).show()
                showSubscribeDialog = false
                editingCard = null
            },
        )
    }
}
