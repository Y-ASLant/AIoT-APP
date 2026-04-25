@file:Suppress("DefaultLocale")

package compose.iot.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import compose.iot.R

/**
 * 滑块控制卡片内容
 *
 * 支持「实时控制」和「松手发送」两种模式，自动处理本地拖拽位置与外部值同步。
 *
 * @param externalValue 由 ViewModel 提供的当前值（从服务器/持久化获取）
 * @param min 滑块最小值
 * @param max 滑块最大值
 * @param step 滑块步进
 * @param unitSuffix 数值单位后缀
 * @param continuousMode 是否启用实时控制模式
 * @param onValueChange 实时控制模式下每次滑动时回调
 * @param onValueChangeFinished 松手发送模式下松手时回调
 * @param onToggleContinuousMode 切换实时控制模式
 */
@Composable
fun SliderCardContent(
    externalValue: Float,
    min: Float,
    max: Float,
    step: Float,
    unitSuffix: String,
    continuousMode: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    onToggleContinuousMode: (Boolean) -> Unit,
) {
    // 本地滑块位置（拖拽时立即更新，保证流畅）
    var sliderPosition by remember { mutableFloatStateOf(externalValue) }
    var isDragging by remember { mutableStateOf(false) }

    // 仅在未拖拽时同步外部值
    LaunchedEffect(externalValue) {
        if (!isDragging) {
            sliderPosition = externalValue
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 数值显示 + 实时控制开关
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = String.format("%.1f", sliderPosition) + unitSuffix,
                style = MaterialTheme.typography.titleLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.slider_realtime_control),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Switch(
                    checked = continuousMode,
                    onCheckedChange = onToggleContinuousMode,
                    modifier = Modifier.scale(0.7f),
                )
            }
        }

        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                isDragging = true
                sliderPosition = newValue
                if (continuousMode) {
                    val formatted = String.format("%.1f", newValue).toFloat()
                    onValueChange(formatted)
                }
            },
            onValueChangeFinished = {
                isDragging = false
                val formatted = String.format("%.1f", sliderPosition).toFloat()
                if (!continuousMode) {
                    onValueChangeFinished(formatted)
                }
            },
            valueRange = min..max,
            steps = ((max - min) / step).toInt() - 1,
        )

        // 最小/最大值标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = String.format("%.1f", min),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                text = String.format("%.1f", max),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}
