package compose.iot.mqtt

import compose.iot.data.room.SensorHistoryEntity
import compose.iot.data.room.SensorHistoryDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 传感器历史数据管理器 (Backed by Room)
 */
class SensorHistoryManager @Inject constructor(
    private val dao: SensorHistoryDao,
) {
    private val maxHistoryCount = 50 // 每个传感器最多存储50条历史记录
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 添加传感器数据
     * @param cardId 卡片唯一标识
     * @param value 传感器值
     * @param unitSuffix 单位后缀
     */
    fun addData(
        cardId: String,
        value: String,
        unitSuffix: String,
    ) {
        scope.launch {
            try {
                dao.insertHistory(
                    SensorHistoryEntity(
                        cardId = cardId,
                        value = value,
                        timestamp = System.currentTimeMillis(),
                        unitSuffix = unitSuffix,
                    ),
                )
                // 保持最大记录数
                dao.trimHistory(cardId, maxHistoryCount)
            } catch (e: Exception) {
                Timber.e(e, "添加历史数据失败")
            }
        }
    }

    /**
     * 获取传感器历史数据
     * @param cardId 卡片唯一标识
     * @return 历史数据列表
     */
    suspend fun getHistoryData(cardId: String): List<SensorHistoryData> {
        return try {
            dao.getHistory(cardId, maxHistoryCount).map { it.toData() }
        } catch (e: Exception) {
            Timber.e(e, "获取历史数据失败")
            emptyList()
        }
    }

    /**
     * 清除指定传感器的历史数据
     * @param cardId 卡片唯一标识
     */
    fun clearHistory(cardId: String) {
        scope.launch {
            try {
                dao.clearHistory(cardId)
            } catch (e: Exception) {
                Timber.e(e, "清除历史数据失败")
            }
        }
    }

    /**
     * 释放相关资源，避免作用域泄漏
     */
    fun dispose() {
        scope.cancel()
    }
}
