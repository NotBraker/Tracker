package com.notbraker.tracker.data.template

/**
 * Static catalog of template definitions. Templates are never deleted;
 * per-user "used" state is stored separately in TemplatePreferences.
 */
object TemplateCatalog {

    data class Template(
        val id: String,
        val category: String,
        val title: String,
        val description: String,
        val icon: String,
        val tags: List<String>,
        val defaultReminderHour: Int,
        val defaultReminderMinute: Int
    ) {
        val templateTag: String get() = "Template: $title"
    }

    val allTemplates: List<Template> = listOf(
        Template("PH1", "Health", "Hydration Protocol", "Reach daily water target.", "H", listOf("DAILY", "TARGET"), 9, 0),
        Template("PH2", "Health", "Mobility Session", "10-minute mobility reset.", "M", listOf("DAILY"), 7, 30),
        Template("PH3", "Performance", "8k Steps", "Hit 8,000+ movement baseline.", "P", listOf("DAILY", "TARGET"), 18, 0),
        Template("PH4", "Focus", "Deep Work Block", "Single uninterrupted 60-minute sprint.", "F", listOf("DAILY"), 10, 0),
        Template("PH5", "Focus", "Plan Next Day", "Define top priorities before shutdown.", "N", listOf("WEEKLY"), 20, 0),
        Template("PH6", "Learning", "Read 30 Minutes", "Deliberate reading session.", "L", listOf("DAILY"), 21, 0),
        Template("PH7", "Learning", "Language Practice", "Spaced repetition and speaking drill.", "S", listOf("WEEKLY"), 19, 30),
        Template("PH8", "Discipline", "Sleep Start Time", "Lock bedtime discipline window.", "D", listOf("DAILY", "TARGET"), 22, 0)
    )

    fun getById(id: String): Template? = allTemplates.find { it.id == id }
}
