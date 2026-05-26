package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Repository(private val db: AppDatabase) {

    private val habitDao = db.habitDao()
    private val dailyLogDao = db.dailyLogDao()
    private val xpLogDao = db.xpLogDao()

    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allHabitLogs: Flow<List<HabitLog>> = habitDao.getAllHabitLogs()
    val allDailyLogs: Flow<List<DailyLog>> = dailyLogDao.getAllDailyLogs()
    
    val totalXpFlow: Flow<Int> = xpLogDao.getTotalXpFlow().map { it ?: 0 }
    val allXpLogs: Flow<List<XpLog>> = xpLogDao.getAllXpLogs()

    suspend fun insertHabit(habit: Habit): Int {
        val id = habitDao.insertHabit(habit).toInt()
        // Award XP for creating a habit
        addXp("HABIT_ADD", 20, "HA_$id")
        return id
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habitId: Int) {
        habitDao.deleteHabitById(habitId)
        habitDao.deleteLogsByHabitId(habitId)
    }

    suspend fun completeHabit(habit: Habit, dateString: String): Boolean {
        // Insert completion log
        val log = HabitLog(habitId = habit.id, dateString = dateString)
        habitDao.insertLog(log)

        // Calculate and update the habit's streak if it is a Daily habit
        if (habit.frequency == "Daily") {
            val updatedStreak = calculateNewStreakAfterCompletion(habit, dateString)
            habitDao.updateHabit(habit.copy(
                currentStreak = updatedStreak,
                lastCompletedDate = dateString
            ))
        }

        // Award XP for completing a habit (+10 XP)
        // Reference is HAB_COMP_habitId_dateString
        val referenceId = "HC_${habit.id}_$dateString"
        return addXp("HABIT_COMPLETE", 10, referenceId)
    }

    suspend fun uncompleteHabit(habit: Habit, dateString: String) {
        // Remove completion log
        habitDao.deleteLogByHabitAndDate(habit.id, dateString)

        // Decrement/Reset streak if Daily habit
        if (habit.frequency == "Daily") {
            // Recompute streak based on remaining completion logs
            // For safety we can reset or compute, let's keep it simple:
            val prevStreak = (habit.currentStreak - 1).coerceAtLeast(0)
            habitDao.updateHabit(habit.copy(
                currentStreak = prevStreak,
                lastCompletedDate = "" // Simple fallback, ViewModel will re-evaluate
            ))
        }
    }

    suspend fun saveMoodAndJournal(dateString: String, mood: Int, journal: String): Pair<Boolean, Boolean> {
        val existing = dailyLogDao.getDailyLogByDate(dateString)
        val dailyLog = DailyLog(dateString = dateString, moodLevel = mood, journalText = journal)
        dailyLogDao.insertDailyLog(dailyLog)

        var earnedMoodXp = false
        var earnedJournalXp = false

        // Award XP for Mood logging (+5 XP)
        if (existing == null || existing.moodLevel == 0) {
            earnedMoodXp = addXp("MOOD_LOG", 5, "ML_$dateString")
        }

        // Award XP for Journal writing (+15 XP)
        if (journal.isNotBlank() && (existing == null || existing.journalText.isBlank())) {
            earnedJournalXp = addXp("JOURNAL_WRITE", 15, "JW_$dateString")
        }

        return Pair(earnedMoodXp, earnedJournalXp)
    }

    suspend fun addXp(category: String, points: Int, referenceId: String): Boolean {
        if (xpLogDao.hasXpLogWithRef(referenceId)) return false
        val log = XpLog(category = category, points = points, referenceId = referenceId)
        val result = xpLogDao.insertXpLog(log)
        return result != -1L
    }

    private fun calculateNewStreakAfterCompletion(habit: Habit, completedDateStr: String): Int {
        // Simple logic for streak increment:
        // If lastCompletedDate was yesterday, streak increments.
        // If yesterday was NOT completed, streak resets to 1 (new start).
        // If lastCompletedDate is today (it shouldn't be completed twice in a day, but safe-check), keep streak.
        val lastDate = habit.lastCompletedDate
        if (lastDate.isEmpty()) return 1
        
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val last = format.parse(lastDate)
            val current = format.parse(completedDateStr)
            if (last != null && current != null) {
                val diffMs = current.time - last.time
                val diffDays = diffMs / (1000 * 60 * 60 * 24)
                if (diffDays <= 1L) {
                    habit.currentStreak + 1
                } else {
                    1
                }
            } else {
                1
            }
        } catch (e: Exception) {
            1
        }
    }
}
