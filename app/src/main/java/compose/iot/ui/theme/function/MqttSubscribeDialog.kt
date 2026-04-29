package compose.iot.ui.theme.function

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import compose.iot.R
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import compose.iot.mqtt.*

private val transparentSegmentedColors
    @Composable get() = SegmentedButtonDefaults.colors(
        inactiveContainerColor = androidx.compose.ui.graphics.Color.Transparent,
    )

@Composable
fun MqttSubscribeDialog(
    onDismissRequest: () -> Unit,
    onSubscribe: (SubscriptionCard) -> Unit,
    onDelete: (() -> Unit)? = null,
    editingCard: SubscriptionCard? = null,
) {
    var topic by remember(editingCard) { mutableStateOf(editingCard?.topic ?: "") }
    var displayName by remember(editingCard) { mutableStateOf(editingCard?.displayName ?: "") }
    var jsonParam by remember(editingCard) { mutableStateOf(editingCard?.jsonParam ?: "") }
    var unitSuffix by remember(editingCard) { mutableStateOf(editingCard?.unitSuffix ?: "") }
    var cardStyle by remember(editingCard) { mutableStateOf(editingCard?.cardStyle ?: CardStyle.FILLED) }
    var cardSize by remember(editingCard) { mutableStateOf(editingCard?.cardSize ?: CardSize.S1x1) }
    var deviceType by remember(editingCard) { mutableStateOf(editingCard?.deviceType ?: DeviceType.SENSOR) }
    var serverType by remember(editingCard) { mutableStateOf(editingCard?.serverType ?: ServerType.EMQX) }
    var isButtonStyle by remember(editingCard) { mutableStateOf(editingCard?.isButtonStyle == true) }
    var isSliderStyle by remember(editingCard) { mutableStateOf(editingCard?.isSliderStyle == true) }
    var isPushButtonStyle by remember(editingCard) { mutableStateOf(editingCard?.isPushButtonStyle == true) }
    var switchOnValue by remember(editingCard) { mutableStateOf(editingCard?.switchOnValue ?: "1") }
    var switchOffValue by remember(editingCard) { mutableStateOf(editingCard?.switchOffValue ?: "0") }
    var buttonValue by remember(editingCard) { mutableStateOf(editingCard?.buttonValue ?: "1") }
    var sliderMin by remember(editingCard) { mutableStateOf(editingCard?.sliderMin?.toString() ?: "0") }
    var sliderMax by remember(editingCard) { mutableStateOf(editingCard?.sliderMax?.toString() ?: "100") }
    var sliderStep by remember(editingCard) { mutableStateOf(editingCard?.sliderStep?.toString() ?: "1") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(if (editingCard != null) R.string.dialog_edit_device else R.string.dialog_add_device)) },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 服务类型选择
                Text(stringResource(R.string.service_type), style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ServerType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = serverType == type,
                            onClick = { serverType = type },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ServerType.entries.size),
                            colors = transparentSegmentedColors,
                        ) {
                            Text(
                                when (type) {
                                    ServerType.EMQX -> "EMQX"
                                    ServerType.HomeAssistant -> stringResource(R.string.home_assistant)
                                },
                            )
                        }
                    }
                }

                // 设备类型选择
                Text(stringResource(R.string.device_type), style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DeviceType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = deviceType == type,
                            onClick = { deviceType = type },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = DeviceType.entries.size),
                            colors = transparentSegmentedColors,
                        ) {
                            Text(
                                when (type) {
                                    DeviceType.SENSOR -> stringResource(R.string.device_type_sensor)
                                    DeviceType.ACTUATOR -> stringResource(R.string.device_type_actuator)
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text(stringResource(R.string.topic_or_entity_id)) },
                    placeholder = { Text(stringResource(R.string.example_device_status)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.display_name)) },
                    placeholder = { Text(stringResource(R.string.example_temperature)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = jsonParam,
                    onValueChange = { jsonParam = it },
                    label = { Text(stringResource(R.string.mqtt_json_field)) },
                    placeholder = { Text(stringResource(R.string.example_state)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = unitSuffix,
                    onValueChange = { unitSuffix = it },
                    label = { Text(stringResource(R.string.unit_suffix_optional)) },
                    placeholder = { Text(stringResource(R.string.example_unit_celsius)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // 如果是执行器，显示控制类型选择
                if (deviceType == DeviceType.ACTUATOR) {
                    Text(stringResource(R.string.control_type), style = MaterialTheme.typography.labelMedium)

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SegmentedButton(
                            selected = isButtonStyle,
                            onClick = {
                                isButtonStyle = true
                                isSliderStyle = false
                                isPushButtonStyle = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                            colors = transparentSegmentedColors,
                        ) {
                            Text(stringResource(R.string.control_switch))
                        }
                        SegmentedButton(
                            selected = isSliderStyle,
                            onClick = {
                                isButtonStyle = false
                                isSliderStyle = true
                                isPushButtonStyle = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
                            colors = transparentSegmentedColors,
                        ) {
                            Text(stringResource(R.string.control_slider))
                        }
                        SegmentedButton(
                            selected = isPushButtonStyle,
                            onClick = {
                                isButtonStyle = false
                                isSliderStyle = false
                                isPushButtonStyle = true
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
                            colors = transparentSegmentedColors,
                        ) {
                            Text(stringResource(R.string.control_button))
                        }
                        SegmentedButton(
                            selected = !isButtonStyle && !isSliderStyle && !isPushButtonStyle,
                            onClick = {
                                isButtonStyle = false
                                isSliderStyle = false
                                isPushButtonStyle = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
                            colors = transparentSegmentedColors,
                        ) {
                            Text(stringResource(R.string.control_input))
                        }
                    }

                    // 根据所选控制类型显示不同的配置选项
                    if (isButtonStyle) {
                        OutlinedTextField(
                            value = switchOnValue,
                            onValueChange = { switchOnValue = it },
                            label = { Text(stringResource(R.string.switch_on_value)) },
                            placeholder = { Text(stringResource(R.string.example_one)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = switchOffValue,
                            onValueChange = { switchOffValue = it },
                            label = { Text(stringResource(R.string.switch_off_value)) },
                            placeholder = { Text(stringResource(R.string.example_zero)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }

                    // 滑块配置
                    if (isSliderStyle) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = sliderMin,
                                onValueChange = { sliderMin = it },
                                label = { Text(stringResource(R.string.minimum_value)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )

                            OutlinedTextField(
                                value = sliderMax,
                                onValueChange = { sliderMax = it },
                                label = { Text(stringResource(R.string.maximum_value)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }

                        OutlinedTextField(
                            value = sliderStep,
                            onValueChange = { sliderStep = it },
                            label = { Text(stringResource(R.string.step_value)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }

                    // 按钮配置
                    if (isPushButtonStyle) {
                        OutlinedTextField(
                            value = buttonValue,
                            onValueChange = { buttonValue = it },
                            label = { Text(stringResource(R.string.button_value)) },
                            placeholder = { Text(stringResource(R.string.button_value_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // 卡片样式选择
                Text(stringResource(R.string.card_style), style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CardStyle.entries.forEachIndexed { index, style ->
                        SegmentedButton(
                            selected = cardStyle == style,
                            onClick = { cardStyle = style },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = CardStyle.entries.size),
                            colors = transparentSegmentedColors,
                        ) {
                            Text(
                                when (style) {
                                    CardStyle.HIGHLIGHT -> stringResource(R.string.card_style_highlight)
                                    CardStyle.MINIMAL -> stringResource(R.string.card_style_minimal)
                                    CardStyle.FILLED -> stringResource(R.string.card_style_filled)
                                },
                            )
                        }
                    }
                }

                Text(stringResource(R.string.card_size), style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CardSize.entries.forEachIndexed { index, size ->
                        SegmentedButton(
                            selected = cardSize == size,
                            onClick = { cardSize = size },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = CardSize.entries.size),
                            colors = transparentSegmentedColors,
                        ) {
                            Text(
                                when (size) {
                                    CardSize.S1x1 -> stringResource(R.string.card_size_1x1)
                                    CardSize.S1x2 -> stringResource(R.string.card_size_1x2)
                                    CardSize.S2x1 -> stringResource(R.string.card_size_2x1)
                                    CardSize.S2x2 -> stringResource(R.string.card_size_2x2)
                                },
                            )
                        }
                    }
                }

                // 删除按钮（仅在编辑模式下显示）
                if (editingCard != null && onDelete != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Icon(
                            TablerIcons.Trash,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_card))
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
                                cardSize = cardSize,
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
                                sliderStep = if (deviceType == DeviceType.ACTUATOR) sliderStep.toFloatOrNull() ?: 1f else 1f,
                            ),
                        )
                    }
                },
                enabled = topic.isNotBlank() && displayName.isNotBlank() && jsonParam.isNotBlank(),
            ) {
                Text(stringResource(if (editingCard != null) R.string.update else R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
