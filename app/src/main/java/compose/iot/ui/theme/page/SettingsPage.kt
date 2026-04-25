package compose.iot.ui.theme.page

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import compose.iot.mqtt.MqttForegroundService
import compose.iot.navigation.AppDestination
import compose.iot.R
import compose.iot.ui.app.LocalAppSettingsViewModel
import compose.iot.ui.components.AppScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(navController: NavController) {
    val context = LocalContext.current
    val settingsViewModel = LocalAppSettingsViewModel.current
    val appSettings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                MqttForegroundService.start(context)
            }
        }

    AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        title = stringResource(R.string.settings_title),
        navController = navController,
        showBackButton = false,
        scrollBehavior = scrollBehavior,
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_section_ui),
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
                            headlineContent = { Text(stringResource(R.string.app_theme)) },
                            supportingContent = { Text(stringResource(R.string.app_theme_description)) },
                            modifier = Modifier.clickable { navController.navigate(AppDestination.AppTheme) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.system_corner_style)) },
                            supportingContent = { Text(stringResource(R.string.system_corner_style_description)) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )

                        Slider(
                            value = appSettings.cornerShapeLevel.toFloat(),
                            onValueChange = { settingsViewModel.updateCornerShapeLevel(it.toInt()) },
                            valueRange = 0f..2f,
                            steps = 1,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(stringResource(R.string.corner_small), style = MaterialTheme.typography.labelMedium)
                            Text(stringResource(R.string.corner_default), style = MaterialTheme.typography.labelMedium)
                            Text(stringResource(R.string.corner_large), style = MaterialTheme.typography.labelMedium)
                        }

                        if (Build.VERSION.SDK_INT >= 34) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            )

                            ListItem(
                                headlineContent = { Text(stringResource(R.string.predictive_back_animation)) },
                                supportingContent = { Text(stringResource(R.string.predictive_back_animation_description)) },
                                trailingContent = {
                                    Switch(
                                        checked = appSettings.predictiveBackEnabled,
                                        onCheckedChange = settingsViewModel::updatePredictiveBack,
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
                    text = stringResource(R.string.settings_section_background),
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
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.notification_keep_alive)) },
                        supportingContent = { Text(stringResource(R.string.notification_keep_alive_description)) },
                        trailingContent = {
                            Switch(
                                checked = appSettings.appKeepAlive,
                                onCheckedChange = { isChecked ->
                                    settingsViewModel.updateAppKeepAlive(isChecked)
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
                                                MqttForegroundService.start(context)
                                            }
                                        } else {
                                            MqttForegroundService.start(context)
                                        }
                                    } else {
                                        MqttForegroundService.stop(context)
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
