package compose.iot.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorHistoryDao {
    @Query("SELECT * FROM sensor_history WHERE cardId = :cardId ORDER BY timestamp DESC LIMIT :limit")
    fun getHistoryStream(
        cardId: String,
        limit: Int = 50,
    ): Flow<List<SensorHistoryEntity>>

    @Query("SELECT * FROM sensor_history WHERE cardId = :cardId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getHistory(
        cardId: String,
        limit: Int = 50,
    ): List<SensorHistoryEntity>

    @Insert
    suspend fun insertHistory(history: SensorHistoryEntity)

    @Query("DELETE FROM sensor_history WHERE cardId = :cardId")
    suspend fun clearHistory(cardId: String)

    // Limits the history per card to say 100 max to prevent unbounded growth
    @Query("DELETE FROM sensor_history WHERE cardId = :cardId AND id NOT IN (SELECT id FROM sensor_history WHERE cardId = :cardId ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun trimHistory(
        cardId: String,
        keepCount: Int = 50,
    )
}
