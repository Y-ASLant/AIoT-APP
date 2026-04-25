package compose.iot

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application 级单例容器
 *
 * 提供全局共享的 MqttManager 和 PreferencesManager 实例，
 * 替代手动在 Activity/Composable 中创建和传递依赖。
 */
@HiltAndroidApp
class AiotApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 仅在 Debug 构建时植入日志树
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
