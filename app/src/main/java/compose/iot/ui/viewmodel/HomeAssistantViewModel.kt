package compose.iot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import compose.iot.AiotApp
import compose.iot.data.preferences.PreferencesManager
import compose.iot.mqtt.CardStyle
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.ServerType
import compose.iot.mqtt.SubscriptionCard
import compose.iot.ui.theme.page.SubscriptionCardStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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

data class HomeAssistantUiState(
    val serverUrl: String = "",
    val accessToken: String = "",
    val pollingInterval: String = "5",
    val isLoading: Boolean = false,
    val devices: List<HADevice> = emptyList(),
    val showDeviceList: Boolean = false,
    val searchQuery: String = "",
)

sealed class HomeAssistantEvent {
    data class ShowToast(val message: String) : HomeAssistantEvent()
}

class HomeAssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AiotApp
    private val prefs: PreferencesManager = app.preferencesManager
    private val dao = app.appDatabase.subscriptionCardDao()

    private val _uiState =
        MutableStateFlow(
            HomeAssistantUiState(
                serverUrl = prefs.haServerUrl,
                accessToken = prefs.haAccessToken,
                pollingInterval = prefs.haPollingInterval.toString(),
            ),
        )
    val uiState: StateFlow<HomeAssistantUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeAssistantEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun updateServerUrl(value: String) {
        _uiState.update { it.copy(serverUrl = value) }
    }

    fun updateAccessToken(value: String) {
        _uiState.update { it.copy(accessToken = value) }
    }

    fun updatePollingInterval(value: String) {
        if (value.isEmpty() || value.toIntOrNull() != null) {
            _uiState.update { it.copy(pollingInterval = value) }
        }
    }

    fun updateSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    fun connectAndLoadDevices() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.accessToken.isBlank() || state.pollingInterval.isBlank()) {
            emitToast("请填写所有必填项")
            return
        }

        val intervalValue = state.pollingInterval.toIntOrNull()
        if (intervalValue == null || intervalValue < 1) {
            emitToast("轮询间隔必须是大于0的整数")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val normalizedServerUrl = normalizeServerUrl(state.serverUrl)
            if (!testConnection(normalizedServerUrl, state.accessToken)) {
                _uiState.update { it.copy(isLoading = false) }
                emitToast("连接失败，请检查配置")
                return@launch
            }

            prefs.haServerUrl = normalizedServerUrl
            prefs.haAccessToken = state.accessToken
            prefs.haPollingInterval = intervalValue

            val devices = fetchDevices(normalizedServerUrl, state.accessToken)
            _uiState.update {
                it.copy(
                    serverUrl = normalizedServerUrl,
                    devices = devices,
                    showDeviceList = true,
                    isLoading = false,
                )
            }
            emitToast("连接成功")
        }
    }

    fun refreshDevices() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.accessToken.isBlank()) {
            emitToast("Home Assistant 服务器未配置")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val devices = fetchDevices(normalizeServerUrl(state.serverUrl), state.accessToken)
            _uiState.update { it.copy(devices = devices, isLoading = false) }
        }
    }

    fun addDevice(device: HADevice) {
        viewModelScope.launch {
            val card = buildCard(device)
            val existingCards = dao.getAllCards()
            if (existingCards.any { it.topic == card.topic && it.jsonParam == card.jsonParam }) {
                emitToast("该设备已添加")
                return@launch
            }

            dao.insertCards(listOf(card))
            persistInitialActuatorState(card, device.state)
            emitToast("已添加设备：${device.friendlyName}")
        }
    }

    private fun buildCard(device: HADevice): SubscriptionCard =
        SubscriptionCard(
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
                    device.entityId.startsWith("light.") -> DeviceType.ACTUATOR
                    device.deviceClass in listOf("temperature", "humidity", "pressure", "illuminance") -> DeviceType.SENSOR
                    device.deviceClass in listOf("light", "fan", "cover", "button") -> DeviceType.ACTUATOR
                    else -> DeviceType.SENSOR
                },
            unitSuffix =
                when (device.deviceClass) {
                    "temperature" -> "°C"
                    "humidity" -> "%"
                    else -> ""
                },
            isButtonStyle = device.entityId.startsWith("switch.") || device.entityId.startsWith("light."),
            isSliderStyle = device.entityId.startsWith("number."),
            isPushButtonStyle = device.entityId.startsWith("button."),
            buttonValue = "1",
        )

    private fun persistInitialActuatorState(
        card: SubscriptionCard,
        state: String,
    ) {
        if (card.deviceType != DeviceType.ACTUATOR) return

        val cardId = "${card.topic}:${card.jsonParam}"
        val persistedValue =
            when {
                card.isSliderStyle -> state.toFloatOrNull()?.toString() ?: "0"
                else -> state
            }
        SubscriptionCardStorage.persistActuatorValue(getApplication(), card, cardId, persistedValue)
    }

    private suspend fun testConnection(
        serverUrl: String,
        accessToken: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val connection =
                    (URL("$serverUrl/api/").openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Authorization", "Bearer $accessToken")
                        setRequestProperty("Content-Type", "application/json")
                        connectTimeout = 5000
                        readTimeout = 5000
                        instanceFollowRedirects = true
                    }

                try {
                    connection.responseCode == 200
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Timber.e(e, "HA连接异常")
                false
            }
        }

    private suspend fun fetchDevices(
        serverUrl: String,
        accessToken: String,
    ): List<HADevice> =
        withContext(Dispatchers.IO) {
            try {
                val connection =
                    (URL("$serverUrl/api/states").openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Authorization", "Bearer $accessToken")
                        setRequestProperty("Content-Type", "application/json")
                        connectTimeout = 5000
                        readTimeout = 5000
                        instanceFollowRedirects = true
                    }

                try {
                    if (connection.responseCode != 200) {
                        val errorMessage = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                        Timber.w("HA获取设备失败: HTTP ${connection.responseCode} - $errorMessage")
                        return@withContext emptyList()
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    buildList {
                        JSONArray(response).let { array ->
                            for (i in 0 until array.length()) {
                                val item = array.getJSONObject(i)
                                val entityId = item.getString("entity_id")
                                val attributes = item.getJSONObject("attributes")
                                add(
                                    HADevice(
                                        entityId = entityId,
                                        friendlyName = attributes.optString("friendly_name", entityId),
                                        state = item.getString("state"),
                                        deviceClass = attributes.optString("device_class").takeIf { it.isNotEmpty() },
                                    ),
                                )
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Timber.e(e, "HA获取设备异常")
                emptyList()
            }
        }

    private fun normalizeServerUrl(serverUrl: String): String {
        var finalUrl = serverUrl.trim()
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            finalUrl = "http://$finalUrl"
        }
        if (finalUrl.endsWith("/")) {
            finalUrl = finalUrl.dropLast(1)
        }
        return finalUrl
    }

    private fun emitToast(message: String) {
        _events.trySend(HomeAssistantEvent.ShowToast(message))
    }
}
