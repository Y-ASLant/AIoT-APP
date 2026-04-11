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

- Kotlin 2.1.20
- Jetpack Compose (Material 3)
- Android Navigation Compose
- Eclipse Paho MQTT Client
- OkHttp / Java-WebSocket
- SharedPreferences 本地存储
- ktlint 代码风格检查

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

### 代码质量检查

项目已集成 ktlint 用于代码风格检查。

```powershell
# 检查代码风格
./gradlew ktlintCheck --no-daemon

# 自动格式化代码
./gradlew ktlintFormat --no-daemon
```

> 推荐使用 Android Studio 内置格式化 (`Ctrl+Alt+L`)，基于 `.editorconfig` 配置。

### APK 构建

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
compose.iot/
├── mqtt/                      # MQTT 通信层
│   ├── MqttManager.kt         # MQTT 连接管理
│   ├── HomeAssistantManager.kt # Home Assistant 集成
│   ├── SubscriptionCard.kt    # 设备卡片管理
│   └── SensorHistoryManager.kt # 传感器历史数据
├── ui/theme/
│   ├── function/               # UI 组件
│   │   ├── Bottom_Bar.kt       # 底部导航栏
│   │   └── Dialog.kt           # 对话框组件
│   ├── page/                  # 页面
│   │   ├── Index.kt           # 设备中心主页
│   │   ├── Login.kt           # 登录配置
│   │   ├── Dash.kt            # 仪表盘
│   │   ├── HomeAssistant.kt   # HA 配置
│   │   ├── About.kt           # 关于页面
│   │   ├── Changelog.kt       # 更新日志
│   │   └── video/             # 视频流页面
│   └── theme/                  # 主题配置
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt
```

## 许可证

本项目使用 [MIT License](LICENSE)。
