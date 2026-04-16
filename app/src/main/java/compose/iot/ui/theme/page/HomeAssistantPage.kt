package compose.iot.ui.theme.page

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import compose.iot.ui.components.AppScaffold
import compose.iot.ui.theme.function.standardEnterTransition
import compose.iot.ui.theme.function.standardExitTransition
import compose.iot.ui.viewmodel.HomeAssistantEvent
import compose.iot.ui.viewmodel.HomeAssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAssistantPage(
    navController: NavController,
    viewModel: HomeAssistantViewModel = viewModel(),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeAssistantEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(uiState.showDeviceList) {
        if (uiState.showDeviceList) {
            focusManager.clearFocus()
        }
    }

    val filteredDevices =
        remember(uiState.devices, uiState.searchQuery) {
            if (uiState.searchQuery.isBlank()) {
                uiState.devices
            } else {
                uiState.devices.filter {
                    it.friendlyName.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.entityId.contains(uiState.searchQuery, ignoreCase = true)
                }
            }
        }

    AppScaffold(
        title = "Home Assistant 配置",
        navController = navController,
        actions = {
            if (uiState.showDeviceList) {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.refreshDevices()
                    },
                ) {
                    Icon(TablerIcons.Refresh, contentDescription = "刷新设备列表")
                }
            }
        },
    ) { _ ->
        AnimatedVisibility(
            visible = isVisible,
            enter = standardEnterTransition(initialOffsetY = -50),
            exit = standardExitTransition(targetOffsetY = -50),
        ) {
            if (!uiState.showDeviceList) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "网关详情",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )

                    OutlinedTextField(
                        value = uiState.serverUrl,
                        onValueChange = viewModel::updateServerUrl,
                        label = { Text("服务器地址") },
                        placeholder = { Text("例如: http://homeassistant.local:8123") },
                        leadingIcon = { Icon(TablerIcons.Home, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )

                    OutlinedTextField(
                        value = uiState.accessToken,
                        onValueChange = viewModel::updateAccessToken,
                        label = { Text("长期访问令牌 (Token)") },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(TablerIcons.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )

                    OutlinedTextField(
                        value = uiState.pollingInterval,
                        onValueChange = viewModel::updatePollingInterval,
                        label = { Text("轮询间隔（秒）") },
                        placeholder = { Text("默认为5秒") },
                        leadingIcon = { Icon(TablerIcons.InfoCircle, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.small,
                    )

                    Text(
                        text = "轮询间隔决定了设备状态更新的频率，建议设置在3-30秒之间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = viewModel::connectAndLoadDevices,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        enabled = !uiState.isLoading,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = TablerIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                "连接并获取设备",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .height(48.dp),
                        placeholder = {
                            Text(
                                "搜索设备...",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                TablerIcons.Search,
                                contentDescription = "搜索",
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.updateSearchQuery("")
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        TablerIcons.X,
                                        contentDescription = "清除",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                    )

                    if (uiState.devices.isNotEmpty()) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text =
                                    "共 ${uiState.devices.size} 个设备" +
                                        if (uiState.searchQuery.isNotEmpty()) ", 已过滤 ${filteredDevices.size} 个" else "",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        focusManager.clearFocus()
                                    }
                                },
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(
                            items = filteredDevices,
                            key = { device -> device.entityId },
                        ) { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.addDevice(device)
                                },
                            ) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    Text(
                                        text = device.friendlyName,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "ID: ${device.entityId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "状态: ${device.state}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (device.deviceClass != null) {
                                        Text(
                                            text = "类型: ${device.deviceClass}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
