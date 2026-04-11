package compose.iot

import android.app.Application
import timber.log.Timber

class AiotApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 仅在 Debug 构建中植入日志树，Release 构建不会输出任何 Timber 日志
        if (BuildConfig.DEBUG) {
            Timber.plant(
                object : Timber.DebugTree() {
                    override fun createStackElementTag(element: StackTraceElement): String {
                        // 使用 "类名:行号" 格式，方便在 Logcat 中快速定位
                        return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
                    }
                },
            )
        }
    }
}
