package compose.iot.ui.theme.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.iot.R
import compose.iot.ui.theme.function.standardEnterTransition
import compose.iot.ui.theme.function.standardExitTransition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogPage() {
    val isVisible by remember { mutableStateOf(true) }

    val changelogItems =
        listOf(
            ChangelogItem(
                version = "1.2.1",
                date = "2025-05-13",
                changes = stringArrayResource(R.array.changelog_1_2_1).toList(),
            ),
            ChangelogItem(
                version = "1.2.0",
                date = "2025-05-04",
                changes = stringArrayResource(R.array.changelog_1_2_0).toList(),
            ),
            ChangelogItem(
                version = "1.1.9",
                date = "2025-04-26",
                changes = stringArrayResource(R.array.changelog_1_1_9).toList(),
            ),
            ChangelogItem(
                version = "1.1.8",
                date = "2025-04-20",
                changes = stringArrayResource(R.array.changelog_1_1_8).toList(),
            ),
            ChangelogItem(
                version = "1.1.7",
                date = "2025-04-13",
                changes = stringArrayResource(R.array.changelog_1_1_7).toList(),
            ),
            ChangelogItem(
                version = "1.1.6",
                date = "2025-04-12",
                changes = stringArrayResource(R.array.changelog_1_1_6).toList(),
            ),
            ChangelogItem(
                version = "1.1.5",
                date = "2025-04-10",
                changes = stringArrayResource(R.array.changelog_1_1_5).toList(),
            ),
            ChangelogItem(
                version = "1.1.3",
                date = "2025-04-08",
                changes = stringArrayResource(R.array.changelog_1_1_3).toList(),
            ),
            ChangelogItem(
                version = "1.1.0",
                date = "2025-04-05",
                changes = stringArrayResource(R.array.changelog_1_1_0).toList(),
            ),
            ChangelogItem(
                version = "1.0.0",
                date = "2025-01-20",
                changes = stringArrayResource(R.array.changelog_1_0_0).toList(),
            ),
            ChangelogItem(
                version = "0.9.0",
                date = "2024-12-28",
                changes = stringArrayResource(R.array.changelog_0_9_0).toList(),
            ),
            ChangelogItem(
                version = "0.8.1",
                date = "2024-09-08",
                changes = stringArrayResource(R.array.changelog_0_8_1).toList(),
            ),
            ChangelogItem(
                version = "0.5.0",
                date = "2024-06-21",
                changes = stringArrayResource(R.array.changelog_0_5_0).toList(),
            ),
            ChangelogItem(
                version = "0.4.4",
                date = "2024-06-02",
                changes = stringArrayResource(R.array.changelog_0_4_4).toList(),
            ),
            ChangelogItem(
                version = "0.0.5",
                date = "2024-03-19",
                changes = stringArrayResource(R.array.changelog_0_0_5).toList(),
            ),
            ChangelogItem(
                version = "0.0.2",
                date = "2024-03-01",
                changes = stringArrayResource(R.array.changelog_0_0_2).toList(),
            ),
        )

    AnimatedVisibility(
        visible = isVisible,
        enter = standardEnterTransition(initialOffsetY = -50),
        exit = standardExitTransition(targetOffsetY = -50),
    ) {
        compose.iot.ui.components.AppScaffold(
            title = stringResource(R.string.changelog),
            navController = null,
            showBackButton = false,
        ) { _ ->
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = changelogItems,
                    key = { _, item -> item.version },
                ) { index, item ->
                    ChangelogCard(
                        item = item,
                        isLatest = index == 0,
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ChangelogCard(
    item: ChangelogItem,
    isLatest: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isLatest) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // 版本号 + badge + 日期
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "v${item.version}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            if (isLatest) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                    if (isLatest) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = stringResource(R.string.changelog_latest),
                                modifier = Modifier.padding(horizontal = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // 更新内容
            item.changes.forEach { change ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = change,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

data class ChangelogItem(
    val version: String,
    val date: String,
    val changes: List<String>,
)
