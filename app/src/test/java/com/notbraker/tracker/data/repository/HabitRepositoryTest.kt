package com.notbraker.tracker.data.repository

import com.notbraker.tracker.data.dao.HabitDao
import com.notbraker.tracker.data.model.DayCompletionCount
import com.notbraker.tracker.data.model.Habit
import com.notbraker.tracker.data.model.HabitCompletion
import com.notbraker.tracker.data.model.HabitCompletionTotal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitRepositoryTest {

    private lateinit var dao: InMemoryHabitDao
    private lateinit var repository: HabitRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        dao = InMemoryHabitDao()
        repository = HabitRepository(dao, testDispatcher)
    }

    @Test
    fun freePlanBlocksSixthHabit() = runTest(testDispatcher) {
        repeat(5) { index ->
            dao.insertHabit(
                Habit(
                    name = "Habit $index",
                    description = "desc",
                    icon = "✨",
                    frequencyLabel = "Daily",
                    createdAtEpochDay = 100
                )
            )
        }

        val result = repository.createHabit(
            name = "Sixth",
            description = "blocked",
            icon = "🚫",
            frequencyLabel = "Daily",
            reminderHour = null,
            reminderMinute = null,
            templateId = null,
            isPremium = false
        )

        assertFalse(result.success)
        assertTrue(result.reason.orEmpty().contains("Free plan supports up to 5 active habits"))
    }

    @Test
    fun freePlanBlocksSecondReminder() = runTest(testDispatcher) {
        dao.insertHabit(
            Habit(
                name = "Water",
                description = "Hydrate",
                icon = "💧",
                frequencyLabel = "Daily",
                reminderEnabled = true,
                reminderHour = 9,
                reminderMinute = 0,
                createdAtEpochDay = 100
            )
        )

        val result = repository.createHabit(
            name = "Workout",
            description = "Move",
            icon = "🏃",
            frequencyLabel = "Daily",
            reminderHour = 7,
            reminderMinute = 30,
            templateId = null,
            isPremium = false
        )

        assertFalse(result.success)
        assertTrue(result.reason.orEmpty().contains("Free plan supports up to 1 reminder"))
    }

    @Test
    fun completionUpdatesStreakAndLongest() = runTest(testDispatcher) {
        val id = dao.insertHabit(
            Habit(
                name = "Read",
                description = "30 min",
                icon = "📚",
                frequencyLabel = "Daily",
                createdAtEpochDay = 100
            )
        )

        repository.setHabitCompletion(id, 100, true)
        repository.setHabitCompletion(id, 101, true)
        var habit = dao.getHabitById(id)!!
        assertEquals(2, habit.streakCurrent)
        assertEquals(2, habit.streakLongest)

        repository.setHabitCompletion(id, 101, false)
        habit = dao.getHabitById(id)!!
        assertEquals(1, habit.streakCurrent)
        assertEquals(2, habit.streakLongest)
    }
}

private class InMemoryHabitDao : HabitDao {
    private var nextId = 1L
    private val habits = mutableListOf<Habit>()
    private val completions = mutableListOf<HabitCompletion>()
    private val habitsFlow = MutableStateFlow<List<Habit>>(emptyList())
    private val completionsFlow = MutableStateFlow<List<HabitCompletion>>(emptyList())

    override fun observeActiveHabits(): Flow<List<Habit>> {
        return habitsFlow.map { list ->
            list.filter { !it.isArchived }.sortedWith(compareBy(Habit::sortOrder, Habit::id))
        }
    }

    override fun observeHabitById(habitId: Long): Flow<Habit?> {
        return habitsFlow.map { list -> list.firstOrNull { it.id == habitId } }
    }

    override suspend fun getHabitById(habitId: Long): Habit? = habits.firstOrNull { it.id == habitId }

    override suspend fun insertHabit(habit: Habit): Long {
        val id = nextId++
        habits.add(habit.copy(id = id))
        pushHabits()
        return id
    }

    override suspend fun updateHabit(habit: Habit) {
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index >= 0) {
            habits[index] = habit
            pushHabits()
        }
    }

    override suspend fun deleteHabit(habitId: Long) {
        habits.removeAll { it.id == habitId }
        pushHabits()
    }

    override suspend fun archiveHabit(habitId: Long) {
        val index = habits.indexOfFirst { it.id == habitId }
        if (index >= 0) {
            habits[index] = habits[index].copy(isArchived = true)
            pushHabits()
        }
    }

    override suspend fun countActiveHabits(): Int = habits.count { !it.isArchived }

    override suspend fun countReminderEnabledHabits(): Int {
        return habits.count { !it.isArchived && it.reminderEnabled }
    }

    override suspend fun getHabitsWithReminders(): List<Habit> {
        return habits.filter { !it.isArchived && it.reminderEnabled }
    }

    override suspend fun upsertCompletion(completion: HabitCompletion) {
        completions.removeAll { it.habitId == completion.habitId && it.epochDay == completion.epochDay }
        completions.add(completion)
        pushCompletions()
    }

    override suspend fun deleteCompletion(habitId: Long, epochDay: Long) {
        completions.removeAll { it.habitId == habitId && it.epochDay == epochDay }
        pushCompletions()
    }

    override suspend fun deleteCompletionsForHabit(habitId: Long) {
        completions.removeAll { it.habitId == habitId }
        pushCompletions()
    }

    override fun observeCompletedHabitIdsForDay(epochDay: Long): Flow<List<Long>> {
        return completionsFlow.map { list ->
            list.filter { it.epochDay == epochDay }.map { it.habitId }
        }
    }

    override fun observeCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> {
        return completionsFlow.map { list ->
            list.filter { it.habitId == habitId }.sortedByDescending { it.epochDay }
        }
    }

    override suspend fun getCompletionEpochDaysForHabit(habitId: Long): List<Long> {
        return completions.filter { it.habitId == habitId }.map { it.epochDay }.sortedDescending()
    }

    override fun observeCompletedCountForDay(epochDay: Long): Flow<Int> {
        return completionsFlow.map { list -> list.count { it.epochDay == epochDay } }
    }

    override suspend fun getCompletedCountForDay(epochDay: Long): Int {
        return completions.count { it.epochDay == epochDay }
    }

    override fun observeCompletionCountsBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<DayCompletionCount>> {
        return completionsFlow.map { list ->
            list.filter { it.epochDay in fromEpochDay..toEpochDay }
                .groupBy { it.epochDay }
                .map { (day, dayCompletions) ->
                    DayCompletionCount(epochDay = day, completedCount = dayCompletions.size)
                }
                .sortedBy { it.epochDay }
        }
    }

    override fun observeCompletionTotalsByHabit(
        fromEpochDay: Long,
        toEpochDay: Long
    ): Flow<List<HabitCompletionTotal>> {
        return completionsFlow.map { list ->
            list.filter { it.epochDay in fromEpochDay..toEpochDay }
                .groupBy { it.habitId }
                .map { (habitId, habitCompletions) ->
                    HabitCompletionTotal(habitId = habitId, completedCount = habitCompletions.size)
                }
        }
    }

    private fun pushHabits() {
        habitsFlow.value = habits.toList()
    }

    private fun pushCompletions() {
        completionsFlow.value = completions.toList()
    }
}
