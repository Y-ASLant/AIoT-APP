package compose.iot.ui.app

import androidx.lifecycle.ViewModel
import compose.iot.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<AppSettingsState> = _uiState.asStateFlow()

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateCornerShapeLevel(level: Int) {
        prefs.cornerShapeLevel = level
        _uiState.update { it.copy(cornerShapeLevel = level) }
    }

    fun updateAppKeepAlive(enabled: Boolean) {
        prefs.appKeepAlive = enabled
        _uiState.update { it.copy(appKeepAlive = enabled) }
    }

    fun updateDarkMode(mode: Int) {
        prefs.darkMode = mode
        _uiState.update {
            it.copy(
                darkMode = mode,
                themeColor = if (mode == 0) 0 else it.themeColor,
            )
        }
        if (mode == 0) {
            prefs.themeColor = 0
        }
    }

    fun updateThemeColor(colorId: Int) {
        prefs.themeColor = colorId
        _uiState.update { it.copy(themeColor = colorId) }
    }

    fun updatePredictiveBack(enabled: Boolean) {
        prefs.predictiveBackEnabled = enabled
        _uiState.update { it.copy(predictiveBackEnabled = enabled) }
    }

    private fun loadState() =
        AppSettingsState(
            cornerShapeLevel = prefs.cornerShapeLevel,
            appKeepAlive = prefs.appKeepAlive,
            darkMode = prefs.darkMode,
            themeColor = prefs.themeColor,
            predictiveBackEnabled = prefs.predictiveBackEnabled,
        )
}
