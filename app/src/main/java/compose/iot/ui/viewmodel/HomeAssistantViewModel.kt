package compose.iot.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import compose.iot.R
import compose.iot.data.homeassistant.HADevice
import compose.iot.data.homeassistant.HomeAssistantRepository
import compose.iot.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class HomeAssistantViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val repository: HomeAssistantRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

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
            emitToast(appContext.getString(R.string.ha_fill_all_fields))
            return
        }

        val intervalValue = state.pollingInterval.toIntOrNull()
        if (intervalValue == null || intervalValue < 1) {
            emitToast(appContext.getString(R.string.ha_polling_interval_invalid))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val normalizedServerUrl = repository.normalizeServerUrl(state.serverUrl)
            val result = repository.fetchDevices(normalizedServerUrl, state.accessToken)

            result
                .onSuccess { devices ->
                    prefs.haServerUrl = normalizedServerUrl
                    prefs.haAccessToken = state.accessToken
                    prefs.haPollingInterval = intervalValue
                    _uiState.update {
                        it.copy(
                            serverUrl = normalizedServerUrl,
                            devices = devices,
                            showDeviceList = true,
                            isLoading = false,
                        )
                    }
                    emitToast(appContext.getString(R.string.ha_connected_successfully))
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    emitToast(it.message ?: appContext.getString(R.string.ha_failed_to_connect))
                }
        }
    }

    fun refreshDevices() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.accessToken.isBlank()) {
            emitToast(appContext.getString(R.string.ha_not_configured))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.fetchDevices(repository.normalizeServerUrl(state.serverUrl), state.accessToken)
                .onSuccess { devices ->
                    _uiState.update { it.copy(devices = devices, isLoading = false) }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    emitToast(it.message ?: appContext.getString(R.string.ha_failed_to_refresh))
                }
        }
    }

    fun addDevice(device: HADevice) {
        viewModelScope.launch {
            repository.addDevice(device)
                .onSuccess { card ->
                    repository.persistInitialActuatorState(appContext, card, device.state)
                    emitToast(appContext.getString(R.string.ha_added_device, device.friendlyName))
                }.onFailure {
                    emitToast(it.message ?: appContext.getString(R.string.ha_failed_to_add_device))
                }
        }
    }

    private fun emitToast(message: String) {
        _events.trySend(HomeAssistantEvent.ShowToast(message))
    }
}
