package compose.iot.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import compose.iot.mqtt.DeviceType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 统一 SharedPreferences 管理器
 * 集中管理分散在多个 Prefs 文件中的持久化访问
 */
class PreferencesManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
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

    var mqttAutoConnect: Boolean
        get() = mqttPrefs.getBoolean("auto_connect", false)
        set(value) = mqttPrefs.edit { putBoolean("auto_connect", value) }

    var mqttVersion: Int
        get() = mqttPrefs.getInt("mqtt_version", 3)
        set(value) = mqttPrefs.edit { putInt("mqtt_version", value) }

    var mqttServerIp: String
        get() = mqttPrefs.getString("server_ip", "broker.emqx.io") ?: "broker.emqx.io"
        set(value) = mqttPrefs.edit { putString("server_ip", value) }

    var mqttServerPort: String
        get() = mqttPrefs.getString("server_port", "1883") ?: "1883"
        set(value) = mqttPrefs.edit { putString("server_port", value) }

    var mqttClientId: String
        get() = mqttPrefs.getString("client_id", "ComposeApplication") ?: "ComposeApplication"
        set(value) = mqttPrefs.edit { putString("client_id", value) }

    var mqttUsername: String?
        get() = mqttPrefs.getString("username", "ASLant")
        set(value) = mqttPrefs.edit { putString("username", value) }

    var mqttPassword: String?
        get() = mqttPrefs.getString("password", "")
        set(value) = mqttPrefs.edit { putString("password", value) }

    // endregion

    // region ── Home Assistant 配置 ──

    var haServerUrl: String
        get() = haPrefs.getString("server_url", "") ?: ""
        set(value) = haPrefs.edit { putString("server_url", value) }

    var haAccessToken: String
        get() = haPrefs.getString("access_token", "") ?: ""
        set(value) = haPrefs.edit { putString("access_token", value) }

    var haPollingInterval: Int
        get() = haPrefs.getInt("polling_interval", 3)
        set(value) = haPrefs.edit { putInt("polling_interval", value) }

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

    var predictiveBackEnabled: Boolean
        get() = appSettingsPrefs.getBoolean("predictive_back", true)
        set(value) {
            appSettingsPrefs.edit { putBoolean("predictive_back", value) }
        }

    var darkMode: Int
        get() = appSettingsPrefs.getInt("dark_mode", 0)
        set(value) {
            appSettingsPrefs.edit { putInt("dark_mode", value) }
        }

    var themeColor: Int
        get() = appSettingsPrefs.getInt("theme_color", 0)
        set(value) {
            appSettingsPrefs.edit { putInt("theme_color", value) }
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
