package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val frequency: String, // "Daily", "Weekly", "Monthly"
    val targetCount: Int,  // 1 for Daily, 1, 2, 3 for Weekly, 1, 2, 4 for Monthly
    val iconEmoji: String,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val currentStreak: Int = 0,
    val lastCompletedDate: String = "" // "yyyy-MM-dd" to help calculate streak
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val dateString: String, // "yyyy-MM-dd"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val dateString: String, // "yyyy-MM-dd"
    val moodLevel: Int, // 1 to 5: 😔 = 1, 😐 = 2, 🙂 = 3, 😊 = 4, 🤩 = 5
    val journalText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "xp_logs")
data class XpLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "HABIT_COMPLETE", "HABIT_ADD", "MOOD_LOG", "JOURNAL_WRITE"
    val points: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val referenceId: String = "" // To prevent duplicates (e.g. "HA_id" or "ML_dateString" or "JW_dateString" or "HC_habitId_dateString")
)

@Entity(tableName = "concept_completions")
data class ConceptCompletion(
    @PrimaryKey val compositeKey: String, // "yyyy-MM-dd_conceptId"
    val conceptId: String,
    val dateString: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val username: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)
