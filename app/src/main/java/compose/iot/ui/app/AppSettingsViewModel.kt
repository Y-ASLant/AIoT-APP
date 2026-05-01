package compose.iot.ui.app

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import compose.iot.data.preferences.PreferencesManager
import compose.iot.data.room.SubscriptionCardDao
import compose.iot.mqtt.CardSize
import compose.iot.mqtt.CardStyle
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.MqttManager
import compose.iot.mqtt.ServerType
import compose.iot.mqtt.SubscriptionCard
import compose.iot.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val subscriptionCardDao: SubscriptionCardDao,
    private val mqttManager: MqttManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<AppSettingsState> = _uiState.asStateFlow()
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateCornerShapeLevel(level: Int) {
        prefs.cornerShapeLevel = level
        _uiState.update { it.copy(cornerShapeLevel = level) }
    }

    fun updateAppKeepAlive(enabled: Boolean) {
        prefs.appKeepAlive = enabled
        _uiState.update { it.copy(appKeepAlive = enabled) }
    }

    fun updateDarkMode(mode: Int) {
        prefs.darkMode = mode
        _uiState.update {
            it.copy(
                darkMode = mode,
                themeColor = if (mode == 0) 0 else it.themeColor,
            )
        }
        if (mode == 0) {
            prefs.themeColor = 0
        }
    }

    fun updateThemeColor(colorId: Int) {
        prefs.themeColor = colorId
        _uiState.update { it.copy(themeColor = colorId) }
    }

    fun updatePredictiveBack(enabled: Boolean) {
        prefs.predictiveBackEnabled = enabled
        _uiState.update { it.copy(predictiveBackEnabled = enabled) }
    }

    fun exportSubscriptionCards(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val cards = subscriptionCardDao.getAllCards()
                val payload =
                    JSONObject().apply {
                        put("version", 1)
                        put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                        put("mqtt", exportMqttConfig())
                        put("cards", JSONArray().apply {
                            cards.forEach { put(it.toJson()) }
                        })
                    }

                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(payload.toString(2))
                } ?: error("无法创建导出文件")

                emitMessage(context.getString(R.string.settings_export_success, cards.size))
            }.onFailure { error ->
                emitMessage(context.getString(R.string.settings_export_failed, error.message ?: context.getString(R.string.unknown_error)))
            }
        }
    }

    fun importSubscriptionCards(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val content =
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        reader.readText()
                    } ?: error("无法读取导入文件")

                val importedCards = parseImportedCards(content)
                parseImportedMqttConfig(content)?.let(::applyMqttConfig)
                check(importedCards.isNotEmpty()) { context.getString(R.string.settings_import_empty) }

                subscriptionCardDao.replaceAllCards(importedCards)
                emitMessage(context.getString(R.string.settings_import_success, importedCards.size))
            }.onFailure { error ->
                emitMessage(context.getString(R.string.settings_import_failed, error.message ?: context.getString(R.string.unknown_error)))
            }
        }
    }

    private fun loadState() =
        AppSettingsState(
            cornerShapeLevel = prefs.cornerShapeLevel,
            appKeepAlive = prefs.appKeepAlive,
            darkMode = prefs.darkMode,
            themeColor = prefs.themeColor,
            predictiveBackEnabled = prefs.predictiveBackEnabled,
        )

    private fun parseImportedCards(content: String): List<SubscriptionCard> {
        val root = JSONObject(content)
        val cards = root.optJSONArray("cards") ?: error("配置文件格式不正确")
        return buildList(cards.length()) {
            for (index in 0 until cards.length()) {
                add(cards.getJSONObject(index).toSubscriptionCard())
            }
        }
    }

    private fun parseImportedMqttConfig(content: String): ImportedMqttConfig? {
        val root = JSONObject(content)
        val mqtt = root.optJSONObject("mqtt") ?: return null
        return ImportedMqttConfig(
            autoConnect = mqtt.optBoolean("autoConnect", false),
            mqttVersion = mqtt.optInt("mqttVersion", 3),
            serverIp = mqtt.optString("serverIp", prefs.mqttServerIp),
            serverPort = mqtt.optString("serverPort", prefs.mqttServerPort),
            clientId = mqtt.optString("clientId", prefs.mqttClientId),
            username = mqtt.optString("username", prefs.mqttUsername.orEmpty()),
            password = mqtt.optString("password", prefs.mqttPassword.orEmpty()),
        )
    }

    private fun exportMqttConfig() =
        JSONObject().apply {
            put("autoConnect", prefs.mqttAutoConnect)
            put("mqttVersion", prefs.mqttVersion)
            put("serverIp", prefs.mqttServerIp)
            put("serverPort", prefs.mqttServerPort)
            put("clientId", prefs.mqttClientId)
            put("username", prefs.mqttUsername.orEmpty())
            put("password", prefs.mqttPassword.orEmpty())
        }

    private fun applyMqttConfig(config: ImportedMqttConfig) {
        prefs.mqttAutoConnect = config.autoConnect
        prefs.mqttVersion = config.mqttVersion
        prefs.mqttServerIp = config.serverIp
        prefs.mqttServerPort = config.serverPort
        prefs.mqttClientId = config.clientId
        prefs.mqttUsername = config.username
        prefs.mqttPassword = config.password

        mqttManager.setMqttVersion(config.mqttVersion)
        mqttManager.setServerUri("tcp://${config.serverIp}:${config.serverPort}")
        mqttManager.setClientId(config.clientId)
        mqttManager.setUsername(config.username)
        mqttManager.setPassword(config.password)
    }

    private fun SubscriptionCard.toJson() =
        JSONObject().apply {
            put("topic", topic)
            put("displayName", displayName)
            put("jsonParam", jsonParam)
            put("unitSuffix", unitSuffix)
            put("cardStyle", cardStyle.name)
            put("cardSize", cardSize.name)
            put("deviceType", deviceType.name)
            put("serverType", serverType.name)
            put("isButtonStyle", isButtonStyle)
            put("switchOnValue", switchOnValue)
            put("switchOffValue", switchOffValue)
            put("isSliderStyle", isSliderStyle)
            put("sliderMin", sliderMin)
            put("sliderMax", sliderMax)
            put("sliderStep", sliderStep)
            put("isPushButtonStyle", isPushButtonStyle)
            put("buttonValue", buttonValue)
        }

    private fun JSONObject.toSubscriptionCard(): SubscriptionCard =
        SubscriptionCard(
            topic = getString("topic"),
            displayName = getString("displayName"),
            jsonParam = getString("jsonParam"),
            unitSuffix = optString("unitSuffix", ""),
            cardStyle = optEnum("cardStyle", CardStyle.FILLED),
            cardSize = optEnum("cardSize", CardSize.S1x1),
            deviceType = optEnum("deviceType", DeviceType.SENSOR),
            serverType = optEnum("serverType", ServerType.EMQX),
            isButtonStyle = optBoolean("isButtonStyle", false),
            switchOnValue = optString("switchOnValue", "1"),
            switchOffValue = optString("switchOffValue", "0"),
            isSliderStyle = optBoolean("isSliderStyle", false),
            sliderMin = optDouble("sliderMin", 0.0).toFloat(),
            sliderMax = optDouble("sliderMax", 100.0).toFloat(),
            sliderStep = optDouble("sliderStep", 1.0).toFloat(),
            isPushButtonStyle = optBoolean("isPushButtonStyle", false),
            buttonValue = optString("buttonValue", "1"),
        )

    private inline fun <reified T : Enum<T>> JSONObject.optEnum(
        key: String,
        defaultValue: T,
    ): T = optString(key).takeIf { it.isNotBlank() }?.let {
        enumValues<T>().firstOrNull { enumValue -> enumValue.name == it }
    } ?: defaultValue

    private fun emitMessage(message: String) {
        _events.trySend(message)
    }

    private data class ImportedMqttConfig(
        val autoConnect: Boolean,
        val mqttVersion: Int,
        val serverIp: String,
        val serverPort: String,
        val clientId: String,
        val username: String,
        val password: String,
    )
}
