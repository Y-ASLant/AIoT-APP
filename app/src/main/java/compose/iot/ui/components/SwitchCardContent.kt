package compose.iot.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import compose.iot.R

/**
 * 开关控制卡片内容
 *
 * @param isOn 当前开关状态
 * @param isLoading 是否正在发送指令（禁用交互）
 * @param onToggle 用户切换开关时的回调
 */
@Composable
fun SwitchCardContent(
    isOn: Boolean,
    isLoading: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.switch_off),
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (!isOn) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
        )
        Switch(
            checked = isOn,
            enabled = !isLoading,
            onCheckedChange = onToggle,
        )
        Text(
            text = stringResource(R.string.switch_on),
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (isOn) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
        )
    }
}
