package compose.iot.mqtt

import androidx.compose.runtime.Immutable
import androidx.room.Entity

enum class DeviceType {
    SENSOR, // 传感器
    ACTUATOR, // 执行器
}

enum class ServerType {
    EMQX, // EMQX 服务器
    HomeAssistant, // Home Assistant 服务器
}

enum class CardSize {
    S1x1, // 单列单行
    S1x2, // 单列双行
    S2x1, // 双列单行
    S2x2, // 双列双行
}

@Immutable
@Entity(tableName = "subscription_cards", primaryKeys = ["topic", "jsonParam"])
data class SubscriptionCard(
    // 对于EMQX是MQTT主题，对于Home Assistant是实体ID
    val topic: String,
    val displayName: String,
    val jsonParam: String,
    val unitSuffix: String = "",
    // 卡片样式
    val cardStyle: CardStyle = CardStyle.FILLED,
    // 卡片尺寸
    val cardSize: CardSize = CardSize.S1x1,
    // 设备类型
    val deviceType: DeviceType = DeviceType.SENSOR,
    // 服务器类型
    val serverType: ServerType = ServerType.EMQX,
    // 是否使用开关样式
    val isButtonStyle: Boolean = false,
    // 开关打开时的值
    val switchOnValue: String = "1",
    // 开关关闭时的值
    val switchOffValue: String = "0",
    // 是否使用滑块样式
    val isSliderStyle: Boolean = false,
    // 滑块最小值
    val sliderMin: Float = 0f,
    // 滑块最大值
    val sliderMax: Float = 100f,
    // 滑块步进值
    val sliderStep: Float = 1f,
    // 是否为一次性按钮样式
    val isPushButtonStyle: Boolean = false,
    // 按钮发送的值
    val buttonValue: String = "1",
)

enum class CardStyle {
    HIGHLIGHT, // 高亮样式
    MINIMAL, // 简约样式
    FILLED, // 填充样式（Material Design 3）
}

// region ── 扩展属性与工具方法 ──

/** 生成卡片的唯一标识 */
val SubscriptionCard.cardId: String
    get() = "$topic:$jsonParam"

/** 从 HA 主题提取实体 ID */
fun SubscriptionCard.extractEntityId(): String =
    topic.removePrefix("homeassistant/").removeSuffix("/state")

/** 根据 cardValues 中的原始值判断开关是否开启 */
fun SubscriptionCard.isSwitchOn(value: String?): Boolean {
    if (value == null) return false
    return when (serverType) {
        ServerType.HomeAssistant -> value == "on"
        ServerType.EMQX -> value == "on" || value == switchOnValue
    }
}

// endregion
