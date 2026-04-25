package compose.iot.ui.theme.page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import compose.iot.R
import compose.iot.navigation.AppDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashPage(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.connection_dashboard), fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) },
                scrollBehavior = scrollBehavior,
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // MQTT 配置
            item {
                ServiceCard(
                    iconContent = {
                        Icon(
                            imageVector = TablerIcons.Server,
                            contentDescription = stringResource(R.string.mqtt_service),
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    title = stringResource(R.string.mqtt_service),
                    description = stringResource(R.string.mqtt_service_description),
                    onClick = { navController.navigate(AppDestination.Login) },
                )
            }

            // Home Assistant 配置
            item {
                ServiceCard(
                    iconContent = {
                        Icon(
                            imageVector = TablerIcons.Home2,
                            contentDescription = stringResource(R.string.home_assistant),
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    title = stringResource(R.string.home_assistant),
                    description = stringResource(R.string.home_assistant_description),
                    onClick = { navController.navigate(AppDestination.HomeAssistant) },
                )
            }

            // 视频流配置
            item {
                ServiceCard(
                    iconContent = {
                        Icon(
                            imageVector = TablerIcons.Video,
                            contentDescription = stringResource(R.string.video_stream_service),
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    title = stringResource(R.string.video_stream_service),
                    description = stringResource(R.string.video_stream_service_description),
                    onClick = { navController.navigate(AppDestination.VideoStream) },
                )
            }

            // 蓝牙配置
            item {
                ServiceCard(
                    iconContent = {
                        Icon(
                            imageVector = TablerIcons.Bluetooth,
                            contentDescription = stringResource(R.string.bluetooth_devices),
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    title = stringResource(R.string.bluetooth_devices),
                    description = stringResource(R.string.bluetooth_devices_description),
                    onClick = { navController.navigate(AppDestination.Bluetooth) },
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(
    iconContent: @Composable () -> Unit,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(description) },
            leadingContent = {
                FilledTonalIconButton(
                    onClick = onClick,
                    colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                ) {
                    iconContent()
                }
            },
            trailingContent = {
                Icon(
                    imageVector = TablerIcons.ChevronRight,
                    contentDescription = stringResource(R.string.enter),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
