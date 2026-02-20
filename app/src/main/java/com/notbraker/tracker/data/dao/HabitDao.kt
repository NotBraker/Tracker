package com.notbraker.tracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notbraker.tracker.data.model.DayCompletionCount
import com.notbraker.tracker.data.model.Habit
import com.notbraker.tracker.data.model.HabitCompletion
import com.notbraker.tracker.data.model.HabitCompletionTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    fun observeHabitById(habitId: Long): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: Long): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: Long)

    @Query("UPDATE habits SET isArchived = 1 WHERE id = :habitId")
    suspend fun archiveHabit(habitId: Long)

    @Query("SELECT COUNT(*) FROM habits WHERE isArchived = 0")
    suspend fun countActiveHabits(): Int

    @Query("SELECT COUNT(*) FROM habits WHERE isArchived = 0 AND reminderEnabled = 1")
    suspend fun countReminderEnabledHabits(): Int

    @Query("SELECT * FROM habits WHERE isArchived = 0 AND reminderEnabled = 1")
    suspend fun getHabitsWithReminders(): List<Habit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: HabitCompletion)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteCompletion(habitId: Long, epochDay: Long)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteCompletionsForHabit(habitId: Long)

    @Query("SELECT habitId FROM habit_completions WHERE epochDay = :epochDay")
    fun observeCompletedHabitIdsForDay(epochDay: Long): Flow<List<Long>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY epochDay DESC")
    fun observeCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>

    @Query("SELECT epochDay FROM habit_completions WHERE habitId = :habitId ORDER BY epochDay DESC")
    suspend fun getCompletionEpochDaysForHabit(habitId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE epochDay = :epochDay")
    fun observeCompletedCountForDay(epochDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE epochDay = :epochDay")
    suspend fun getCompletedCountForDay(epochDay: Long): Int

    @Query(
        """
        SELECT epochDay, COUNT(*) AS completedCount
        FROM habit_completions
        WHERE epochDay BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY epochDay
        ORDER BY epochDay ASC
        """
    )
    fun observeCompletionCountsBetween(
        fromEpochDay: Long,
        toEpochDay: Long
    ): Flow<List<DayCompletionCount>>

    @Query(
        """
        SELECT habitId, COUNT(*) AS completedCount
        FROM habit_completions
        WHERE epochDay BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY habitId
        """
    )
    fun observeCompletionTotalsByHabit(
        fromEpochDay: Long,
        toEpochDay: Long
    ): Flow<List<HabitCompletionTotal>>
}
