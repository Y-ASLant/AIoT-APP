package compose.iot

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import compose.iot.mqtt.MqttForegroundService
import compose.iot.ui.theme.function.Background
import compose.iot.ui.theme.function.MainScaffold
import compose.iot.ui.theme.page.*
import compose.iot.ui.theme.page.video.VideoStreamPage
import compose.iot.ui.theme.ui.theme.AIOT_ComposeTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val app = application as AiotApp
        val mqttManager = app.mqttManager
        val prefs = app.preferencesManager

        // 初始化界面状态
        AppState.cornerShapeLevel.intValue = prefs.cornerShapeLevel
        AppState.appKeepAlive.value = prefs.appKeepAlive

        if (prefs.appKeepAlive) {
            MqttForegroundService.start(this)
        }

        // 自动连接 MQTT（配置已在 AiotApp.onCreate 中设置）
        if (prefs.mqttAutoConnect) {
            mqttManager.connect(
                onConnectComplete = {
                    timber.log.Timber.i("MQTT 自动连接成功")
                },
                onError = { error ->
                    timber.log.Timber.w("MQTT 自动连接失败: %s", error)
                },
            )
        }

        enableEdgeToEdge()
        setContent {
            AIOT_ComposeTheme {
                Background()
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "index") {
                    composable("index") {
                        MainScaffold(scope, navController)
                    }
                    composable("login") {
                        LoginPage(navController, mqttManager)
                    }
                    composable("homeassistant") {
                        HomeAssistantPage(navController)
                    }
                    composable("changelog") {
                        ChangelogPage()
                    }
                    composable("video_stream") {
                        VideoStreamPage(navController)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val prefs = (application as AiotApp).preferencesManager
        if (!prefs.appKeepAlive) {
            (application as AiotApp).mqttManager.release()
        }
    }
}
