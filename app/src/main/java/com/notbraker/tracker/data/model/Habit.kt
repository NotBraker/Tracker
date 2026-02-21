package com.notbraker.tracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Junction
import androidx.room.Embedded

enum class HabitFrequencyType {
    DAILY,
    WEEKLY
}

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val icon: String,
    val frequencyLabel: String,
    val frequencyType: String = HabitFrequencyType.DAILY.name,
    val weeklyTarget: Int = 7,
    val dailyTarget: Int = 1,
    val scheduledDays: String = "",
    val timesPerDayTarget: Int = 1,
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

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "routine_habit_cross_ref",
    primaryKeys = ["routineId", "habitId"],
    indices = [Index("routineId"), Index("habitId")],
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineHabitCrossRef(
    val routineId: Long,
    val habitId: Long,
    val sortOrder: Int = 0
)

data class RoutineSummary(
    val id: Long,
    val name: String,
    val description: String,
    val habitCount: Int
)

data class RoutineWithHabits(
    @Embedded val routine: Routine,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RoutineHabitCrossRef::class,
            parentColumn = "routineId",
            entityColumn = "habitId"
        )
    )
    val habits: List<Habit>
)
