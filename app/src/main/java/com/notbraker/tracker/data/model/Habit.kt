package com.notbraker.tracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val icon: String,
    val frequencyLabel: String,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val streakCurrent: Int = 0,
    val streakLongest: Int = 0,
    val lastCompletedEpochDay: Long? = null,
    val templateId: String? = null,
    val isArchived: Boolean = false,
    val createdAtEpochDay: Long,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "epochDay"],
    indices = [Index("habitId"), Index("epochDay")]
)
data class HabitCompletion(
    val habitId: Long,
    val epochDay: Long,
    val completedAtEpochMillis: Long
)

data class DayCompletionCount(
    val epochDay: Long,
    val completedCount: Int
)

data class HabitCompletionTotal(
    val habitId: Long,
    val completedCount: Int
)

data class HabitDayState(
    val habit: Habit,
    val isCompleted: Boolean
)

data class HabitCreationResult(
    val success: Boolean,
    val reason: String? = null,
    val habitId: Long? = null
)
