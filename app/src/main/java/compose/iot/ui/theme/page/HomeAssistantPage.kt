package compose.iot.ui.theme.page

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.Home
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.Lock
import compose.icons.tablericons.Refresh
import compose.icons.tablericons.Search
import compose.icons.tablericons.X
import compose.iot.R
import compose.iot.ui.components.AppScaffold
import compose.iot.ui.theme.function.standardEnterTransition
import compose.iot.ui.theme.function.standardExitTransition
import compose.iot.ui.viewmodel.HomeAssistantEvent
import compose.iot.ui.viewmodel.HomeAssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAssistantPage(
    navController: NavController,
    viewModel: HomeAssistantViewModel = hiltViewModel(),
) {
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isVisible by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeAssistantEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
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
        title = stringResource(R.string.home_assistant),
        navController = navController,
        actions = {
            if (uiState.showDeviceList) {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.refreshDevices()
                    },
                ) {
                    Icon(TablerIcons.Refresh, contentDescription = stringResource(R.string.refresh))
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                        text = stringResource(R.string.ha_server_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )

                    OutlinedTextField(
                        value = uiState.serverUrl,
                        onValueChange = viewModel::updateServerUrl,
                        label = { Text(stringResource(R.string.server_url)) },
                        placeholder = { Text(stringResource(R.string.home_assistant_server_url_placeholder)) },
                        leadingIcon = { Icon(TablerIcons.Home, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )

                    OutlinedTextField(
                        value = uiState.accessToken,
                        onValueChange = viewModel::updateAccessToken,
                        label = { Text(stringResource(R.string.access_token)) },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(TablerIcons.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )

                    OutlinedTextField(
                        value = uiState.pollingInterval,
                        onValueChange = viewModel::updatePollingInterval,
                        label = { Text(stringResource(R.string.polling_interval_seconds)) },
                        placeholder = { Text(stringResource(R.string.default_number_five)) },
                        leadingIcon = { Icon(TablerIcons.InfoCircle, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.small,
                    )

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
                            Icon(imageVector = TablerIcons.Check, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(stringResource(R.string.connect))
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
                        placeholder = { Text(stringResource(R.string.search_devices)) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(TablerIcons.Search, contentDescription = stringResource(R.string.search), modifier = Modifier.size(20.dp))
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
                                    Icon(TablerIcons.X, contentDescription = stringResource(R.string.clear), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                    )

                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { focusManager.clearFocus() }
                                },
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(items = filteredDevices, key = { device -> device.entityId }) { device ->
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
                                        text = stringResource(R.string.entity_id_line, device.entityId),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(text = stringResource(R.string.entity_state_line, device.state), style = MaterialTheme.typography.bodySmall)
                                    if (device.deviceClass != null) {
                                        Text(text = stringResource(R.string.entity_class_line, device.deviceClass), style = MaterialTheme.typography.bodySmall)
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
