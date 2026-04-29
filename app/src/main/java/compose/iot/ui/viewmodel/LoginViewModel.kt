package compose.iot.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import compose.iot.R
import compose.iot.data.preferences.PreferencesManager
import compose.iot.mqtt.MqttManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class LoginUiState(
    val serverIp: String = "",
    val serverPort: String = "1883",
    val clientId: String = "",
    val autoConnect: Boolean = false,
    val mqttVersion: Int = 3,
    val username: String = "",
    val password: String = "",
    val isConnected: Boolean = false,
)

sealed interface LoginEffect {
    data class ShowMessage(
        val message: String,
    ) : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val mqttManager: MqttManager,
    private val preferencesManager: PreferencesManager,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            LoginUiState(
                serverIp = preferencesManager.mqttServerIp,
                serverPort = preferencesManager.mqttServerPort,
                clientId = preferencesManager.mqttClientId,
                autoConnect = preferencesManager.mqttAutoConnect,
                mqttVersion = preferencesManager.mqttVersion,
                username = preferencesManager.mqttUsername ?: "ASLant",
                password = preferencesManager.mqttPassword ?: "",
                isConnected = mqttManager.isConnected(),
            ),
        )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun updateServerIp(value: String) {
        _uiState.update { it.copy(serverIp = value) }
    }

    fun updateServerPort(value: String) {
        _uiState.update { it.copy(serverPort = value) }
    }

    fun updateClientId(value: String) {
        _uiState.update { it.copy(clientId = value) }
    }

    fun updateAutoConnect(value: Boolean) {
        preferencesManager.mqttAutoConnect = value
        _uiState.update { it.copy(autoConnect = value) }
    }

    fun updateMqttVersion(value: Int) {
        _uiState.update { it.copy(mqttVersion = value) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun connectOrDisconnect() {
        if (_uiState.value.isConnected) {
            mqttManager.disconnect()
            _uiState.update { it.copy(isConnected = false) }
            emitMessage(appContext.getString(R.string.mqtt_disconnected))
            return
        }

        val state = _uiState.value
        persistConfig(state)
        mqttManager.setMqttVersion(state.mqttVersion)
        mqttManager.setServerUri("tcp://${state.serverIp}:${state.serverPort}")
        mqttManager.setClientId(state.clientId)
        mqttManager.setUsername(state.username)
        mqttManager.setPassword(state.password)
        mqttManager.connect(
            onConnectComplete = {
                _uiState.update { it.copy(isConnected = true) }
                emitMessage(appContext.getString(R.string.server_connection_success))
            },
            onError = { error ->
                _uiState.update { it.copy(isConnected = false) }
                emitMessage(error)
            },
        )
    }

    private fun persistConfig(state: LoginUiState) {
        preferencesManager.mqttServerIp = state.serverIp
        preferencesManager.mqttServerPort = state.serverPort
        preferencesManager.mqttClientId = state.clientId
        preferencesManager.mqttUsername = state.username
        preferencesManager.mqttPassword = state.password
        preferencesManager.mqttVersion = state.mqttVersion
        preferencesManager.mqttAutoConnect = state.autoConnect
    }

    private fun emitMessage(message: String) {
        _effects.trySend(LoginEffect.ShowMessage(message))
    }
}
