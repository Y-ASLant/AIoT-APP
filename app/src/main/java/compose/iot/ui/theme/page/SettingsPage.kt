package compose.iot.ui.theme.page

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import compose.iot.AppState
import compose.iot.data.preferences.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                compose.iot.mqtt.MqttForegroundService.start(context)
            }
        }

    compose.iot.ui.components.AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        title = "首选项",
        navController = navController,
        showBackButton = false, // It's in the bottom nav usually, but wait, if it's navigated from somewhere it might need it. Let's set it to false for now based on current behavior where SettingsPage didn't have a back button.
        scrollBehavior = scrollBehavior
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
        ) {
            item {
                Text(
                    text = "界面",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp),
                )
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text("应用主题") },
                            supportingContent = { Text("动态取色与深色模式配置") },
                            modifier = Modifier.clickable { navController.navigate("app_theme") },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )

                        ListItem(
                            headlineContent = { Text("系统圆角风格") },
                            supportingContent = { Text("调整全局组件的圆角度数") },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )

                        val currentLevel = AppState.cornerShapeLevel.intValue

                        Slider(
                            value = currentLevel.toFloat(),
                            onValueChange = { newValue ->
                                val level = newValue.toInt()
                                AppState.cornerShapeLevel.intValue = level
                                prefs.cornerShapeLevel = level
                            },
                            valueRange = 0f..2f,
                            steps = 1,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("小圆角", style = MaterialTheme.typography.labelMedium)
                            Text("默认", style = MaterialTheme.typography.labelMedium)
                            Text("大圆角", style = MaterialTheme.typography.labelMedium)
                        }

                        if (android.os.Build.VERSION.SDK_INT >= 34) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            )

                            val predictiveBack = AppState.predictiveBackEnabled.value
                            ListItem(
                                headlineContent = { Text("预测性返回动画") },
                                supportingContent = { Text("在 Android 14+ 开启高级侧滑返回动效") },
                                trailingContent = {
                                    Switch(
                                        checked = predictiveBack,
                                        onCheckedChange = { isChecked ->
                                            AppState.predictiveBackEnabled.value = isChecked
                                            prefs.predictiveBackEnabled = isChecked
                                        },
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "后台服务",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp),
                )
            }

            item {
                val keepAlive = AppState.appKeepAlive.value
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    ListItem(
                        headlineContent = { Text("通知栏保活") },
                        supportingContent = { Text("开启通知栏常驻，保证软件在后台连接稳定") },
                        trailingContent = {
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
                                            } else {
                                                compose.iot.mqtt.MqttForegroundService.start(context)
                                            }
                                        } else {
                                            compose.iot.mqtt.MqttForegroundService.start(context)
                                        }
                                    } else {
                                        compose.iot.mqtt.MqttForegroundService.stop(context)
                                    }
                                },
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
            }
        }
    }
}
