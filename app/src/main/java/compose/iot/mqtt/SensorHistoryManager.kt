package compose.iot.mqtt

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * 传感器历史数据管理器
 */
class SensorHistoryManager(private val context: Context) {
    private val maxHistoryCount = 50 // 每个传感器最多存储50条历史记录
    
    /**
     * 添加传感器数据
     * @param cardId 卡片唯一标识
     * @param value 传感器值
     * @param unitSuffix 单位后缀
     */
    fun addData(cardId: String, value: String, unitSuffix: String) {
        try {
            val prefs = context.getSharedPreferences("sensor_history", Context.MODE_PRIVATE)
            val historyJson = prefs.getString(cardId, "[]") ?: "[]"
            val historyArray = JSONArray(historyJson)
            
            // 创建新的历史数据
            val newData = JSONObject().apply {
                put("value", value)
                put("timestamp", System.currentTimeMillis())
                put("unitSuffix", unitSuffix)
            }
            
            // 添加到数组开头
            val newArray = JSONArray()
            newArray.put(newData)
            
            // 保留最新的maxHistoryCount条记录
            val count = historyArray.length().coerceAtMost(maxHistoryCount - 1)
            for (i in 0 until count) {
                newArray.put(historyArray.getJSONObject(i))
            }
            
            // 保存到SharedPreferences
            prefs.edit().apply {
                putString(cardId, newArray.toString())
                apply()
            }
        } catch (e: Exception) {
            Log.e("SensorHistory", "添加历史数据失败", e)
        }
    }
    
    /**
     * 获取传感器历史数据
     * @param cardId 卡片唯一标识
     * @return 历史数据列表
     */
    fun getHistoryData(cardId: String): List<SensorHistoryData> {
        try {
            val prefs = context.getSharedPreferences("sensor_history", Context.MODE_PRIVATE)
            val historyJson = prefs.getString(cardId, "[]") ?: "[]"
            val historyArray = JSONArray(historyJson)
            
            val result = mutableListOf<SensorHistoryData>()
            for (i in 0 until historyArray.length()) {
                val item = historyArray.getJSONObject(i)
                result.add(
                    SensorHistoryData(
                        value = item.getString("value"),
                        timestamp = item.getLong("timestamp"),
                        unitSuffix = item.optString("unitSuffix", "")
                    )
                )
            }
            
            return result
        } catch (e: Exception) {
            Log.e("SensorHistory", "获取历史数据失败", e)
            return emptyList()
        }
    }
    
    /**
     * 清除指定传感器的历史数据
     * @param cardId 卡片唯一标识
     */
    fun clearHistory(cardId: String) {
        try {
            val prefs = context.getSharedPreferences("sensor_history", Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove(cardId)
                apply()
            }
        } catch (e: Exception) {
            Log.e("SensorHistory", "清除历史数据失败", e)
        }
    }
} 