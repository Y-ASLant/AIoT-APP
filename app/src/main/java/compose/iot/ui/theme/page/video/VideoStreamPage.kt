package compose.iot.ui.theme.page.video

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.Home
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.Lock
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Refresh
import compose.icons.tablericons.User
import compose.icons.tablericons.X
import compose.iot.R
import compose.iot.ui.components.AppScaffold
import compose.iot.ui.viewmodel.VideoStreamEffect
import compose.iot.ui.viewmodel.VideoStreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoStreamPage(
    navController: NavController,
    viewModel: VideoStreamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is VideoStreamEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = stringResource(R.string.video_stream_title),
        navController = navController,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { _ ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isStreamingActive) {
                VideoStreamView(
                    bitmap = uiState.frameBitmap,
                    messageCount = uiState.messageCount,
                    lastMessage = uiState.lastMessage,
                    errorMessage = uiState.errorMessage,
                    onClose = viewModel::stopStreaming,
                )
            } else {
                val config = uiState.config

                Text(
                    text = stringResource(R.string.network),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = TablerIcons.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.websocket_security), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (config.isSecure) stringResource(R.string.use_wss) else stringResource(R.string.use_ws),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = config.isSecure,
                            onCheckedChange = { checked -> viewModel.updateConfig { it.copy(isSecure = checked) } },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = config.serverUrl,
                        onValueChange = { value -> viewModel.updateConfig { it.copy(serverUrl = value) } },
                        label = { Text(stringResource(R.string.server)) },
                        leadingIcon = { Icon(TablerIcons.Home, contentDescription = null) },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                    OutlinedTextField(
                        value = config.port,
                        onValueChange = { value -> viewModel.updateConfig { it.copy(port = value) } },
                        label = { Text(stringResource(R.string.port)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.3f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                }

                OutlinedTextField(
                    value = config.path,
                    onValueChange = { value -> viewModel.updateConfig { it.copy(path = value) } },
                    label = { Text(stringResource(R.string.path)) },
                    leadingIcon = { Icon(TablerIcons.InfoCircle, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )

                Text(
                    text = stringResource(R.string.credentials),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = config.username,
                        onValueChange = { value -> viewModel.updateConfig { it.copy(username = value) } },
                        label = { Text(stringResource(R.string.username)) },
                        leadingIcon = { Icon(TablerIcons.User, contentDescription = null) },
                        modifier = Modifier.weight(0.5f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                    OutlinedTextField(
                        value = config.password,
                        onValueChange = { value -> viewModel.updateConfig { it.copy(password = value) } },
                        label = { Text(stringResource(R.string.password)) },
                        leadingIcon = { Icon(TablerIcons.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.weight(0.5f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                    ) {
                        Text(stringResource(R.string.websocket_url), style = MaterialTheme.typography.titleSmall)
                        Text(text = config.fullWebSocketUrl, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                if (uiState.isTestingConnection) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (uiState.isUrlTested) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (uiState.isConnectionSuccessful) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    },
                            ),
                    ) {
                        Text(
                            text =
                                if (uiState.isConnectionSuccessful) {
                                    stringResource(R.string.connection_successful)
                                } else {
                                    uiState.connectionErrorMessage.ifBlank { stringResource(R.string.connection_failed) }
                                },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::testConnection,
                        modifier = Modifier.weight(1f).height(56.dp),
                        enabled = !uiState.isTestingConnection,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(imageVector = TablerIcons.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.test))
                    }

                    if (uiState.isConnectionSuccessful) {
                        Button(
                            onClick = viewModel::startStreaming,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Icon(imageVector = TablerIcons.PlayerPlay, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.start))
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.saveConfig()
                                navController.navigateUp()
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.buttonColors(),
                        ) {
                            Icon(imageVector = TablerIcons.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoStreamView(
    bitmap: android.graphics.Bitmap?,
    messageCount: Int,
    lastMessage: String,
    errorMessage: String,
    onClose: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.video_stream_content_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(R.string.frames_count, messageCount),
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), shape = MaterialTheme.shapes.small)
                        .padding(8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.waiting_for_frames))
                if (messageCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.messages_count, messageCount))
                }
                if (lastMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.last_message, lastMessage))
                }
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.error_message, errorMessage), color = MaterialTheme.colorScheme.error)
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), shape = CircleShape),
        ) {
            Icon(imageVector = TablerIcons.X, contentDescription = stringResource(R.string.close_stream), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
