package compose.iot.data.room

import androidx.room.TypeConverter
import compose.iot.mqtt.CardStyle
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.ServerType

class Converters {
    @TypeConverter
    fun fromDeviceType(value: DeviceType): String = value.name

    @TypeConverter
    fun toDeviceType(value: String): DeviceType = enumValueOf(value)

    @TypeConverter
    fun fromServerType(value: ServerType): String = value.name

    @TypeConverter
    fun toServerType(value: String): ServerType = enumValueOf(value)

    @TypeConverter
    fun fromCardStyle(value: CardStyle): String = value.name

    @TypeConverter
    fun toCardStyle(value: String): CardStyle = enumValueOf(value)
}
