package com.notbraker.tracker.feature.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.notbraker.tracker.core.components.AppCard
import com.notbraker.tracker.core.components.SectionHeader
import com.notbraker.tracker.core.designsystem.HabitColors
import com.notbraker.tracker.core.designsystem.TrackerTheme

data class HabitTemplate(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val icon: String,
    val defaultReminderHour: Int,
    val defaultReminderMinute: Int
)

private val templates = listOf(
    HabitTemplate("drink_water", "Health", "Drink water", "Hydrate with 8 cups.", "💧", 9, 0),
    HabitTemplate("eat_veggies", "Health", "Eat vegetables", "Add greens to each meal.", "🥦", 12, 30),
    HabitTemplate("walk_8k", "Health", "Walk 8k steps", "Reach 8,000+ steps every day.", "🚶", 18, 0),
    HabitTemplate("deep_work", "Productivity", "Deep work 1h", "One focused hour without distractions.", "🎯", 10, 0),
    HabitTemplate("plan_tomorrow", "Productivity", "Plan tomorrow", "Create top priorities for tomorrow.", "🗓", 20, 0),
    HabitTemplate("read_30", "Learning", "Read 30 minutes", "Read a book for at least 30 minutes.", "📚", 21, 0),
    HabitTemplate("language_practice", "Learning", "Practice language", "Daily language repetition session.", "🗣", 19, 30)
)

@Composable
fun TemplatesScreen(
    onTemplateCreate: (HabitTemplate) -> Unit,
    onBack: () -> Unit = {}
) {
    val spacing = TrackerTheme.spacing
    var selectedTemplate by remember { mutableStateOf<HabitTemplate?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HabitColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                Column {
                    TextButton(onClick = onBack) { Text("Back") }
                    Text(
                        text = "Templates",
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

            templates.groupBy { it.category }.forEach { (category, entries) ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${template.icon} ${template.title}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                onClick = { selectedTemplate = template },
                                shape = MaterialTheme.shapes.medium,
                                color = HabitColors.PrimaryAccent.copy(alpha = 0.24f)
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
