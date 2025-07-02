package compose.iot.ui.theme.page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.iot.ui.theme.function.standardEnterTransition
import compose.iot.ui.theme.function.standardExitTransition

@Composable
fun Page_Changelog() {
    var isVisible by remember { mutableStateOf(true) }

    val changelogItems = listOf(
        ChangelogItem(
            version = "1.2.1",
            date = "2025-05-13",
            changes = listOf(
                "增加检查更新功能",
                "修复滑实时控制EMQX无效问题",
                "About界面信息显示优化",
                "其它Bug修复"
            )
        ),
        ChangelogItem(
            version = "1.2.0",
            date = "2025-05-04",
            changes = listOf(
                "删除蓝牙功能及界面",
                "增加视频流配置界面及功能",
                "WebSocket画面显示",
                "修复EMQX执行器开关问题",
                "增加滑块操作方式实时控制切换"
            )
        ),
        ChangelogItem(
            version = "1.1.9",
            date = "2025-04-26",
            changes = listOf(
                "HA 添加设备操作优化",
                "根据实体ID的前缀自动识别设备类型",
                "HA number 类型设备支持",
                "HA switch 类型设备支持",
                "HA light 类型设备支持",
                "HA button 类型设备支持",
                "修复HA设备添加时软件崩溃问题"
            )
        ),
        ChangelogItem(
            version = "1.1.8",
            date = "2025-04-20",
            changes = listOf(
                "增加BLE按钮与二级界面",
                "增加蓝牙权限获取",
                "滑块监听,实现自动更新",
                "HA添加设备界面增加搜索栏",
                "修复EMQX执行器无法记录具体状态问题",
                "蓝牙扫描、通信功能暂未实现"
            )
        ),
        ChangelogItem(
            version = "1.1.7",
            date = "2025-04-13",
            changes = listOf(
                "优化横屏模式下的界面显示",
                "修复 Home Assistant 设备状态显示异常的问题",
                "改进 HA 设备状态获取机制，提升显示速度",
                "优化错误处理和日志记录，提高系统稳定性"
            )
        ),
        ChangelogItem(
            version = "1.1.6",
            date = "2025-04-12",
            changes = listOf(
                "增加 Password-Based 客户端认证",
                "增加首页瀑布流布局",
                "修复其他已知问题"
            )
        ),
        ChangelogItem(
            version = "1.1.5",
            date = "2025-04-10",
            changes = listOf(
                "新增更新日志界面",
                "重写SplashScreen启动界面，使用系统API",
                "修复其他已知问题"
            )
        ),
        ChangelogItem(
            version = "1.1.3",
            date = "2025-04-08",
            changes = listOf(
                "新增传感器历史数据功能",
                "优化底部弹出菜单交互",
                "修复其他已知问题"
            )
        ),
        ChangelogItem(
            version = "1.1.0",
            date = "2025-04-05",
            changes = listOf(
                "新增 Home Assistant 集成",
                "支持自动发现已有设备",
                "优化UI交互体验"
            )
        ),
        ChangelogItem(
            version = "1.0.0",
            date = "2025-01-20",
            changes = listOf(
                "继续完善已有功能",
                "修复已知Bug，统一UI样式，优化打包APK体积",
                "发布release版，APK体积大幅减小(下降90%)"
            )
        ),
        ChangelogItem(
            version = "0.9.0",
            date = "2024-12-28",
            changes = listOf(
                "完善已有功能",
                "修复已知Bug，升级引用包编译版本",
                "APK打包体积优化，启用R8编译模式",
                "简单实现APK更新功能(能用但不好用)"
            )
        ),
        ChangelogItem(
            version = "0.8.1",
            date = "2024-09-08",
            changes = listOf(
                "新增 EMQX 集成",
                "支持 MQTT-3.1.1 协议",
                "支持实时数据监控"
            )
        ),
        ChangelogItem(
            version = "0.5.0",
            date = "2024-06-21",
            changes = listOf(
                "增加 Paho-mqtt 包",
                "支持 MQTT 协议简单通信",
                "增加多界面",
                "增加底部导航栏",
                "增加导航栏按钮状态切换",
            )
        ),
        ChangelogItem(
            version = "0.4.4",
            date = "2024-06-02",
            changes = listOf(
                "增加 Paho-mqtt 包",
                "支持 MQTT 协议简单通信",
                "增加多界面",
                "增加底部导航栏",
                "增加导航栏按钮状态切换",
            )
        ),
        ChangelogItem(
            version = "0.0.5",
            date = "2024-03-19",
            changes = listOf(
                "Toast 与按钮联动",
                "手动获取权限问题",
                "使用 Edge To Edge 状态栏沉浸",
                "使用 Material Design 3动态主题"
            )
        ),
        ChangelogItem(
            version = "0.0.2",
            date = "2024-03-01",
            changes = listOf(
                "首次发布",
                "使用 Compose 创建基础项目",
                "尝试增加 Material Design 3 组件"
            )
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = standardEnterTransition(initialOffsetY = -50),
            exit = standardExitTransition(targetOffsetY = -50)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp)
            ) {
                Spacer(modifier = Modifier.height(60.dp))
                
                // 标题
                Text(
                    text = "更新日志",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 更新日志列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(changelogItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // 版本号和日期
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Version ${item.version}",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = item.date,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // 更新内容
                                item.changes.forEach { change ->
                                    Text(
                                        text = "• $change",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // 底部间距
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

data class ChangelogItem(
    val version: String,
    val date: String,
    val changes: List<String>
) 