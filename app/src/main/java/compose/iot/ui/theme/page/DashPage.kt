package compose.iot.ui.theme.page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import compose.iot.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashPage(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("连接面板") },
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
                    icon = R.drawable.fluentiot24regular,
                    title = "MQTT 服务",
                    description = "配置 EMQX Broker 连接参数",
                    onClick = { navController.navigate("login") },
                )
            }

            // Home Assistant 配置
            item {
                ServiceCard(
                    icon = R.drawable.homeassistant,
                    title = "Home Assistant",
                    description = "接入 HA 智能家居平台",
                    onClick = { navController.navigate("homeassistant") },
                )
            }

            // 视频流配置
            item {
                ServiceCard(
                    icon = R.drawable.videocam,
                    title = "视频流服务",
                    description = "WebSocket 实时画面监控",
                    onClick = { navController.navigate("video_stream") },
                )
            }

            // 蓝牙配置
            item {
                ServiceCard(
                    icon = R.drawable.bluetooth,
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
    icon: Int,
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
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = title,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "进入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
