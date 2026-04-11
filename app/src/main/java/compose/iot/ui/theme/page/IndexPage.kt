@file:Suppress("DefaultLocale")

package compose.iot.ui.theme.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.iot.R
import compose.iot.mqtt.CardStyle
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.ServerType
import compose.iot.mqtt.SubscriptionCard
import compose.iot.mqtt.cardId
import compose.iot.mqtt.isSwitchOn
import compose.iot.ui.components.InputCardContent
import compose.iot.ui.components.PushButtonCardContent
import compose.iot.ui.components.SensorCardContent
import compose.iot.ui.components.SliderCardContent
import compose.iot.ui.components.SwitchCardContent
import compose.iot.ui.theme.function.MqttSubscribeDialog
import compose.iot.ui.theme.function.SensorHistoryBottomSheet
import compose.iot.ui.theme.function.standardEnterTransition
import compose.iot.ui.theme.function.standardExitTransition
import compose.iot.ui.viewmodel.IndexViewModel
import compose.iot.ui.viewmodel.UiEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IndexPage(viewModel: IndexViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 标题滚动可见性（纯 UI 状态，保留在 Composable）
    val gridState = rememberLazyStaggeredGridState()
    var previousFirstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var isTitleVisible by remember { mutableStateOf(true) }

    LaunchedEffect(remember { derivedStateOf { gridState.firstVisibleItemIndex } }) {
        if (gridState.firstVisibleItemIndex > previousFirstVisibleItemIndex) {
            isTitleVisible = false
        } else if (gridState.firstVisibleItemIndex < previousFirstVisibleItemIndex) {
            isTitleVisible = true
        }
        previousFirstVisibleItemIndex = gridState.firstVisibleItemIndex
    }

    // 收集一次性事件（统一走 Snackbar）
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (viewModel.mqttManager.isConnected()) {
                        viewModel.showAddDialog()
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "请先连接EMQX服务器，然后才可以新增加设备",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加监控参数")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            // ── 顶部标题 + 设备类型筛选 ──
            DeviceTypeHeader(
                isTitleVisible = isTitleVisible,
                selectedDeviceType = uiState.selectedDeviceType,
                onSelectType = viewModel::selectDeviceType,
            )

            // ── 设备卡片网格 ──
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
            ) {
                val filteredCards =
                    uiState.subscriptionCards.filter {
                        it.deviceType == uiState.selectedDeviceType
                    }

                if (filteredCards.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) { EmptyState() }
                } else {
                    items(
                        items = filteredCards,
                        key = { card -> card.cardId },
                        span = { card ->
                            if (card.deviceType == DeviceType.ACTUATOR) {
                                StaggeredGridItemSpan.FullLine
                            } else {
                                StaggeredGridItemSpan.SingleLane
                            }
                        },
                    ) { card ->
                        DeviceCardItem(
                            card = card,
                            value = uiState.cardValues[card.cardId],
                            isLoading = card.cardId in uiState.loadingCards,
                            continuousSliderMode = uiState.continuousSliderMode,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }

    // ── 历史数据底部弹出 ──
    if (uiState.showHistoryBottomSheet && uiState.selectedSensorCard != null) {
        SensorHistoryBottomSheet(
            sensorName = uiState.selectedSensorCard!!.displayName,
            unitSuffix = uiState.selectedSensorCard!!.unitSuffix,
            historyData = uiState.sensorHistoryData,
            onDismiss = viewModel::dismissHistory,
            onClearHistory = viewModel::clearHistory,
        )
    }

    // ── 订阅对话框 ──
    if (uiState.showSubscribeDialog) {
        MqttSubscribeDialog(
            onDismissRequest = viewModel::dismissDialog,
            editingCard = uiState.editingCard,
            onDelete = uiState.editingCard?.let { card -> { viewModel.deleteCard(card) } },
            onSubscribe = { card ->
                viewModel.saveCard(card, uiState.editingCard)
            },
        )
    }
}

// region ── 子组件 ──

@Composable
private fun DeviceTypeHeader(
    isTitleVisible: Boolean,
    selectedDeviceType: DeviceType,
    onSelectType: (DeviceType) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
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
                    Text(text = "设备中心", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth(0.98f)) {
                    SegmentedButton(
                        selected = selectedDeviceType == DeviceType.SENSOR,
                        onClick = { onSelectType(DeviceType.SENSOR) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text(text = "传感器", modifier = Modifier.padding(horizontal = 24.dp))
                    }
                    SegmentedButton(
                        selected = selectedDeviceType == DeviceType.ACTUATOR,
                        onClick = { onSelectType(DeviceType.ACTUATOR) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text(text = "执行器", modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceCardItem(
    card: SubscriptionCard,
    value: String?,
    isLoading: Boolean,
    continuousSliderMode: Boolean,
    viewModel: IndexViewModel,
) {
    val cardModifier =
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = {
                    if (card.deviceType == DeviceType.SENSOR) viewModel.showHistory(card)
                },
                onLongClick = { viewModel.editCard(card) },
            )

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardHeader(card)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardBody(card, value, isLoading, continuousSliderMode, viewModel)
            }
        }
    }

    when (card.cardStyle) {
        CardStyle.HIGHLIGHT ->
            ElevatedCard(
                modifier = cardModifier,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                content = cardContent,
            )
        CardStyle.MINIMAL ->
            OutlinedCard(
                modifier = cardModifier,
                colors =
                    CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                content = cardContent,
            )
        CardStyle.FILLED ->
            Card(
                modifier = cardModifier,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                content = cardContent,
            )
    }
}

@Composable
private fun CardHeader(card: SubscriptionCard) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = card.displayName, style = MaterialTheme.typography.titleMedium)
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

@Composable
private fun CardBody(
    card: SubscriptionCard,
    value: String?,
    isLoading: Boolean,
    continuousSliderMode: Boolean,
    viewModel: IndexViewModel,
) {
    if (card.deviceType == DeviceType.SENSOR) {
        SensorCardContent(value = value, unitSuffix = card.unitSuffix)
    } else {
        when {
            card.isButtonStyle -> {
                SwitchCardContent(
                    isOn = card.isSwitchOn(value),
                    isLoading = isLoading,
                    onToggle = { newState -> viewModel.toggleSwitch(card, newState) },
                )
            }
            card.isSliderStyle -> {
                val floatValue = value?.toFloatOrNull() ?: card.sliderMin
                SliderCardContent(
                    externalValue = floatValue,
                    min = card.sliderMin,
                    max = card.sliderMax,
                    step = card.sliderStep,
                    unitSuffix = card.unitSuffix,
                    continuousMode = continuousSliderMode,
                    onValueChange = { v -> viewModel.changeSlider(card, v) },
                    onValueChangeFinished = { v -> viewModel.changeSlider(card, v) },
                    onToggleContinuousMode = viewModel::toggleContinuousMode,
                )
            }
            card.isPushButtonStyle -> {
                PushButtonCardContent(
                    onPress = { onComplete, onError ->
                        viewModel.pressButton(card, onComplete, onError)
                    },
                )
            }
            else -> {
                InputCardContent(
                    onSend = { text, onComplete, onError ->
                        viewModel.sendInput(card, text, onComplete, onError)
                    },
                )
            }
        }
    }
}

// endregion
