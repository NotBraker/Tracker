package com.notbraker.tracker.data.repository

import com.notbraker.tracker.data.dao.HabitDao
import com.notbraker.tracker.data.model.DayCompletionCount
import com.notbraker.tracker.data.model.Habit
import com.notbraker.tracker.data.model.HabitCompletion
import com.notbraker.tracker.data.model.HabitCompletionTotal
import com.notbraker.tracker.data.model.HabitCreationResult
import com.notbraker.tracker.data.model.HabitDayState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class HabitRepository(
    private val habitDao: HabitDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        const val FREE_HABIT_LIMIT = 5
        const val FREE_REMINDER_LIMIT = 1
    }

    fun observeTodayHabits(epochDay: Long): Flow<List<HabitDayState>> {
        return combine(
            habitDao.observeActiveHabits(),
            habitDao.observeCompletedHabitIdsForDay(epochDay)
        ) { habits, completedIds ->
            val doneSet = completedIds.toSet()
            habits.map { habit ->
                HabitDayState(habit = habit, isCompleted = doneSet.contains(habit.id))
            }
        }.flowOn(ioDispatcher)
    }

    fun observeCompletionCountsBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<DayCompletionCount>> {
        return habitDao.observeCompletionCountsBetween(fromEpochDay, toEpochDay)
    }

    fun observeCompletionTotalsByHabit(
        fromEpochDay: Long,
        toEpochDay: Long
    ): Flow<List<HabitCompletionTotal>> = habitDao.observeCompletionTotalsByHabit(fromEpochDay, toEpochDay)

    fun observeActiveHabits(): Flow<List<Habit>> = habitDao.observeActiveHabits()

    fun observeHabit(habitId: Long): Flow<Habit?> = habitDao.observeHabitById(habitId)

    fun observeCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> {
        return habitDao.observeCompletionsForHabit(habitId)
    }

    suspend fun createHabit(
        name: String,
        description: String,
        icon: String,
        frequencyLabel: String,
        reminderHour: Int?,
        reminderMinute: Int?,
        templateId: String?,
        isPremium: Boolean
    ): HabitCreationResult = withContext(ioDispatcher) {
        val currentHabitCount = habitDao.countActiveHabits()
        if (!isPremium && currentHabitCount >= FREE_HABIT_LIMIT) {
            return@withContext HabitCreationResult(
                success = false,
                reason = "Free plan supports up to $FREE_HABIT_LIMIT active habits."
            )
        }

        val wantsReminder = reminderHour != null && reminderMinute != null
        if (!isPremium && wantsReminder && habitDao.countReminderEnabledHabits() >= FREE_REMINDER_LIMIT) {
            return@withContext HabitCreationResult(
                success = false,
                reason = "Free plan supports up to $FREE_REMINDER_LIMIT reminder."
            )
        }

        val newHabit = Habit(
            name = name.trim(),
            description = description.trim(),
            icon = icon,
            frequencyLabel = frequencyLabel,
            reminderEnabled = wantsReminder,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            createdAtEpochDay = currentEpochDay(),
            sortOrder = currentHabitCount + 1,
            templateId = templateId
        )
        val habitId = habitDao.insertHabit(newHabit)
        HabitCreationResult(success = true, habitId = habitId)
    }

    suspend fun updateHabit(habit: Habit) = withContext(ioDispatcher) {
        habitDao.updateHabit(habit)
    }

    suspend fun archiveHabit(habitId: Long) = withContext(ioDispatcher) {
        habitDao.archiveHabit(habitId)
    }

    suspend fun deleteHabit(habitId: Long) = withContext(ioDispatcher) {
        habitDao.deleteCompletionsForHabit(habitId)
        habitDao.deleteHabit(habitId)
    }

    suspend fun setHabitCompletion(
        habitId: Long,
        epochDay: Long,
        isCompleted: Boolean
    ) = withContext(ioDispatcher) {
        val habit = habitDao.getHabitById(habitId) ?: return@withContext
        if (isCompleted) {
            habitDao.upsertCompletion(
                HabitCompletion(
                    habitId = habitId,
                    epochDay = epochDay,
                    completedAtEpochMillis = System.currentTimeMillis()
                )
            )
            val newStreak = computeNextStreak(habit.lastCompletedEpochDay, epochDay, habit.streakCurrent)
            habitDao.updateHabit(
                habit.copy(
                    streakCurrent = newStreak,
                    streakLongest = maxOf(habit.streakLongest, newStreak),
                    lastCompletedEpochDay = maxOf(habit.lastCompletedEpochDay ?: Long.MIN_VALUE, epochDay)
                )
            )
        } else {
            habitDao.deleteCompletion(habitId, epochDay)
            val allDays = habitDao.getCompletionEpochDaysForHabit(habitId)
            val recalculated = recalculateStreak(allDays)
            habitDao.updateHabit(
                habit.copy(
                    streakCurrent = recalculated.first,
                    streakLongest = maxOf(habit.streakLongest, recalculated.second),
                    lastCompletedEpochDay = allDays.firstOrNull()
                )
            )
        }
    }

    suspend fun updateReminder(
        habitId: Long,
        enabled: Boolean,
        hour: Int?,
        minute: Int?,
        isPremium: Boolean
    ): HabitCreationResult = withContext(ioDispatcher) {
        val habit = habitDao.getHabitById(habitId)
            ?: return@withContext HabitCreationResult(success = false, reason = "Habit not found.")

        if (enabled && (!isPremium) && habitDao.countReminderEnabledHabits() >= FREE_REMINDER_LIMIT && !habit.reminderEnabled) {
            return@withContext HabitCreationResult(
                success = false,
                reason = "Free plan supports up to $FREE_REMINDER_LIMIT reminder."
            )
        }

        habitDao.updateHabit(
            habit.copy(
                reminderEnabled = enabled,
                reminderHour = hour,
                reminderMinute = minute
            )
        )
        HabitCreationResult(success = true, habitId = habitId)
    }

    private fun computeNextStreak(lastCompleted: Long?, today: Long, currentStreak: Int): Int {
        return when {
            lastCompleted == null -> 1
            lastCompleted == today -> currentStreak
            lastCompleted == today - 1 -> currentStreak + 1
            else -> 1
        }
    }

    private fun recalculateStreak(completedDaysDesc: List<Long>): Pair<Int, Int> {
        if (completedDaysDesc.isEmpty()) return 0 to 0
        var current = 1
        var longest = 1
        var streakCursor = completedDaysDesc[0]
        for (i in 1 until completedDaysDesc.size) {
            val day = completedDaysDesc[i]
            if (day == streakCursor - 1) {
                current += 1
                streakCursor = day
            } else {
                longest = maxOf(longest, current)
                current = 1
                streakCursor = day
            }
        }
        longest = maxOf(longest, current)
        return current to longest
    }

    private fun currentEpochDay(): Long = java.time.LocalDate.now().toEpochDay()
}
