@file:Suppress("DefaultLocale")

package compose.iot.ui.theme.page

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.*
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
import compose.iot.ui.viewmodel.IndexViewModel
import compose.iot.ui.viewmodel.UiEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IndexPage(viewModel: IndexViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 收集一次性事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("设备中心", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) },
                scrollBehavior = scrollBehavior,
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.showAddDialog()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(TablerIcons.Plus, contentDescription = "添加监控参数")
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
            val pagerState =
                rememberPagerState(
                    initialPage = if (uiState.selectedDeviceType == DeviceType.SENSOR) 0 else 1,
                    pageCount = { 2 },
                )

            LaunchedEffect(pagerState.currentPage) {
                val targetType = if (pagerState.currentPage == 0) DeviceType.SENSOR else DeviceType.ACTUATOR
                if (uiState.selectedDeviceType != targetType) {
                    viewModel.selectDeviceType(targetType)
                }
            }

            // ── 设备类型筛选 ──
            SingleChoiceSegmentedButtonRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
            ) {
                SegmentedButton(
                    selected = uiState.selectedDeviceType == DeviceType.SENSOR,
                    onClick = {
                        viewModel.selectDeviceType(DeviceType.SENSOR)
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text(text = "传感器")
                }
                SegmentedButton(
                    selected = uiState.selectedDeviceType == DeviceType.ACTUATOR,
                    onClick = {
                        viewModel.selectDeviceType(DeviceType.ACTUATOR)
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text(text = "执行器")
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val type = if (page == 0) DeviceType.SENSOR else DeviceType.ACTUATOR

                val filteredCards by remember(page, uiState.subscriptionCards) {
                    derivedStateOf {
                        uiState.subscriptionCards.filter {
                            it.deviceType == type
                        }
                    }
                }

                val pageGridState = rememberLazyStaggeredGridState()

                // ── 设备卡片网格 ──
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = pageGridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                ) {
                    if (filteredCards.isEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) { EmptyState(type) }
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
private fun EmptyState(deviceType: DeviceType) {
    val iconId = if (deviceType == DeviceType.SENSOR) R.drawable.fluentiot24regular else R.drawable.terminalboxline
    val typeText = if (deviceType == DeviceType.SENSOR) "传感器" else "执行器"

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = "暂无$typeText",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = "还没有${typeText}设备",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "点击右下角 + 按钮添加$typeText",
                style = MaterialTheme.typography.bodyMedium,
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
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                content = cardContent,
            )
        CardStyle.MINIMAL ->
            OutlinedCard(
                modifier = cardModifier,
                content = cardContent,
            )
        CardStyle.FILLED ->
            Card(
                modifier = cardModifier,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                content = cardContent,
            )
    }
}

@Composable
private fun CardHeader(card: SubscriptionCard) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = card.displayName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SuggestionChip(
                onClick = { },
                label = {
                    Text(
                        when (card.deviceType) {
                            DeviceType.SENSOR -> "传感器"
                            DeviceType.ACTUATOR -> "执行器"
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.fluentiot24regular),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                },
                colors =
                    SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                border = null,
            )
            SuggestionChip(
                onClick = { },
                label = {
                    Text(
                        when (card.serverType) {
                            ServerType.EMQX -> "EMQX"
                            ServerType.HomeAssistant -> "HA"
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors =
                    SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                border = null,
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
