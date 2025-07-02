package compose.iot

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import compose.iot.ui.theme.ui.theme.AIOT_ComposeTheme
import compose.iot.ui.theme.function.Background
import compose.iot.ui.theme.function.Page_Switch
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import compose.iot.mqtt.MqttManager
import compose.iot.ui.theme.page.*
import compose.iot.ui.theme.page.video.Page_VideoStream
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

// 全局状态
object AppState {
    val selectedTab = mutableIntStateOf(0)
}

class MainActivity : ComponentActivity() {
    private lateinit var mqttManager: MqttManager

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装 SplashScreen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // 初始化 MQTT Manager
        mqttManager = MqttManager()
        
        // 检查是否需要自动连接
        val sharedPreferences = getSharedPreferences("mqtt_settings", MODE_PRIVATE)
        val autoConnect = sharedPreferences.getBoolean("auto_connect", false)
        if (autoConnect) {
            val serverIp = sharedPreferences.getString("server_ip", "mqtt.aslant.top") ?: "mqtt.aslant.top"
            val serverPort = sharedPreferences.getString("server_port", "1883") ?: "1883"
            val clientId = sharedPreferences.getString("client_id", "ComposeApplication") ?: "ComposeApplication"
            val username = sharedPreferences.getString("username", null)
            val password = sharedPreferences.getString("password", null)
            val serverUri = "tcp://$serverIp:$serverPort"
            mqttManager.setServerUri(serverUri)
            mqttManager.setClientId(clientId)
            mqttManager.setUsername(username)
            mqttManager.setPassword(password)
            mqttManager.connect(
                onConnectComplete = {
                    Toast.makeText(this, "服务器连接成功", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        enableEdgeToEdge()
        setContent {
            AIOT_ComposeTheme {
                Background() //全局背景
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "index") {
                    composable("index") {
                        Page_Switch(scope, navController, mqttManager)
                    }
                    composable("login") {
                        Page_Login(navController, mqttManager)
                    }
                    composable("homeassistant") {
                        Page_HomeAssistant(navController)
                    }
                    composable("changelog") {
                        Page_Changelog()
                    }
                    composable("video_stream") {
                        Page_VideoStream(navController)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttManager.disconnect()
    }
}
