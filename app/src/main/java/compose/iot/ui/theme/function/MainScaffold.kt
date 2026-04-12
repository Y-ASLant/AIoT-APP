package compose.iot.ui.theme.function

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import compose.iot.AppState
import compose.iot.R
import compose.iot.ui.theme.page.AboutPage
import compose.iot.ui.theme.page.DashPage
import compose.iot.ui.theme.page.IndexPage
import compose.iot.ui.theme.page.SettingsPage
import kotlinx.coroutines.CoroutineScope

/**
 * 应用主框架 — 采用 M3 Scaffold + NavigationBar
 *
 * 页面切换动画使用 M3 推荐的 Fade Through（淡入 + 微缩放）模式
 */
@Composable
fun MainScaffold(
    scope: CoroutineScope,
    navController: NavController,
) {
    Scaffold(
        bottomBar = {
            if (AppState.selectedTab.intValue in 0..3) {
                NavigationBar(
                    modifier =
                        Modifier
                            .clip(
                                androidx.compose.foundation.shape.RoundedCornerShape(
                                    topStart = compose.iot.AppState.cornerShapeLevel.intValue.let {
                                        when (it) {
                                            0 -> 8.dp
                                            1 -> 12.dp
                                            2 -> 16.dp
                                            else -> 8.dp
                                        }
                                    },
                                    topEnd = compose.iot.AppState.cornerShapeLevel.intValue.let {
                                        when (it) {
                                            0 -> 8.dp
                                            1 -> 12.dp
                                            2 -> 16.dp
                                            else -> 8.dp
                                        }
                                    }
                                )
                            ),
                ) {
                    NavigationBarItem(
                        selected = AppState.selectedTab.intValue == 0,
                        onClick = {
                            AppState.selectedTab.intValue = 0
                        },
                        icon = {
                            Icon(
                                painter =
                                    painterResource(
                                        if (AppState.selectedTab.intValue == 0) R.drawable.bnbfill else R.drawable.bnbline,
                                    ),
                                contentDescription = "首页",
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = {
                            Text(
                                "首页",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                    NavigationBarItem(
                        selected = AppState.selectedTab.intValue == 1,
                        onClick = {
                            AppState.selectedTab.intValue = 1
                        },
                        icon = {
                            Icon(
                                painter =
                                    painterResource(
                                        if (AppState.selectedTab.intValue == 1) R.drawable.boardfill else R.drawable.boardline,
                                    ),
                                contentDescription = "面板",
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = {
                            Text(
                                "面板",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                    NavigationBarItem(
                        selected = AppState.selectedTab.intValue == 2,
                        onClick = {
                            AppState.selectedTab.intValue = 2
                        },
                        icon = {
                            Icon(
                                imageVector = if (AppState.selectedTab.intValue == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "设置",
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = {
                            Text(
                                "设置",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                    NavigationBarItem(
                        selected = AppState.selectedTab.intValue == 3,
                        onClick = {
                            AppState.selectedTab.intValue = 3
                        },
                        icon = {
                            Icon(
                                painter =
                                    painterResource(
                                        if (AppState.selectedTab.intValue == 3) R.drawable.terminalboxfill else R.drawable.terminalboxline,
                                    ),
                                contentDescription = "关于",
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = {
                            Text(
                                "关于",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        // M3 Fade Through 动画: fadeOut → scaleIn + fadeIn
        AnimatedContent(
            targetState = AppState.selectedTab.intValue,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith
                    fadeOut(animationSpec = tween(200))
            },
            label = "page_switch",
        ) { targetTab ->
            when (targetTab) {
                0 -> IndexPage()
                1 -> DashPage(navController)
                2 -> SettingsPage(navController)
                3 -> AboutPage(navController)
                else -> {}
            }
        }
    }
}
