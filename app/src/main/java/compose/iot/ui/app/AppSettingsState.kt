package compose.iot.ui.app

import androidx.compose.runtime.Immutable

@Immutable
data class AppSettingsState(
    val selectedTab: AppTab = AppTab.INDEX,
    val cornerShapeLevel: Int = 1,
    val appKeepAlive: Boolean = false,
    val darkMode: Int = 0,
    val themeColor: Int = 0,
    val predictiveBackEnabled: Boolean = true,
)

enum class AppTab {
    INDEX,
    DASH,
    SETTINGS,
    ABOUT,
}
