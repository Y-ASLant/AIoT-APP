package compose.iot.ui.theme.function

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import compose.iot.AppState
import compose.iot.ui.theme.page.Page_About
import compose.iot.ui.theme.page.Page_Dash
import compose.iot.ui.theme.page.Page_Index
import compose.iot.mqtt.MqttManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.navigation.NavController

@Composable
fun Page_Switch(
    scope: CoroutineScope,
    navController: NavController,
    mqttManager: MqttManager
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = AppState.selectedTab.intValue,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(
                        animationSpec = tween(350)
                    )
                }, label = ""
            ) { targetTab ->
                when (targetTab) {
                    0 -> Page_Index(mqttManager)
                    1 -> Page_Dash(navController)
                    2 -> Page_About(navController)
                    else -> {} // 可以添加默认页面或者错误处理
                }
            }
        }

        // 只在主要的三个页面显示底栏
        if (AppState.selectedTab.intValue in 0..2) {
            Bottom_button(AppState.selectedTab.intValue) { newIndex ->
                scope.launch {
                    AppState.selectedTab.intValue = newIndex
                }
            }
        }
    }
}