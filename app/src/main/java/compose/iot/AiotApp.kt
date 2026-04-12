package compose.iot

import android.app.Application
import compose.iot.data.preferences.PreferencesManager
import compose.iot.mqtt.MqttManager
import timber.log.Timber

/**
 * Application 级单例容器
 *
 * 提供全局共享的 MqttManager 和 PreferencesManager 实例，
 * 替代手动在 Activity/Composable 中创建和传递依赖。
 */
class AiotApp : Application() {
    lateinit var mqttManager: MqttManager
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var appDatabase: compose.iot.data.room.AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        // 仅在 Debug 构建时植入日志树
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // 初始化全局单例
        // 初始化全局单例
        mqttManager = MqttManager()
        preferencesManager = PreferencesManager(this)
        appDatabase = compose.iot.data.room.AppDatabase.getDatabase(this)

        // 检查是否需要自动连接 MQTT
        if (preferencesManager.mqttAutoConnect) {
            val serverUri = "tcp://${preferencesManager.mqttServerIp}:${preferencesManager.mqttServerPort}"
            mqttManager.setMqttVersion(preferencesManager.mqttVersion)
            mqttManager.setServerUri(serverUri)
            mqttManager.setClientId(preferencesManager.mqttClientId)
            mqttManager.setUsername(preferencesManager.mqttUsername)
            mqttManager.setPassword(preferencesManager.mqttPassword)
        }
    }
}
