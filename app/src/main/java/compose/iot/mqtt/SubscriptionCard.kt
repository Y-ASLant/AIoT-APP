package compose.iot.mqtt

enum class DeviceType {
    SENSOR,      // 传感器
    ACTUATOR     // 执行器
}

enum class ServerType {
    EMQX,        // EMQX 服务器
    HomeAssistant           // Home Assistant 服务器
}

data class SubscriptionCard(
    val topic: String,  // 对于EMQX是MQTT主题，对于Home Assistant是实体ID
    val displayName: String,
    val jsonParam: String,
    val unitSuffix: String = "",
    val cardStyle: CardStyle = CardStyle.FILLED,  // 卡片样式
    val deviceType: DeviceType = DeviceType.SENSOR,  // 设备类型
    val serverType: ServerType = ServerType.EMQX,  // 服务器类型
    val isButtonStyle: Boolean = false,  // 是否使用开关样式
    val switchOnValue: String = "1",  // 开关打开时的值
    val switchOffValue: String = "0",  // 开关关闭时的值
    val isSliderStyle: Boolean = false,  // 是否使用滑块样式
    val sliderMin: Float = 0f,  // 滑块最小值
    val sliderMax: Float = 100f,  // 滑块最大值
    val sliderStep: Float = 1f,  // 滑块步进值
    val isPushButtonStyle: Boolean = false,  // 是否为一次性按钮样式
    val buttonValue: String = "1"  // 按钮发送的值
)

enum class CardStyle {
    HIGHLIGHT,  // 高亮样式
    MINIMAL,    // 简约样式
    FILLED      // 填充样式（Material Design 3）
} 