package compose.iot.ui.theme.function

import androidx.compose.animation.*
import androidx.compose.animation.core.*

/**
 * 标准页面进入动画
 */
fun standardEnterTransition(
    initialOffsetY: Int = 300,
    durationMillis: Int = 300,
    delayMillis: Int = 0
): EnterTransition = slideInVertically(
    animationSpec = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = EaseOutQuart
    )
) { initialOffsetY } + fadeIn(
    animationSpec = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = EaseOutQuart
    )
)

/**
 * 标准页面退出动画
 */
fun standardExitTransition(
    targetOffsetY: Int = 300,
    durationMillis: Int = 300,
    delayMillis: Int = 0
): ExitTransition = slideOutVertically(
    animationSpec = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = EaseInQuart
    )
) { targetOffsetY } + fadeOut(
    animationSpec = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = EaseInQuart
    )
)

// 自定义缓动曲线
private val EaseInQuart = CubicBezierEasing(0.5f, 0f, 0.75f, 0f)
private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
