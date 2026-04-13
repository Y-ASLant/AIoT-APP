package compose.iot.ui.theme.page

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

private fun hasBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun BluetoothPage(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bluetoothManager =
        remember {
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        }
    val bluetoothAdapter = remember { bluetoothManager?.adapter }

    var isScanning by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(hasBluetoothPermissions(context)) }
    val discoveredDevices = remember { mutableStateListOf<BluetoothDevice>() }

    // 权限申请
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            hasPermission = permissions.values.all { it }
            if (!hasPermission) {
                Toast.makeText(context, "需要蓝牙权限才能扫描设备", Toast.LENGTH_SHORT).show()
            }
        }

    // 进入页面自动申请权限
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(requiredPermissions())
        }
    }

    // 扫描回调
    val scanCallback =
        remember {
            object : ScanCallback() {
                override fun onScanResult(
                    callbackType: Int,
                    result: ScanResult,
                ) {
                    val device = result.device
                    if (device !in discoveredDevices) {
                        discoveredDevices.add(device)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Timber.e("BLE scan failed: $errorCode")
                    isScanning = false
                }
            }
        }

    // 自动停止扫描（10 秒）
    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(10_000L)
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (e: Exception) {
                Timber.e(e, "Stop scan error")
            }
            isScanning = false
        }
    }

    // 页面退出时停止扫描
    DisposableEffect(Unit) {
        onDispose {
            if (isScanning) {
                try {
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                } catch (e: Exception) {
                    Timber.e(e, "Dispose stop scan error")
                }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    compose.iot.ui.components.AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        title = "蓝牙设备",
        navController = navController,
        scrollBehavior = scrollBehavior,
    ) { _ ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
        ) {
            // 蓝牙状态卡片
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.large,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = TablerIcons.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "蓝牙状态",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Badge(
                            containerColor =
                                if (bluetoothAdapter?.isEnabled == true) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                            contentColor =
                                if (bluetoothAdapter?.isEnabled == true) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                        ) {
                            Text(
                                text =
                                    if (bluetoothAdapter?.isEnabled == true) "已开启" else "未开启",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    if (!hasPermission) {
                        Text(
                            text = "缺少蓝牙权限，点击下方按钮重新申请",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            if (!hasPermission) {
                                permissionLauncher.launch(requiredPermissions())
                                return@FilledTonalButton
                            }
                            if (bluetoothAdapter?.isEnabled != true) {
                                Toast.makeText(context, "请先在系统设置中开启蓝牙", Toast.LENGTH_SHORT).show()
                                return@FilledTonalButton
                            }
                            if (isScanning) {
                                bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
                                isScanning = false
                            } else {
                                // 检查定位服务是否开启 (Android 11及以下必须开启GPS才能扫描BLE)
                                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                                val isLocationEnabled =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        locationManager?.isLocationEnabled == true
                                    } else {
                                        locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                                            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
                                    }

                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isLocationEnabled) {
                                    Toast.makeText(context, "请开启系统定位服务(GPS)以发现蓝牙设备", Toast.LENGTH_SHORT).show()
                                    return@FilledTonalButton
                                }

                                discoveredDevices.clear()
                                val settings =
                                    android.bluetooth.le.ScanSettings.Builder()
                                        .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                                        .build()
                                bluetoothAdapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
                                isScanning = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在扫描…")
                        } else if (!hasPermission) {
                            Icon(
                                imageVector = TablerIcons.Bluetooth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("授予蓝牙权限")
                        } else {
                            Icon(
                                imageVector = TablerIcons.Bluetooth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("扫描 BLE 设备")
                        }
                    }
                }
            }

            // 设备列表
            if (discoveredDevices.isEmpty() && !isScanning) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = TablerIcons.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(
                            text = "点击扫描发现附近蓝牙设备",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = discoveredDevices.toList(),
                        key = { it.address },
                    ) { device ->
                        DeviceItem(device = device)
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceItem(device: BluetoothDevice) {
    val deviceName =
        try {
            device.name
        } catch (_: SecurityException) {
            null
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        ListItem(
            headlineContent = { Text(deviceName ?: "未知设备", style = MaterialTheme.typography.bodyLarge) },
            supportingContent = { Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingContent = {
                FilledTonalIconButton(
                    onClick = { },
                    colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                ) {
                    Icon(
                        imageVector = TablerIcons.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
