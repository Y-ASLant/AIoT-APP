@file:Suppress("DefaultLocale")

package compose.iot.ui.theme.page

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.TablerIcons
import compose.icons.tablericons.Plus
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
fun IndexPage(viewModel: IndexViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val callbacks =
        remember(viewModel) {
            IndexPageCallbacks(
                onShowAddDialog = viewModel::showAddDialog,
                onSelectDeviceType = viewModel::selectDeviceType,
                onShowHistory = viewModel::showHistory,
                onEditCard = viewModel::editCard,
                onToggleSwitch = viewModel::toggleSwitch,
                onChangeSlider = viewModel::changeSlider,
                onToggleContinuousMode = viewModel::toggleContinuousMode,
                onPressButton = viewModel::pressButton,
                onSendInput = viewModel::sendInput,
                onDismissHistory = viewModel::dismissHistory,
                onClearHistory = viewModel::clearHistory,
                onDismissDialog = viewModel::dismissDialog,
                onSaveCard = viewModel::saveCard,
                onDeleteCard = viewModel::deleteCard,
            )
        }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.device_center), fontWeight = FontWeight.SemiBold) },
                scrollBehavior = scrollBehavior,
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        floatingActionButton = {
            FloatingActionButton(
                onClick = callbacks.onShowAddDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(TablerIcons.Plus, contentDescription = stringResource(R.string.add_device))
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
                    callbacks.onSelectDeviceType(targetType)
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
            ) {
                SegmentedButton(
                    selected = uiState.selectedDeviceType == DeviceType.SENSOR,
                    onClick = {
                        callbacks.onSelectDeviceType(DeviceType.SENSOR)
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text(text = stringResource(R.string.device_type_sensor))
                }
                SegmentedButton(
                    selected = uiState.selectedDeviceType == DeviceType.ACTUATOR,
                    onClick = {
                        callbacks.onSelectDeviceType(DeviceType.ACTUATOR)
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text(text = stringResource(R.string.device_type_actuator))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val type = if (page == 0) DeviceType.SENSOR else DeviceType.ACTUATOR
                val filteredCards by remember(page, uiState.subscriptionCards) {
                    derivedStateOf { uiState.subscriptionCards.filter { it.deviceType == type } }
                }

                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = rememberLazyStaggeredGridState(),
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
                                callbacks = callbacks,
                            )
                        }
                    }
                }
            }
        }
    }

    val selectedSensorCard = uiState.selectedSensorCard
    if (uiState.showHistoryBottomSheet && selectedSensorCard != null) {
        SensorHistoryBottomSheet(
            sensorName = selectedSensorCard.displayName,
            unitSuffix = selectedSensorCard.unitSuffix,
            historyData = uiState.sensorHistoryData,
            onDismiss = callbacks.onDismissHistory,
            onClearHistory = callbacks.onClearHistory,
        )
    }

    if (uiState.showSubscribeDialog) {
        MqttSubscribeDialog(
            onDismissRequest = callbacks.onDismissDialog,
            editingCard = uiState.editingCard,
            onDelete = uiState.editingCard?.let { card -> { callbacks.onDeleteCard(card) } },
            onSubscribe = { card -> callbacks.onSaveCard(card, uiState.editingCard) },
        )
    }
}

@Stable
private data class IndexPageCallbacks(
    val onShowAddDialog: () -> Unit,
    val onSelectDeviceType: (DeviceType) -> Unit,
    val onShowHistory: (SubscriptionCard) -> Unit,
    val onEditCard: (SubscriptionCard) -> Unit,
    val onToggleSwitch: (SubscriptionCard, Boolean) -> Unit,
    val onChangeSlider: (SubscriptionCard, Float) -> Unit,
    val onToggleContinuousMode: (Boolean) -> Unit,
    val onPressButton: (SubscriptionCard, onComplete: () -> Unit, onError: (String) -> Unit) -> Unit,
    val onSendInput: (SubscriptionCard, String, onComplete: () -> Unit, onError: (String) -> Unit) -> Unit,
    val onDismissHistory: () -> Unit,
    val onClearHistory: () -> Unit,
    val onDismissDialog: () -> Unit,
    val onSaveCard: (SubscriptionCard, SubscriptionCard?) -> Unit,
    val onDeleteCard: (SubscriptionCard) -> Unit,
)

@Composable
private fun EmptyState(deviceType: DeviceType) {
    val iconId = if (deviceType == DeviceType.SENSOR) R.drawable.fluentiot24regular else R.drawable.terminalboxline
    val typeText =
        if (deviceType == DeviceType.SENSOR) {
            stringResource(R.string.device_type_sensor)
        } else {
            stringResource(R.string.device_type_actuator)
        }

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
                contentDescription = stringResource(R.string.empty_device_content_description, typeText),
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = stringResource(R.string.empty_device_title, typeText),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.empty_device_subtitle, typeText),
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
    callbacks: IndexPageCallbacks,
) {
    val cardModifier =
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = {
                    if (card.deviceType == DeviceType.SENSOR) callbacks.onShowHistory(card)
                },
                onLongClick = { callbacks.onEditCard(card) },
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
                CardBody(card, value, isLoading, continuousSliderMode, callbacks)
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
                onClick = {},
                label = {
                    Text(
                        when (card.deviceType) {
                            DeviceType.SENSOR -> stringResource(R.string.device_type_sensor)
                            DeviceType.ACTUATOR -> stringResource(R.string.device_type_actuator)
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
                onClick = {},
                label = {
                    Text(
                        when (card.serverType) {
                            ServerType.EMQX -> stringResource(R.string.server_type_emqx)
                            ServerType.HomeAssistant -> stringResource(R.string.server_type_ha)
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
    callbacks: IndexPageCallbacks,
) {
    if (card.deviceType == DeviceType.SENSOR) {
        SensorCardContent(value = value, unitSuffix = card.unitSuffix)
    } else {
        when {
            card.isButtonStyle -> {
                SwitchCardContent(
                    isOn = card.isSwitchOn(value),
                    isLoading = isLoading,
                    onToggle = { newState -> callbacks.onToggleSwitch(card, newState) },
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
                    onValueChange = { v -> callbacks.onChangeSlider(card, v) },
                    onValueChangeFinished = { v -> callbacks.onChangeSlider(card, v) },
                    onToggleContinuousMode = callbacks.onToggleContinuousMode,
                )
            }
            card.isPushButtonStyle -> {
                PushButtonCardContent(
                    onPress = { onComplete, onError -> callbacks.onPressButton(card, onComplete, onError) },
                )
            }
            else -> {
                InputCardContent(
                    onSend = { text, onComplete, onError -> callbacks.onSendInput(card, text, onComplete, onError) },
                )
            }
        }
    }
}
