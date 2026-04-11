package compose.iot.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import compose.iot.mqtt.SensorHistoryData

@Entity(tableName = "sensor_history")
data class SensorHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: String,
    val value: String,
    val timestamp: Long,
    val unitSuffix: String,
) {
    fun toData(): SensorHistoryData {
        return SensorHistoryData(
            value = value,
            timestamp = timestamp,
            unitSuffix = unitSuffix,
        )
    }
}
