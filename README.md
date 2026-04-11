# AIoT-APP

<div align="center">

![AIoT-APP Logo](demo.png)

</div>

## 项目简介

AIoT-APP 是一个使用 Kotlin 和 Jetpack Compose 开发的原生 Android 智能家居控制应用。

项目目标是提供一个面向个人或定制化场景的 AIoT 控制面板，统一接入 MQTT 设备、Home Assistant 实体，以及基于 WebSocket 的视频流页面。

## 当前能力

- MQTT 设备连接、订阅和消息下发
- Home Assistant REST 轮询集成
- 传感器卡片和执行器卡片的自定义配置
- 设备状态本地持久化
- 传感器历史数据查看
- WebSocket 视频流测试与显示页面
- 基于 Material 3 的 Compose 原生界面

## 技术栈

- Kotlin
- Jetpack Compose
- Android Material 3
- Android Navigation Compose
- Eclipse Paho MQTT
- OkHttp / Java-WebSocket
- SharedPreferences 本地存储

## 运行要求

- Android 8.0 及以上
- `minSdk 26`
- `targetSdk 35`
- Java 21
- Android Studio 新版稳定版

## 构建方式

1. 使用 Android Studio 打开项目根目录。
2. 等待 Gradle 同步完成。
3. 运行 `app` 模块到模拟器或真机。

命令行编译：

```powershell
./gradlew :app:assembleDebug
```

仅验证 Kotlin 编译：

```powershell
./gradlew :app:compileDebugKotlin
```

Release APK 打包：

```powershell
./gradlew :app:assembleRelease
```

Android Studio 打 APK：

1. 打开 `Build`
2. 选择 `Generate Signed App Bundle or APK...`
3. 选择 `APK`，不要选 `Android App Bundle`
4. 选择 `release`
5. 在 Android Studio 向导中直接选择当前项目目录下的 keystore 文件 `ASLant`
6. 填入 alias 和密码(均为 `ASLant`)后点击 `Create`

## 主要页面

- `设备中心`：展示传感器和执行器卡片，支持添加、编辑、删除、历史记录查看
- `MQTT 配置`：配置服务器地址、端口、账号、密码和自动连接
- `Home Assistant`：配置服务地址、访问令牌和轮询参数
- `视频流`：测试和显示 WebSocket 视频连接
- `关于 / 更新日志`：展示版本说明和项目信息

## 配置说明

### MQTT

支持配置：

- 服务器地址
- 端口
- Client ID
- 用户名 / 密码
- 自动连接

### Home Assistant

当前使用的是 REST 轮询方式，不是 WebSocket 实时订阅。

需要配置：

- Home Assistant 服务地址
- Long-Lived Access Token
- 轮询间隔

### 设备卡片

卡片支持按以下维度配置：

- 数据源类型：MQTT / Home Assistant
- 设备类型：传感器 / 执行器
- 展示名称
- Topic 或 Entity ID
- JSON 字段名
- 单位后缀
- 控件样式：按钮、开关、滑块等

## 项目结构

```text
app/src/main/java/compose/iot/
  MainActivity.kt
  mqtt/
    MqttManager.kt
    HomeAssistantManager.kt
    SubscriptionCard.kt
    SensorHistoryManager.kt
  ui/theme/
    function/
    page/
      Page_Index.kt
      Page_login.kt
      Page_HomeAssistant.kt
      Page_About.kt
      Page_Changelog.kt
      video/
```

## 当前实现说明

- MQTT 连接、订阅、发布已经切到后台线程，避免阻塞界面
- Home Assistant 的监听器和轮询任务支持按实体释放
- 新增、编辑、删除卡片时，MQTT 与 Home Assistant 的订阅回收逻辑已做一致化处理

## 已知限制

- Home Assistant 当前是轮询，不是事件流订阅
- 项目目前没有单元测试和 UI 测试目录
- 配置信息当前主要通过 SharedPreferences 存储
- 核心页面 `Page_Index.kt` 仍然偏大，后续适合继续拆分

## 许可证

本项目使用 [MIT License](LICENSE)。
