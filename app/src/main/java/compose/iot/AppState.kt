package compose.iot

import androidx.compose.runtime.mutableIntStateOf

/**
 * 全局导航状态
 */
object AppState {
    val selectedTab = mutableIntStateOf(0)
    val cornerShapeLevel = mutableIntStateOf(1) // 0: Small(8dp), 1: Medium(12dp), 2: Large(16dp)
    val appKeepAlive = androidx.compose.runtime.mutableStateOf(false)
}
