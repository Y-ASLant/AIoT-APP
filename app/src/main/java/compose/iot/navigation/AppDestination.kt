package compose.iot.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppDestination {
    @Serializable
    data object Root : AppDestination

    @Serializable
    data object Login : AppDestination

    @Serializable
    data object HomeAssistant : AppDestination

    @Serializable
    data object Changelog : AppDestination

    @Serializable
    data object VideoStream : AppDestination

    @Serializable
    data object Bluetooth : AppDestination

    @Serializable
    data object AppTheme : AppDestination
}

enum class RootTab {
    INDEX,
    DASH,
    SETTINGS,
    ABOUT,
}
