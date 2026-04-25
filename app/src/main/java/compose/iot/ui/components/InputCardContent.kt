package compose.iot.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import compose.iot.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 文本输入控制卡片内容
 *
 * 用户输入文本并点击发送，发送成功后清空输入框。
 *
 * @param onSend 发送文本时的回调，提供 onComplete 和 onError 回调
 */
@Composable
fun InputCardContent(
    onSend: (text: String, onComplete: () -> Unit, onError: (String) -> Unit) -> Unit,
) {
    var inputValue by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            placeholder = { Text(stringResource(R.string.input_placeholder)) },
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            enabled = !isLoading,
            singleLine = true,
        )
        Button(
            onClick = {
                isLoading = true
                onSend(
                    inputValue,
                    {
                        // onComplete
                        scope.launch {
                            delay(1000)
                            isLoading = false
                            inputValue = ""
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
            modifier = Modifier.height(48.dp),
            enabled = inputValue.isNotBlank() && !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.send))
            }
        }
    }
}
