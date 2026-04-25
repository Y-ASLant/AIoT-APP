package compose.iot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import compose.iot.R

/**
 * 传感器数据展示卡片内容
 *
 * @param value 当前传感器值（null 表示尚未接收到数据）
 * @param unitSuffix 数值单位后缀
 */
@Composable
fun SensorCardContent(
    value: String?,
    unitSuffix: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text =
                when (value) {
                    null -> stringResource(R.string.sensor_waiting)
                    "unknown" -> stringResource(R.string.sensor_unknown)
                    else -> "$value$unitSuffix"
                },
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
