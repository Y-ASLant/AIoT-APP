package compose.iot.ui.theme.page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashPage(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("连接面板", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) },
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
                            contentDescription = "MQTT 服务",
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    title = "MQTT 服务",
                    description = "配置 EMQX Broker 连接参数",
                    onClick = { navController.navigate("login") },
                )
            }

            // Home Assistant 配置
            item {
                ServiceCard(
                    iconContent = {
                        Icon(
                            imageVector = TablerIcons.Home2,
                            contentDescription = "Home Assistant",
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    title = "Home Assistant",
                    description = "接入 HA 智能家居平台",
                    onClick = { navController.navigate("homeassistant") },
                )
            }

            // 视频流配置
            item {
                ServiceCard(
                    iconContent = {
                        Icon(
                            imageVector = TablerIcons.Video,
                            contentDescription = "视频流服务",
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    title = "视频流服务",
                    description = "WebSocket 实时画面监控",
                    onClick = { navController.navigate("video_stream") },
                )
            }

            // 蓝牙配置
            item {
                ServiceCard(
                    iconContent = {
                        Icon(
                            imageVector = TablerIcons.Bluetooth,
                            contentDescription = "蓝牙设备",
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    title = "蓝牙设备",
                    description = "扫描并连接 BLE 外设",
                    onClick = { navController.navigate("bluetooth") },
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
                    contentDescription = "进入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
