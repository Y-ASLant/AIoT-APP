package compose.iot.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import compose.iot.R
import compose.iot.data.video.VideoStreamConfig
import compose.iot.data.video.VideoStreamEvent
import compose.iot.data.video.VideoStreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoStreamUiState(
    val config: VideoStreamConfig = VideoStreamConfig(),
    val isUrlTested: Boolean = false,
    val isConnectionSuccessful: Boolean = false,
    val isTestingConnection: Boolean = false,
    val connectionErrorMessage: String = "",
    val isStreamingActive: Boolean = false,
    val frameBitmap: Bitmap? = null,
    val messageCount: Int = 0,
    val lastMessage: String = "",
    val errorMessage: String = "",
)

sealed interface VideoStreamEffect {
    data class ShowMessage(
        val message: String,
    ) : VideoStreamEffect
}

@HiltViewModel
class VideoStreamViewModel @Inject constructor(
    private val repository: VideoStreamRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoStreamUiState(config = repository.loadConfig()))
    val uiState: StateFlow<VideoStreamUiState> = _uiState.asStateFlow()

    private val _effects = Channel<VideoStreamEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var streamJob: Job? = null

    fun updateConfig(transform: (VideoStreamConfig) -> VideoStreamConfig) {
        _uiState.update {
            it.copy(
                config = transform(it.config),
                isUrlTested = false,
                isConnectionSuccessful = false,
                connectionErrorMessage = "",
            )
        }
    }

    fun saveConfig() {
        repository.saveConfig(_uiState.value.config)
    }

    fun testConnection() {
        val config = _uiState.value.config
        if (config.serverUrl.isBlank() || config.port.isBlank()) {
            _uiState.update {
                it.copy(
                    isUrlTested = true,
                    isConnectionSuccessful = false,
                    connectionErrorMessage = appContext.getString(R.string.video_stream_required_server_port),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, isUrlTested = false) }
            val (success, errorMessage) = repository.testConnection(config)
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    isUrlTested = true,
                    isConnectionSuccessful = success,
                    connectionErrorMessage = errorMessage,
                )
            }
        }
    }

    fun startStreaming() {
        val config = _uiState.value.config
        stopStreaming()
        _uiState.update { it.copy(isStreamingActive = true, errorMessage = "", frameBitmap = null) }
        streamJob =
            viewModelScope.launch {
                repository.stream(config).collect { event ->
                    when (event) {
                        is VideoStreamEvent.Frame -> {
                            _uiState.update {
                                it.copy(
                                    frameBitmap = event.bitmap,
                                    messageCount = event.messageCount,
                                    errorMessage = "",
                                )
                            }
                        }
                        is VideoStreamEvent.Waiting -> {
                            _uiState.update {
                                it.copy(
                                    frameBitmap = null,
                                    messageCount = event.messageCount,
                                    lastMessage = event.lastMessage,
                                    errorMessage = event.errorMessage,
                                )
                            }
                        }
                        is VideoStreamEvent.Failed -> {
                            _uiState.update {
                                it.copy(
                                    isStreamingActive = false,
                                    isConnectionSuccessful = false,
                                    connectionErrorMessage = event.message,
                                )
                            }
                            _effects.trySend(VideoStreamEffect.ShowMessage(event.message))
                        }
                    }
                }
            }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { it.copy(isStreamingActive = false, frameBitmap = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
    }
}
