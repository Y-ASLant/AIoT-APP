package compose.iot.ui.viewmodel

import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.SensorHistoryData
import compose.iot.mqtt.SubscriptionCard

/**
 * IndexPage 的 UI 状态
 */
data class IndexUiState(
    val subscriptionCards: List<SubscriptionCard> = emptyList(),
    val cardValues: Map<String, String> = emptyMap(),
    val selectedDeviceType: DeviceType = DeviceType.SENSOR,
    val continuousSliderMode: Boolean = false,
    val isInitialDataReady: Boolean = false,
    val loadingCards: Set<String> = emptySet(),
    val showSubscribeDialog: Boolean = false,
    val editingCard: SubscriptionCard? = null,
    val showHistoryBottomSheet: Boolean = false,
    val selectedSensorCard: SubscriptionCard? = null,
    val sensorHistoryData: List<SensorHistoryData> = emptyList(),
)

/**
 * 一次性 UI 事件
 */
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}
