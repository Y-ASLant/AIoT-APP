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
- 设备状态本地持久化（Room 数据库）
- 传感器历史数据查看与自动裁剪
- WebSocket 视频流测试与显示页面
- 基于 Material 3 的 Compose 原生界面
- 完整的深色模式支持与 Material You 动态取色，配合自定义全局应用主题
- 动态圆角风格自定义与自适应主题图标 (Themed App Icons)
- MVVM 架构（ViewModel + StateFlow + Channel）

## 技术栈

- **语言**: Kotlin 2.1.20
- **UI 框架**: Jetpack Compose (Material 3 / BOM 2025.02.00)
- **导航**: Android Navigation Compose
- **MQTT**: Eclipse Paho MQTT v3 / Android Service
- **网络**: OkHttp 4.12.0
- **数据持久化**: Room 2.6.1 + SharedPreferences
- **架构**: ViewModel + StateFlow + Channel（MVVM）
- **日志**: Timber
- **构建**: KSP + ktlint

## 运行要求

- Android 8.0 及以上（`minSdk 26`）
- `targetSdk 35` / `compileSdk 35`
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
- `系统设置`：全局界面风格个性化定制 (如深浅色模式切换、动态主题取色、圆角大小调整等)
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
- 卡片样式：高亮 / 极简 / 填充
- 控件样式：按钮、开关、滑块、输入框等

## 项目结构

```text
compose.iot/
├── AiotApp.kt                      # Application 全局依赖 (MqttManager, Database, Preferences)
├── AppState.kt                     # 全局应用状态
├── MainActivity.kt                 # 主 Activity (导航入口)
├── data/
│   ├── preferences/
│   │   └── PreferencesManager.kt   # SharedPreferences 统一管理
│   └── room/
│       ├── AppDatabase.kt          # Room 数据库定义
│       ├── Converters.kt           # Room 类型转换器
│       ├── SensorHistoryDao.kt     # 传感器历史 DAO
│       ├── SensorHistoryEntity.kt  # 传感器历史 Entity
│       └── SubscriptionCardDao.kt  # 设备卡片 DAO
├── mqtt/
│   ├── MqttManager.kt              # MQTT 连接管理
│   ├── HomeAssistantManager.kt     # Home Assistant REST 轮询集成
│   ├── SubscriptionCard.kt         # 设备卡片数据模型 (Room Entity)
│   ├── SensorHistoryData.kt        # 传感器历史数据模型
│   └── SensorHistoryManager.kt     # 传感器历史数据管理
├── ui/
│   ├── components/                 # 可复用卡片内容组件
│   │   ├── InputCardContent.kt     # 输入框卡片
│   │   ├── PushButtonCardContent.kt # 按钮卡片
│   │   ├── SensorCardContent.kt    # 传感器卡片
│   │   ├── SliderCardContent.kt    # 滑块卡片
│   │   └── SwitchCardContent.kt    # 开关卡片
│   ├── theme/
│   │   ├── function/               # UI 功能组件
│   │   │   ├── AnimationUtils.kt   # 动画工具
│   │   │   ├── Background.kt       # 背景组件
│   │   │   ├── MainScaffold.kt     # 主脚手架 + 底部导航
│   │   │   ├── MqttSubscribeDialog.kt # 设备配置对话框
│   │   │   └── SensorHistoryBottomSheet.kt # 传感器历史底部弹窗
│   │   ├── page/                   # 页面
│   │   │   ├── IndexPage.kt        # 设备中心主页
│   │   │   ├── LoginPage.kt        # MQTT 配置页
│   │   │   ├── DashPage.kt         # 仪表盘
│   │   │   ├── HomeAssistantPage.kt # HA 配置页
│   │   │   ├── SettingsPage.kt     # 系统设置页
│   │   │   ├── AboutPage.kt        # 关于页面
│   │   │   ├── ChangelogPage.kt    # 更新日志
│   │   │   ├── DeviceSubscriptionController.kt # 设备订阅控制器
│   │   │   ├── SubscriptionCardStorage.kt # 执行器状态持久化
│   │   │   └── video/
│   │   │       ├── VideoStreamPage.kt  # 视频流页面
│   │   │       └── WebSocketClient.kt  # WebSocket 客户端
│   │   └── ui/theme/               # 主题配置
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   └── viewmodel/
│       ├── IndexUiState.kt         # UI 状态数据类 + 事件定义
│       └── IndexViewModel.kt       # 设备中心 ViewModel
└── util/
    └── AppResult.kt                # 通用App结果封装
```

## 许可证

本项目使用 [MIT License](LICENSE)。
