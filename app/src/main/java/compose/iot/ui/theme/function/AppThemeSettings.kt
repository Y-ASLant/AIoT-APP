package compose.iot.ui.theme.function

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import compose.iot.AppState
import compose.iot.data.preferences.PreferencesManager
import compose.iot.ui.theme.ui.theme.*

enum class AppDarkMode(val value: Int, val title: String, val icon: ImageVector) {
    Auto(0, "自动", TablerIcons.Settings),
    Light(1, "浅色", TablerIcons.Sun),
    Dark(2, "深色", TablerIcons.MoonStars),
}

data class ThemeColorItemData(
    val id: Int,
    val primaryLight: Color,
    val primaryDark: Color,
    val primaryContainerLight: Color,
    val tertiaryContainerLight: Color,
)

val AppThemeColorsList =
    listOf(
        // ID 0 is Dynamic
        // Blue
        ThemeColorItemData(1, Color(0xFF0061A4), Color(0xFF9ECAFF), Color(0xFFD1E4FF), Color(0xFFD7E2FF)),
        // Default M3 Purple
        ThemeColorItemData(2, Color(0xFF6750A4), Color(0xFFD0BCFF), Color(0xFFEADDFF), Color(0xFFFFD8E4)),
        // Orange
        ThemeColorItemData(3, Color(0xFF825500), Color(0xFFFFB951), Color(0xFFFFDDB3), Color(0xFFFFDEAC)),
        // Mint Green
        ThemeColorItemData(4, Color(0xFF006C4C), Color(0xFF41E0A0), Color(0xFF89F8C7), Color(0xFFCDE8DF)),
        // Aqua
        ThemeColorItemData(5, Color(0xFF006874), Color(0xFF4FD8EB), Color(0xFF97F0FF), Color(0xFFB1EBF2)),
        // Rose
        ThemeColorItemData(6, Color(0xFF984061), Color(0xFFFFB0C8), Color(0xFFFFD9E2), Color(0xFFFFD9E2)),
        // Forest
        ThemeColorItemData(7, Color(0xFF386A20), Color(0xFF9CD67D), Color(0xFFB7F397), Color(0xFFD3E7CD)),
        // Rust
        ThemeColorItemData(8, Color(0xFF9C432A), Color(0xFFFFB4A4), Color(0xFFFFDAD3), Color(0xFFFFDBD1)),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemePage(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val isSystemDark = isSystemInDarkTheme()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("应用主题") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            TablerIcons.ChevronLeft,
                            contentDescription = "返回",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
        ) {
            // Preview Mockup
            ExampleThemePreview()

            TitleItem(text = "调色板")

            val isDarkTheme =
                when (AppState.darkMode.intValue) {
                    AppDarkMode.Light.value -> false
                    AppDarkMode.Dark.value -> true
                    else -> isSystemDark
                }

            ThemePaletteItem(
                themeColor = AppState.themeColor.intValue,
                isDarkMode = isDarkTheme,
                enabled = AppState.darkMode.intValue != AppDarkMode.Auto.value,
                onChange = {
                    AppState.themeColor.intValue = it
                    prefs.themeColor = it
                },
            )

            TitleItem(text = "深色模式")
            DarkModeItem(
                currentMode = AppState.darkMode.intValue,
                onChange = {
                    AppState.darkMode.intValue = it
                    prefs.darkMode = it
                    if (it == AppDarkMode.Auto.value) {
                        AppState.themeColor.intValue = 0
                        prefs.themeColor = 0
                    }
                },
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ExampleThemePreview() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(0.6f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = TablerIcons.Server,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "AIoT X", style = MaterialTheme.typography.labelLarge)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(60.dp).fillMaxWidth(0.85f),
                ) {}

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(60.dp).fillMaxWidth(0.85f),
                ) {}

                Spacer(modifier = Modifier.height(80.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.height(45.dp).fillMaxWidth(),
                ) {}
            }
        }
    }
}

@Composable
internal fun TitleItem(
    text: String,
) = Text(
    text = text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 18.dp, top = 24.dp),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemePaletteItem(
    themeColor: Int,
    isDarkMode: Boolean,
    enabled: Boolean = true,
    onChange: (Int) -> Unit,
) {
    FlowRow(
        modifier =
            Modifier
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pContext = androidx.compose.ui.platform.LocalContext.current
            val dynamicColorScheme = if (isDarkMode) dynamicDarkColorScheme(pContext) else dynamicLightColorScheme(pContext)
            ThemeColorBox(
                selected = themeColor == 0,
                enabled = enabled,
                colorName = "动态",
                primaryColor = dynamicColorScheme.primary,
                primaryContainer = dynamicColorScheme.primaryContainer,
                tertiaryContainer = dynamicColorScheme.tertiaryContainer,
                onPrimary = dynamicColorScheme.onPrimary,
                onClick = { onChange(0) },
            )
        }

        AppThemeColorsList.forEach { colorData ->
            ThemeColorBox(
                selected = themeColor == colorData.id,
                enabled = enabled,
                colorName = null,
                primaryColor = if (isDarkMode) colorData.primaryDark else colorData.primaryLight,
                primaryContainer = if (isDarkMode) colorData.primaryDark.copy(alpha = 0.3f) else colorData.primaryContainerLight,
                tertiaryContainer = if (isDarkMode) colorData.primaryDark.copy(alpha = 0.25f) else colorData.tertiaryContainerLight,
                onPrimary = Color.White,
                onClick = { onChange(colorData.id) },
            )
        }
    }
}

@Composable
private fun ThemeColorBox(
    selected: Boolean,
    enabled: Boolean,
    colorName: String?,
    primaryColor: Color,
    primaryContainer: Color,
    tertiaryContainer: Color,
    onPrimary: Color,
    onClick: () -> Unit,
) {
    val boxColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)

    Box(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable(
                    onClick = onClick,
                    enabled = enabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .background(color = boxColor)
                .size(60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .fillMaxSize(0.75f),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .background(color = primaryContainer),
                )
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(color = tertiaryContainer),
                )
            }

            Spacer(
                modifier =
                    Modifier
                        .fillMaxSize(0.5f)
                        .clip(CircleShape)
                        .background(color = if (selected) onPrimary else primaryColor),
            )

            if (selected) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    imageVector = TablerIcons.CircleCheck,
                    contentDescription = null,
                    tint = primaryColor,
                )
            }
        }
    }
}

@Composable
fun DarkModeItem(
    currentMode: Int,
    onChange: (Int) -> Unit,
) = LazyRow(
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(15.dp),
) {
    val modes = AppDarkMode.values()
    items(modes) { item ->
        val selected = item.value == currentMode
        Box(
            modifier =
                Modifier
                    .clip(MaterialTheme.shapes.large)
                    .clickable(
                        onClick = { onChange(item.value) },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
                    .background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(if (selected) 6.dp else 1.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val animateZ by animateFloatAsState(
                    targetValue = if (selected) 0f else 360f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    label = "animateZ",
                )

                Icon(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = if (selected) animateZ else 0f },
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.5f),
                )

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
