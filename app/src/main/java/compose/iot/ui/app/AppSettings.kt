package compose.iot.ui.app

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppSettingsViewModel =
    staticCompositionLocalOf<AppSettingsViewModel> {
        error("AppSettingsViewModel not provided")
    }
