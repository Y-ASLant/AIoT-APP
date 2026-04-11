package compose.iot.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import compose.iot.mqtt.DeviceType

/**
 * 统一 SharedPreferences 管理器
 * 集中管理分散在多个 Prefs 文件中的持久化访问
 */
class PreferencesManager(private val context: Context) {
    // region ── SharedPreferences 实例 ──

    private val mqttPrefs: SharedPreferences
        get() = context.getSharedPreferences(MQTT_SETTINGS, Context.MODE_PRIVATE)

    private val haPrefs: SharedPreferences
        get() = context.getSharedPreferences(HA_CONFIG, Context.MODE_PRIVATE)

    private val switchStatesPrefs: SharedPreferences
        get() = context.getSharedPreferences(SWITCH_STATES, Context.MODE_PRIVATE)

    private val sliderStatesPrefs: SharedPreferences
        get() = context.getSharedPreferences(SLIDER_STATES, Context.MODE_PRIVATE)

    private val appSettingsPrefs: SharedPreferences
        get() = context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE)

    // endregion

    // region ── MQTT 配置 ──

    val mqttAutoConnect: Boolean get() = mqttPrefs.getBoolean("auto_connect", false)
    val mqttServerIp: String get() = mqttPrefs.getString("server_ip", "mqtt.aslant.top") ?: "mqtt.aslant.top"
    val mqttServerPort: String get() = mqttPrefs.getString("server_port", "1883") ?: "1883"
    val mqttClientId: String get() = mqttPrefs.getString("client_id", "ComposeApplication") ?: "ComposeApplication"
    val mqttUsername: String? get() = mqttPrefs.getString("username", null)
    val mqttPassword: String? get() = mqttPrefs.getString("password", null)

    // endregion

    // region ── Home Assistant 配置 ──

    val haServerUrl: String get() = haPrefs.getString("server_url", "") ?: ""
    val haAccessToken: String get() = haPrefs.getString("access_token", "") ?: ""
    val haPollingInterval: Int get() = haPrefs.getInt("polling_interval", 3)

    // endregion

    // region ── 应用设置 ──

    var selectedDeviceType: DeviceType
        get() {
            val name =
                appSettingsPrefs.getString("selected_device_type", DeviceType.SENSOR.name)
                    ?: DeviceType.SENSOR.name
            return try {
                DeviceType.valueOf(name)
            } catch (_: Exception) {
                DeviceType.SENSOR
            }
        }
        set(value) {
            appSettingsPrefs.edit { putString("selected_device_type", value.name) }
        }

    var sliderContinuousUpdate: Boolean
        get() = appSettingsPrefs.getBoolean("slider_continuous_update", false)
        set(value) {
            appSettingsPrefs.edit { putBoolean("slider_continuous_update", value) }
        }

    var cornerShapeLevel: Int
        get() = appSettingsPrefs.getInt("corner_shape_level", 1)
        set(value) {
            appSettingsPrefs.edit { putInt("corner_shape_level", value) }
        }

    var appKeepAlive: Boolean
        get() = appSettingsPrefs.getBoolean("app_keep_alive", false)
        set(value) {
            appSettingsPrefs.edit { putBoolean("app_keep_alive", value) }
        }

    // endregion

    // region ── 开关/滑块状态 ──

    fun getSwitchState(
        cardId: String,
        default: Boolean = false,
    ): Boolean =
        switchStatesPrefs.getBoolean(cardId, default)

    fun saveSwitchState(
        cardId: String,
        isOn: Boolean,
    ) {
        switchStatesPrefs.edit { putBoolean(cardId, isOn) }
    }

    fun getSliderState(
        cardId: String,
        default: Float = 0f,
    ): Float =
        sliderStatesPrefs.getFloat(cardId, default)

    fun saveSliderState(
        cardId: String,
        value: Float,
    ) {
        sliderStatesPrefs.edit { putFloat(cardId, value) }
    }

    // endregion

    companion object {
        private const val MQTT_SETTINGS = "mqtt_settings"
        private const val HA_CONFIG = "homeassistant_config"
        private const val SWITCH_STATES = "switch_states"
        private const val SLIDER_STATES = "slider_states"
        private const val APP_SETTINGS = "app_settings"
    }
}
