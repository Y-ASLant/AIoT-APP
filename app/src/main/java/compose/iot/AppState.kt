package compose.iot

import androidx.compose.runtime.mutableIntStateOf

/**
 * 全局导航状态
 */
object AppState {
    val selectedTab = mutableIntStateOf(0)
    val cornerShapeLevel = mutableIntStateOf(1) // 0: Small(8dp), 1: Medium(12dp), 2: Large(16dp)
    val appKeepAlive = androidx.compose.runtime.mutableStateOf(false)
    val darkMode = mutableIntStateOf(0) // 0: Auto, 1: Light, 2: Dark
    val themeColor = mutableIntStateOf(0) // 0: Dynamic, 1: Green, 2: Blue, 3: Orange, 4: Purple, 5: Red
    val predictiveBackEnabled = androidx.compose.runtime.mutableStateOf(true) // Default true for Android 14+
}
