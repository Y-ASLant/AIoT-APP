package compose.iot.ui.theme.page

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import compose.iot.mqtt.MqttManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.edit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page_Login(navController: NavController? = null, mqttManager: MqttManager) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(mqttManager.isConnected()) }

    // 从SharedPreferences读取保存的配置
    val sharedPreferences =
        remember { context.getSharedPreferences("mqtt_settings", Context.MODE_PRIVATE) }
    var serverIp by remember {
        mutableStateOf(
            sharedPreferences.getString("server_ip", "mqtt.aslant.top") ?: ""
        )
    }
    var serverPort by remember {
        mutableStateOf(
            sharedPreferences.getString("server_port", "1883") ?: ""
        )
    }
    var clientId by remember {
        mutableStateOf(
            sharedPreferences.getString(
                "client_id",
                "ComposeApplication"
            ) ?: ""
        )
    }
    var autoConnect by remember {
        mutableStateOf(
            sharedPreferences.getBoolean(
                "auto_connect",
                false
            )
        )
    }
    var username by remember {
        mutableStateOf(
            sharedPreferences.getString("username", "ASLant") ?: "ASLant"
        )
    }
    var password by remember { mutableStateOf(sharedPreferences.getString("password", "") ?: "") }

    fun connectToMqtt(
        mqttManager: MqttManager,
        serverIp: String,
        serverPort: Int,
        clientId: String,
        username: String?,
        password: String?,
        context: Context,
        onConnectionResult: (Boolean) -> Unit
    ) {
        val serverUri = "tcp://$serverIp:$serverPort"
        mqttManager.setServerUri(serverUri)
        mqttManager.setClientId(clientId)
        mqttManager.setUsername(username)
        mqttManager.setPassword(password)
        mqttManager.connect(
            onConnectComplete = {
                onConnectionResult(true)
                Toast.makeText(context, "服务器连接成功", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                onConnectionResult(false)
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MQTT 配置") },
                navigationIcon = {
                    IconButton(onClick = { navController?.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // MQTT状态卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isConnected) "服务器已连接" else "服务器未连接",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (isConnected) "服务器: $serverIp:$serverPort\nClient ID: $clientId" else "请配置MQTT服务器",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // MQTT配置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MQTT服务器配置",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = { serverIp = it },
                        label = { Text("服务器地址") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        label = { Text("端口号") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        label = { Text("Client ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation()
                    )


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("开启自动连接")
                        Switch(
                            checked = autoConnect,
                            onCheckedChange = {
                                autoConnect = it
                                // 保存自动连接设置
                                sharedPreferences.edit { putBoolean("auto_connect", it) }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isConnected) {
                                    // 保存配置
                                    sharedPreferences.edit {
                                        putString("server_ip", serverIp)
                                        putString("server_port", serverPort)
                                        putString("client_id", clientId)
                                        putString("username", username)
                                        putString("password", password)
                                    }
                                    connectToMqtt(
                                        mqttManager,
                                        serverIp,
                                        serverPort.toIntOrNull() ?: 1883,
                                        clientId,
                                        username,
                                        password,
                                        context
                                    ) { success ->
                                        isConnected = success
                                    }
                                } else {
                                    mqttManager.disconnect()
                                    isConnected = false
                                    Toast.makeText(context, "MQTT服务器已断开", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (isConnected) "断开连接" else "连接服务器")
                        }
                    }
                }
            }


        }
    }
}
