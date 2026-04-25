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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.Dashboard
import compose.icons.tablericons.Home
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.Settings
import compose.iot.R
import compose.iot.ui.app.AppTab
import compose.iot.ui.app.LocalAppSettingsViewModel
import compose.iot.ui.theme.page.AboutPage
import compose.iot.ui.theme.page.DashPage
import compose.iot.ui.theme.page.IndexPage
import compose.iot.ui.theme.page.SettingsPage

@Composable
fun MainScaffold(navController: NavController) {
    val settingsViewModel = LocalAppSettingsViewModel.current
    val appSettings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val topCornerRadius =
        when (appSettings.cornerShapeLevel) {
            0 -> 8.dp
            1 -> 12.dp
            2 -> 16.dp
            else -> 8.dp
        }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.clip(RoundedCornerShape(topStart = topCornerRadius, topEnd = topCornerRadius)),
            ) {
                NavigationBarItem(
                    selected = appSettings.selectedTab == AppTab.INDEX,
                    onClick = { settingsViewModel.selectTab(AppTab.INDEX) },
                    icon = {
                        Icon(
                            imageVector = TablerIcons.Home,
                            contentDescription = stringResource(R.string.nav_home),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_home), style = MaterialTheme.typography.labelMedium) },
                )
                NavigationBarItem(
                    selected = appSettings.selectedTab == AppTab.DASH,
                    onClick = { settingsViewModel.selectTab(AppTab.DASH) },
                    icon = {
                        Icon(
                            imageVector = TablerIcons.Dashboard,
                            contentDescription = stringResource(R.string.nav_dashboard),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_dashboard), style = MaterialTheme.typography.labelMedium) },
                )
                NavigationBarItem(
                    selected = appSettings.selectedTab == AppTab.SETTINGS,
                    onClick = { settingsViewModel.selectTab(AppTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = TablerIcons.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.labelMedium) },
                )
                NavigationBarItem(
                    selected = appSettings.selectedTab == AppTab.ABOUT,
                    onClick = { settingsViewModel.selectTab(AppTab.ABOUT) },
                    icon = {
                        Icon(
                            imageVector = TablerIcons.InfoCircle,
                            contentDescription = stringResource(R.string.nav_about),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_about), style = MaterialTheme.typography.labelMedium) },
                )
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = appSettings.selectedTab,
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
                AppTab.INDEX -> IndexPage()
                AppTab.DASH -> DashPage(navController)
                AppTab.SETTINGS -> SettingsPage(navController)
                AppTab.ABOUT -> AboutPage(navController)
            }
        }
    }
}
