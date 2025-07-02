package compose.iot.mqtt

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 传感器历史数据模型
 * @param value 传感器值
 * @param timestamp 时间戳
 * @param unitSuffix 单位后缀
 */
data class SensorHistoryData(
    val value: String,
    val timestamp: Long = System.currentTimeMillis(),
    val unitSuffix: String = ""
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun getFormattedValue(): String {
        return "$value$unitSuffix"
    }
    
    companion object {
        @SuppressLint("DefaultLocale")
        fun getStatistics(data: List<SensorHistoryData>): Triple<String, String, String> {
            if (data.isEmpty()) {
                return Triple("0.00", "0.00", "0.00")
            }
            
            val values = data.map { it.value.toDoubleOrNull() ?: 0.0 }
            val avg = values.average()
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 0.0
            
            return Triple(
                String.format("%.2f", avg),
                String.format("%.2f", min),
                String.format("%.2f", max)
            )
        }
    }
}