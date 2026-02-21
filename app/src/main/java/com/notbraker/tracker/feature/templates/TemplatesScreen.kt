package com.notbraker.tracker.feature.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.SectionHeader
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme
import com.notbraker.tracker.data.template.TemplateCatalog
import com.notbraker.tracker.data.template.getHiddenTemplateIdsFlow

@Composable
fun TemplatesScreen(
    onTemplateCreate: (TemplateCatalog.Template) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val hiddenTemplateIds by context.getHiddenTemplateIdsFlow().collectAsStateWithLifecycle(initialValue = emptySet())
    val visibleTemplates = TemplateCatalog.allTemplates.filter { it.id !in hiddenTemplateIds }

    val spacing = TrackerTheme.spacing
    var selectedTemplate by remember { mutableStateOf<TemplateCatalog.Template?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.BackgroundPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                Column {
                    TextButton(onClick = onBack) { Text("Back") }
                    Text(
                        text = "Popular Habits",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Start faster with curated productivity habits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            visibleTemplates.groupBy { it.category }.forEach { (category, entries) ->
                item {
                    SectionHeader(title = category)
                }
                items(entries, key = { it.id }) { template ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(TrackerTheme.spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = HabitColors.SurfaceAccentGlow
                                ) {
                                    Text(
                                        text = template.icon,
                                        modifier = Modifier.padding(horizontal = TrackerTheme.spacing.sm, vertical = TrackerTheme.spacing.xs),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Column {
                                    Text(
                                        text = template.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = template.description,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = HabitColors.OnSurfaceMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(TrackerTheme.spacing.xxs)) {
                                        template.tags.forEach { tag ->
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = HabitColors.Surface
                                            ) {
                                                Text(
                                                    tag,
                                                    modifier = Modifier.padding(horizontal = TrackerTheme.spacing.xs, vertical = TrackerTheme.spacing.xxs),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Surface(
                                onClick = { selectedTemplate = template },
                                shape = MaterialTheme.shapes.medium,
                                color = HabitColors.Surface
                            ) {
                                Text(
                                    "Use",
                                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.navigationBarsPadding()) {
                    Text("")
                }
            }
        }
    }

    selectedTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { selectedTemplate = null },
            title = { Text("Create from template?") },
            text = {
                Text("Create \"${template.title}\" with default reminder at ${template.defaultReminderHour}:${template.defaultReminderMinute.toString().padStart(2, '0')}.")
            },
            dismissButton = {
                TextButton(onClick = { selectedTemplate = null }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTemplate = null
                        onTemplateCreate(template)
                    }
                ) { Text("Create") }
            }
        )
    }
}
