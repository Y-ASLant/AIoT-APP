package compose.iot.ui.theme.page

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import compose.iot.AppState
import compose.iot.data.preferences.PreferencesManager

@Composable
fun SettingsPage(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                compose.iot.mqtt.MqttForegroundService.start(context)
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "系统设置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "系统圆角风格",
                    style = MaterialTheme.typography.titleMedium,
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    val currentLevel = AppState.cornerShapeLevel.intValue

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("小圆角", style = MaterialTheme.typography.labelMedium)
                        Text("默认", style = MaterialTheme.typography.labelMedium)
                        Text("大圆角", style = MaterialTheme.typography.labelMedium)
                    }

                    Slider(
                        value = currentLevel.toFloat(),
                        onValueChange = { newValue ->
                            val level = newValue.toInt()
                            AppState.cornerShapeLevel.intValue = level
                            prefs.cornerShapeLevel = level
                        },
                        valueRange = 0f..2f,
                        steps = 1,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("通知栏保活", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "开启通知栏常驻，保证软件在后台连接稳定。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val keepAlive = AppState.appKeepAlive.value
                    Switch(
                        checked = keepAlive,
                        onCheckedChange = { isChecked ->
                            AppState.appKeepAlive.value = isChecked
                            prefs.appKeepAlive = isChecked
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val isGranted =
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.POST_NOTIFICATIONS,
                                        ) == PackageManager.PERMISSION_GRANTED

                                    if (!isGranted) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                compose.iot.mqtt.MqttForegroundService.start(context)
                            } else {
                                compose.iot.mqtt.MqttForegroundService.stop(context)
                            }
                        },
                    )
                }
            }
        }
    }
}
