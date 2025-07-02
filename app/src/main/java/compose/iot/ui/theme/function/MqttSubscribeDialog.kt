package compose.iot.ui.theme.function

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.iot.mqtt.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun MqttSubscribeDialog(
    onDismissRequest: () -> Unit,
    onSubscribe: (SubscriptionCard) -> Unit,
    onDelete: (() -> Unit)? = null,
    editingCard: SubscriptionCard? = null
) {
    var topic by remember { mutableStateOf(editingCard?.topic ?: "") }
    var displayName by remember { mutableStateOf(editingCard?.displayName ?: "") }
    var jsonParam by remember { mutableStateOf(editingCard?.jsonParam ?: "") }
    var unitSuffix by remember { mutableStateOf(editingCard?.unitSuffix ?: "") }
    var cardStyle by remember { mutableStateOf(editingCard?.cardStyle ?: CardStyle.FILLED) }
    var deviceType by remember { mutableStateOf(editingCard?.deviceType ?: DeviceType.SENSOR) }
    var serverType by remember { mutableStateOf(editingCard?.serverType ?: ServerType.EMQX) }
    var isButtonStyle by remember { mutableStateOf(editingCard?.isButtonStyle == true) }
    var isSliderStyle by remember { mutableStateOf(editingCard?.isSliderStyle == true) }
    var isPushButtonStyle by remember { mutableStateOf(editingCard?.isPushButtonStyle == true) }
    var switchOnValue by remember { mutableStateOf(editingCard?.switchOnValue ?: "1") }
    var switchOffValue by remember { mutableStateOf(editingCard?.switchOffValue ?: "0") }
    var buttonValue by remember { mutableStateOf(editingCard?.buttonValue ?: "1") }
    var sliderMin by remember { mutableStateOf(editingCard?.sliderMin?.toString() ?: "0") }
    var sliderMax by remember { mutableStateOf(editingCard?.sliderMax?.toString() ?: "100") }
    var sliderStep by remember { mutableStateOf(editingCard?.sliderStep?.toString() ?: "1") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = if (editingCard != null) "编辑设备" else "添加设备") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 服务类型选择
                Text("服务类型", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ServerType.entries.forEach { type ->
                        FilterChip(
                            selected = serverType == type,
                            onClick = { serverType = type },
                            label = {
                                Text(when (type) {
                                    ServerType.EMQX -> "EMQX"
                                    ServerType.HomeAssistant -> "Home Assistant"
                                })
                            }
                        )
                    }
                }

                // 设备类型选择
                Text("设备类型", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeviceType.entries.forEach { type ->
                        FilterChip(
                            selected = deviceType == type,
                            onClick = { deviceType = type },
                            label = {
                                Text(when (type) {
                                    DeviceType.SENSOR -> "传感器"
                                    DeviceType.ACTUATOR -> "执行器"
                                })
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("MQTT主题或HA实体ID") },
                    placeholder = { Text("例如: device/status") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("显示名称") },
                    placeholder = { Text("例如: 温度") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = jsonParam,
                    onValueChange = { jsonParam = it },
                    label = { Text("MQTT消息JSON字段") },
                    placeholder = { Text("例如: state") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = unitSuffix,
                    onValueChange = { unitSuffix = it },
                    label = { Text("单位后缀（可选）") },
                    placeholder = { Text("例如: °C") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 如果是执行器，显示控制类型选择
                if (deviceType == DeviceType.ACTUATOR) {
                    Text("控制类型", style = MaterialTheme.typography.labelMedium)
                    
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = isButtonStyle,
                            onClick = { 
                                isButtonStyle = true 
                                isSliderStyle = false
                                isPushButtonStyle = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                        ) {
                            Text("开关")
                        }
                        SegmentedButton(
                            selected = isSliderStyle,
                            onClick = { 
                                isButtonStyle = false 
                                isSliderStyle = true
                                isPushButtonStyle = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
                        ) {
                            Text("滑块")
                        }
                        SegmentedButton(
                            selected = isPushButtonStyle,
                            onClick = { 
                                isButtonStyle = false 
                                isSliderStyle = false
                                isPushButtonStyle = true
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
                        ) {
                            Text("按钮")
                        }
                        SegmentedButton(
                            selected = !isButtonStyle && !isSliderStyle && !isPushButtonStyle,
                            onClick = { 
                                isButtonStyle = false 
                                isSliderStyle = false
                                isPushButtonStyle = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
                        ) {
                            Text("输入")
                        }
                    }

                    // 根据所选控制类型显示不同的配置选项
                    if (isButtonStyle) {
                        OutlinedTextField(
                            value = switchOnValue,
                            onValueChange = { switchOnValue = it },
                            label = { Text("开启时的值") },
                            placeholder = { Text("例如: 1") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = switchOffValue,
                            onValueChange = { switchOffValue = it },
                            label = { Text("关闭时的值") },
                            placeholder = { Text("例如: 0") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // 滑块配置
                    if (isSliderStyle) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = sliderMin,
                                onValueChange = { sliderMin = it },
                                label = { Text("最小值") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            
                            OutlinedTextField(
                                value = sliderMax,
                                onValueChange = { sliderMax = it },
                                label = { Text("最大值") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        
                        OutlinedTextField(
                            value = sliderStep,
                            onValueChange = { sliderStep = it },
                            label = { Text("步进值") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    
                    // 按钮配置
                    if (isPushButtonStyle) {
                        OutlinedTextField(
                            value = buttonValue,
                            onValueChange = { buttonValue = it },
                            label = { Text("按钮值") },
                            placeholder = { Text("按钮点击时发送的值") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 卡片样式选择
                Text("卡片样式", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CardStyle.entries.forEach { style ->
                        FilterChip(
                            selected = cardStyle == style,
                            onClick = { cardStyle = style },
                            label = {
                                Text(when (style) {
                                    CardStyle.HIGHLIGHT -> "高亮"
                                    CardStyle.MINIMAL -> "简约"
                                    CardStyle.FILLED -> "填充"
                                })
                            }
                        )
                    }
                }

                // 删除按钮（仅在编辑模式下显示）
                if (editingCard != null && onDelete != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("删除此卡片")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (topic.isNotBlank() && displayName.isNotBlank() && jsonParam.isNotBlank()) {
                        onSubscribe(
                            SubscriptionCard(
                                topic = topic,
                                displayName = displayName,
                                jsonParam = jsonParam,
                                unitSuffix = unitSuffix,
                                cardStyle = cardStyle,
                                deviceType = deviceType,
                                serverType = serverType,
                                isButtonStyle = if (deviceType == DeviceType.ACTUATOR) isButtonStyle else false,
                                isSliderStyle = if (deviceType == DeviceType.ACTUATOR) isSliderStyle else false,
                                isPushButtonStyle = if (deviceType == DeviceType.ACTUATOR) isPushButtonStyle else false,
                                switchOnValue = if (deviceType == DeviceType.ACTUATOR) switchOnValue else "1",
                                switchOffValue = if (deviceType == DeviceType.ACTUATOR) switchOffValue else "0",
                                buttonValue = if (deviceType == DeviceType.ACTUATOR) buttonValue else "1",
                                sliderMin = if (deviceType == DeviceType.ACTUATOR) sliderMin.toFloatOrNull() ?: 0f else 0f,
                                sliderMax = if (deviceType == DeviceType.ACTUATOR) sliderMax.toFloatOrNull() ?: 100f else 100f,
                                sliderStep = if (deviceType == DeviceType.ACTUATOR) sliderStep.toFloatOrNull() ?: 1f else 1f
                            )
                        )
                    }
                },
                enabled = topic.isNotBlank() && displayName.isNotBlank() && jsonParam.isNotBlank()
            ) {
                Text(if (editingCard != null) "更新" else "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}