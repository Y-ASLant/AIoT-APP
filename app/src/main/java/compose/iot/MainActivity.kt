package compose.iot

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import compose.iot.data.preferences.PreferencesManager
import compose.iot.mqtt.MqttForegroundService
import compose.iot.mqtt.MqttManager
import compose.iot.navigation.AppDestination
import compose.iot.ui.app.AppSettingsViewModel
import compose.iot.ui.app.LocalAppSettingsViewModel
import compose.iot.ui.theme.function.AppThemePage
import compose.iot.ui.theme.function.MainScaffold
import compose.iot.ui.theme.page.BluetoothPage
import compose.iot.ui.theme.page.ChangelogPage
import compose.iot.ui.theme.page.HomeAssistantPage
import compose.iot.ui.theme.page.LoginPage
import compose.iot.ui.theme.page.video.VideoStreamPage
import compose.iot.ui.theme.ui.theme.AIOT_ComposeTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var mqttManager: MqttManager

    @Inject
    lateinit var prefs: PreferencesManager

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (prefs.appKeepAlive) {
            MqttForegroundService.start(this)
        }

        if (prefs.mqttAutoConnect) {
            mqttManager.connect(
                onConnectComplete = {
                    Timber.i("MQTT 自动连接成功")
                },
                onError = { error ->
                    Timber.w("MQTT 自动连接失败: %s", error)
                },
            )
        }

        enableEdgeToEdge()
        setContent {
            val appSettingsViewModel: AppSettingsViewModel = hiltViewModel()
            val appSettings by appSettingsViewModel.uiState.collectAsStateWithLifecycle()
            val navController = rememberNavController()

            CompositionLocalProvider(LocalAppSettingsViewModel provides appSettingsViewModel) {
                AIOT_ComposeTheme(appSettings = appSettings) {
                    if (!appSettings.predictiveBackEnabled) {
                        androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
                            navController.navigateUp()
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Root,
                    ) {
                        composable<AppDestination.Root> {
                            MainScaffold(navController = navController)
                        }
                        composable<AppDestination.Login> {
                            LoginPage(navController, mqttManager, prefs)
                        }
                        composable<AppDestination.HomeAssistant> {
                            HomeAssistantPage(navController)
                        }
                        composable<AppDestination.Changelog> {
                            ChangelogPage()
                        }
                        composable<AppDestination.VideoStream> {
                            VideoStreamPage(navController)
                        }
                        composable<AppDestination.Bluetooth> {
                            BluetoothPage(navController)
                        }
                        composable<AppDestination.AppTheme> {
                            AppThemePage(navController)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!prefs.appKeepAlive) {
            mqttManager.release()
        }
    }
}
