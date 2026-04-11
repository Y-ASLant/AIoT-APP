package compose.iot.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 一键按钮控制卡片内容
 *
 * 按下后进入 1 秒加载动画，然后恢复可点击状态。
 *
 * @param onPress 按下按钮时的回调，提供 onComplete 和 onError 回调
 */
@Composable
fun PushButtonCardContent(
    onPress: (onComplete: () -> Unit, onError: (String) -> Unit) -> Unit,
) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                isLoading = true
                onPress(
                    {
                        // onComplete — 延迟重置让动画有更好的显示效果
                        scope.launch {
                            delay(1000)
                            isLoading = false
                        }
                    },
                    {
                        // onError
                        scope.launch {
                            delay(1000)
                            isLoading = false
                        }
                    },
                )
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            enabled = !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("执行")
            }
        }
    }
}
