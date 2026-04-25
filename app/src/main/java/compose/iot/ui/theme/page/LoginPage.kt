package compose.iot.ui.theme.page

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import compose.iot.R
import compose.iot.data.preferences.PreferencesManager
import compose.iot.mqtt.MqttManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(
    navController: NavController? = null,
    mqttManager: MqttManager,
    preferencesManager: PreferencesManager,
) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(mqttManager.isConnected()) }

    var serverIp by remember { mutableStateOf(preferencesManager.mqttServerIp) }
    var serverPort by remember { mutableStateOf(preferencesManager.mqttServerPort) }
    var clientId by remember { mutableStateOf(preferencesManager.mqttClientId) }
    var autoConnect by remember { mutableStateOf(preferencesManager.mqttAutoConnect) }
    var mqttVersion by remember { mutableStateOf(preferencesManager.mqttVersion) }
    var username by remember { mutableStateOf(preferencesManager.mqttUsername ?: "ASLant") }
    var password by remember { mutableStateOf(preferencesManager.mqttPassword ?: "") }

    fun connectToMqtt() {
        preferencesManager.mqttServerIp = serverIp
        preferencesManager.mqttServerPort = serverPort
        preferencesManager.mqttClientId = clientId
        preferencesManager.mqttUsername = username
        preferencesManager.mqttPassword = password
        preferencesManager.mqttVersion = mqttVersion
        preferencesManager.mqttAutoConnect = autoConnect
        val serverUri = "tcp://$serverIp:$serverPort"
        mqttManager.setMqttVersion(mqttVersion)
        mqttManager.setServerUri(serverUri)
        mqttManager.setClientId(clientId)
        mqttManager.setUsername(username)
        mqttManager.setPassword(password)
        mqttManager.connect(
            onConnectComplete = {
                isConnected = true
                Toast.makeText(context, context.getString(R.string.server_connection_success), Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                isConnected = false
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            },
        )
    }

    compose.iot.ui.components.AppScaffold(
        title = stringResource(R.string.mqtt_connection_settings),
        navController = navController,
    ) { _ ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 动态颜色变化的状态卡片
            val statusColor by animateColorAsState(
                targetValue = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                animationSpec = tween(500),
                label = "statusColor",
            )
            val onStatusColor by animateColorAsState(
                targetValue = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                animationSpec = tween(500),
                label = "onStatusColor",
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = statusColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isConnected) TablerIcons.CircleCheck else TablerIcons.AlertTriangle,
                        contentDescription = null,
                        tint = onStatusColor,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isConnected) stringResource(R.string.server_connected) else stringResource(R.string.server_not_connected),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = onStatusColor,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isConnected) "$serverIp:$serverPort" else stringResource(R.string.check_network_or_config),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onStatusColor.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            // MQTT配置区域标题
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.network_credentials),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.height(32.dp),
                    ) {
                        SegmentedButton(
                            selected = mqttVersion == 3,
                            onClick = { mqttVersion = 3 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text(stringResource(R.string.mqtt_version_311), style = MaterialTheme.typography.labelMedium) }
                        SegmentedButton(
                            selected = mqttVersion == 5,
                            onClick = { mqttVersion = 5 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text(stringResource(R.string.mqtt_version_50), style = MaterialTheme.typography.labelMedium) }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = { serverIp = it },
                        label = { Text(stringResource(R.string.server_address)) },
                        leadingIcon = { Icon(TablerIcons.MapPin, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        label = { Text(stringResource(R.string.port)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        modifier = Modifier.weight(0.3f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                }

                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text(stringResource(R.string.client_id)) },
                    leadingIcon = { Icon(TablerIcons.MoodSmile, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )

                Text(
                    text = stringResource(R.string.authentication),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username)) },
                    leadingIcon = { Icon(TablerIcons.User, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    leadingIcon = { Icon(TablerIcons.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = MaterialTheme.shapes.small,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 操作区
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(stringResource(R.string.background_auto_connect), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.background_auto_connect_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoConnect,
                            onCheckedChange = {
                                autoConnect = it
                                preferencesManager.mqttAutoConnect = it
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!isConnected) {
                            connectToMqtt()
                        } else {
                            mqttManager.disconnect()
                            isConnected = false
                            Toast.makeText(context, context.getString(R.string.mqtt_disconnected), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp),
                ) {
                    Icon(
                        imageVector = if (isConnected) TablerIcons.X else TablerIcons.Plus,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isConnected) stringResource(R.string.disconnect_current_connection) else stringResource(R.string.establish_secure_connection),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
