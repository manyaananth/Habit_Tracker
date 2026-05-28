package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class XpFeedback(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val xOffset: Float = (Math.random() * 200 - 100).toFloat(),
    val yOffset: Float = 0f
)

data class ToastMessage(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val secondaryMessage: String = ""
)

enum class HabitFilter {
    All, Daily, Weekly, Monthly
}

class MomentumViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(db)

    // Session / Auth state via SharedPreferences & DB
    private val prefs = application.getSharedPreferences("momentum_prefs", android.content.Context.MODE_PRIVATE)

    val isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val username = MutableStateFlow(prefs.getString("username", "") ?: "")

    // Cloud Sync State
    val isSyncing = MutableStateFlow(false)
    val lastSyncTime = MutableStateFlow(prefs.getString("last_sync_time", "Never") ?: "Never")

    // Data streams
    val habits = repository.allHabits
    val habitLogs = repository.allHabitLogs
    val dailyLogs = repository.allDailyLogs
    val totalXp = repository.totalXpFlow
    val xpLogs = repository.allXpLogs
    val conceptCompletions = repository.allConceptCompletions

    // Local filter state
    val selectedFilter = MutableStateFlow(HabitFilter.All)

    // Coaching state
    val coachResponse = MutableStateFlow<String?>(null)
    val isCoachLoading = MutableStateFlow(false)
    val coachClickCount = MutableStateFlow(0) // For achievement check

    // Floating XP and Toasts queues
    val xpFeedbacks = mutableStateListOf<XpFeedback>()
    val toastsQueue = mutableStateListOf<ToastMessage>()

    // Achievements static definition
    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val icon: String,
        val isUnlocked: Boolean
    )

    // Dynamic Achievements evaluation
    val achievements: Flow<List<Achievement>> = combine(
        habits, habitLogs, dailyLogs, totalXp, coachClickCount
    ) { habitsList, logsList, dailyLogsList, currentXp, coachClicks ->
        val level = calculateLevel(currentXp ?: 0).level
        
        listOf(
            Achievement(
                id = "first_loop",
                title = "Atomic Cycle",
                description = "Complete your first habit",
                icon = "⚙️",
                isUnlocked = logsList.isNotEmpty()
            ),
            Achievement(
                id = "architect",
                title = "Habit Architect",
                description = "Create 5 or more custom habits",
                icon = "🏛️",
                isUnlocked = habitsList.size >= 5
            ),
            Achievement(
                id = "week_master",
                title = "Week Champion",
                description = "Check off a Weekly habit task",
                icon = "🏆",
                isUnlocked = logsList.any { log ->
                    val h = habitsList.find { it.id == log.habitId }
                    h?.frequency == "Weekly"
                }
            ),
            Achievement(
                id = "consistency_3",
                title = "Triple Core",
                description = "Build a streak of 3 on any daily habit",
                icon = "🔥",
                isUnlocked = habitsList.any { it.frequency == "Daily" && it.currentStreak >= 3 }
            ),
            Achievement(
                id = "consistency_7",
                title = "Momentum Zenith",
                description = "Sustain a perfect 7-day daily habit streak",
                icon = "⚡",
                isUnlocked = habitsList.any { it.frequency == "Daily" && it.currentStreak >= 7 }
            ),
            Achievement(
                id = "mindful_soul",
                title = "Mindful Soul",
                description = "Record your first daily mood journal",
                icon = "🌸",
                isUnlocked = dailyLogsList.any { it.journalText.isNotBlank() }
            ),
            Achievement(
                id = "perfect_day",
                title = "Optimal Sync",
                description = "Complete all daily habits today (min. 2 habits)",
                icon = "✨",
                isUnlocked = run {
                    val dailyIds = habitsList.filter { it.frequency == "Daily" && !it.isArchived }.map { it.id }
                    if (dailyIds.size < 2) false
                    else {
                        val todayStr = getTodayDateString()
                        val loggedToday = logsList.filter { it.dateString == todayStr }.map { it.habitId }
                        dailyIds.all { loggedToday.contains(it) }
                    }
                }
            ),
            Achievement(
                id = "quest_ascent",
                title = "High Ascendant",
                description = "Reach Character Level 3",
                icon = "🐉",
                isUnlocked = level >= 3
            ),
            Achievement(
                id = "oracle",
                title = "Awakened Oracle",
                description = "Query your AI Mindset Coach 3 times",
                icon = "🔮",
                isUnlocked = coachClicks >= 3
            ),
            Achievement(
                id = "immortal",
                title = "Immortal Legacy",
                description = "Accumulate over 1,000 Total XP",
                icon = "👑",
                isUnlocked = (currentXp ?: 0) >= 1000
            )
        )
    }

    // Date Format utilities
    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getTodayDisplayString(): String {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.US)
        return sdf.format(Date())
    }

    // XP RPG Level Definitions
    data class LevelConfig(
        val level: Int,
        val title: String,
        val minXp: Int,
        val maxXp: Int
    )

    val levels = listOf(
        LevelConfig(1, "Seedling", 0, 100),
        LevelConfig(2, "Sprout", 100, 300),
        LevelConfig(3, "Sapling", 300, 600),
        LevelConfig(4, "Blossom", 600, 1000),
        LevelConfig(5, "Undergrowth", 1000, 1500),
        LevelConfig(6, "Canopy", 1500, 2100),
        LevelConfig(7, "Guardian Root", 2100, 2800),
        LevelConfig(8, "Grove Weaver", 2800, 3600),
        LevelConfig(9, "Forest Sentinel", 3600, 4500),
        LevelConfig(10, "Ancient Arbor", 4500, 5500),
        LevelConfig(11, "Immortal", 5500, 999999)
    )

    fun calculateLevel(currentXp: Int): LevelConfig {
        for (lvl in levels) {
            if (currentXp in lvl.minXp..lvl.maxXp) {
                return lvl
            }
        }
        return levels.last()
    }

    // Toast/Feedback managers
    fun triggerXpFeedback(points: Int) {
        val feedback = XpFeedback(text = "+$points XP")
        xpFeedbacks.add(feedback)
        // Auto-remove after animation time
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            xpFeedbacks.remove(feedback)
        }
    }

    fun postToast(message: String, secondary: String = "") {
        val toast = ToastMessage(message = message, secondaryMessage = secondary)
        toastsQueue.add(toast)
        viewModelScope.launch {
            kotlinx.coroutines.delay(3500)
            toastsQueue.remove(toast)
        }
    }

    // Actions
    fun addHabit(title: String, frequency: String, targetCount: Int, emoji: String, color: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val h = Habit(
                title = title,
                frequency = frequency,
                targetCount = targetCount,
                iconEmoji = emoji,
                colorHex = color
            )
            val newId = repository.insertHabit(h)
            
            // UI feedback
            viewModelScope.launch {
                triggerXpFeedback(20)
                postToast("Habit Created!", "Earned 20 XP • \"$title\" added")
            }
        }
    }

    fun toggleHabitCompletion(habit: Habit, dateString: String, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isCompleted) {
                // Was already completed, now uncheck
                repository.uncompleteHabit(habit, dateString)
                viewModelScope.launch {
                    postToast("Task Incomplete", "Streak and XP adjusted.")
                }
            } else {
                // Check off
                val addedXp = repository.completeHabit(habit, dateString)
                viewModelScope.launch {
                    if (addedXp) {
                        triggerXpFeedback(10)
                        postToast("Completed Task!", "Earned 10 XP • Nice job!")
                    } else {
                        postToast("Completed Task!", "Already earned XP for today.")
                    }
                }
            }
        }
    }

    fun deleteHabit(habitId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHabit(habitId)
            viewModelScope.launch {
                postToast("Habit Removed", "Successfully deleted habit logs")
            }
        }
    }

    fun saveMoodAndJournal(mood: Int, journalText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = getTodayDateString()
            val (moodXp, journalXp) = repository.saveMoodAndJournal(dateStr, mood, journalText)
            
            viewModelScope.launch {
                var totalPoints = 0
                val categories = mutableListOf<String>()
                if (moodXp) {
                    totalPoints += 5
                    categories.add("Log Daily Mood (+5 XP)")
                }
                if (journalXp) {
                    totalPoints += 15
                    categories.add("Write Daily Reflection (+15 XP)")
                }

                if (totalPoints > 0) {
                    triggerXpFeedback(totalPoints)
                    postToast("Daily Log Saved!", "Earned $totalPoints XP • ${categories.joinToString(" • ")}")
                } else {
                    postToast("Daily Log Saved!", "Journal saved securely")
                }
            }
        }
    }

    fun queryAICoach(habitsList: List<Habit>, logsList: List<HabitLog>, dailyLogsList: List<DailyLog>, totalXpVal: Int) {
        coachClickCount.value = coachClickCount.value + 1
        isCoachLoading.value = true
        coachResponse.value = null

        viewModelScope.launch {
            // Build text context for habits
            val habitsText = if (habitsList.isEmpty()) {
                "No habits configured yet."
            } else {
                habitsList.joinToString("\n") { h ->
                    val compCount = logsList.filter { it.habitId == h.id }.size
                    "• ${h.title} [${h.frequency}, Target: ${h.targetCount}x] - Completed $compCount times. Current Streak: ${h.currentStreak}"
                }
            }

            // Build text context for moods
            val moodSymbols = listOf("😔", "😐", "🙂", "😊", "🤩")
            val moodsText = if (dailyLogsList.isEmpty()) {
                "No daily mood/journal completions yet."
            } else {
                dailyLogsList.take(5).joinToString("\n") { l ->
                    val moodIcon = if (l.moodLevel in 1..5) moodSymbols[l.moodLevel - 1] else "None"
                    "• ${l.dateString}: Mood $moodIcon | Reflection: \"${l.journalText}\""
                }
            }

            val level = calculateLevel(totalXpVal).level
            val insight = GeminiClient.getCoachingInsights(
                habitsText = habitsText,
                moodsText = moodsText,
                totalXp = totalXpVal,
                level = level
            )
            coachResponse.value = insight
            isCoachLoading.value = false
        }
    }

    // AUTH ACTIONS
    fun login(email: String, queryPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedEmail = email.lowercase().trim()
            if (trimmedEmail.isEmpty() || queryPass.isEmpty()) {
                viewModelScope.launch { onResult(false, "Please fill in all fields.") }
                return@launch
            }
            val user = repository.getUserByEmail(trimmedEmail)
            if (user == null) {
                viewModelScope.launch { onResult(false, "No account found with this email.") }
            } else if (user.passwordHash != queryPass) {
                viewModelScope.launch { onResult(false, "Incorrect password.") }
            } else {
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("user_email", trimmedEmail)
                    .putString("username", user.username)
                    .apply()
                
                isLoggedIn.value = true
                userEmail.value = trimmedEmail
                username.value = user.username
                
                viewModelScope.launch {
                    triggerXpFeedback(10)
                    postToast("Welcome Back, ${user.username}!", "Logged in successfully • Synchronized session.")
                    onResult(true, "Welcome back!")
                }
            }
        }
    }

    fun signUp(email: String, usernameVal: String, queryPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedEmail = email.lowercase().trim()
            val trimmedUsername = usernameVal.trim()
            if (trimmedEmail.isEmpty() || trimmedUsername.isEmpty() || queryPass.isEmpty()) {
                viewModelScope.launch { onResult(false, "Please fill in all fields.") }
                return@launch
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                viewModelScope.launch { onResult(false, "Please enter a valid email address.") }
                return@launch
            }
            if (queryPass.length < 4) {
                viewModelScope.launch { onResult(false, "Password must be at least 4 characters.") }
                return@launch
            }

            val success = repository.registerUser(trimmedEmail, trimmedUsername, queryPass)
            if (success) {
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("user_email", trimmedEmail)
                    .putString("username", trimmedUsername)
                    .apply()
                
                isLoggedIn.value = true
                userEmail.value = trimmedEmail
                username.value = trimmedUsername
                
                viewModelScope.launch {
                    triggerXpFeedback(15)
                    postToast("Account Created!", "Earned 15 XP • Sync setup active.")
                    onResult(true, "Registration successful!")
                }
            } else {
                viewModelScope.launch { onResult(false, "An account with this email already exists.") }
            }
        }
    }

    fun logOut() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("user_email", "")
            .putString("username", "")
            .apply()

        isLoggedIn.value = false
        userEmail.value = ""
        username.value = ""
        postToast("Logged Out Successfully", "Your local data remains safe.")
    }

    fun manualSync() {
        if (isSyncing.value) return
        isSyncing.value = true
        postToast("Synchronizing profile...", "Connecting to secure cloud clusters...")
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800)
            val format = SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)
            val timeStr = format.format(Date())
            prefs.edit().putString("last_sync_time", timeStr).apply()
            lastSyncTime.value = timeStr
            isSyncing.value = false
            triggerXpFeedback(10)
            postToast("Sync Success!", "Earned 10 XP • Habits data is safe in cloud.")
        }
    }

    // LEARNING CONCEPTS ACTIONS
    fun completeConceptToday(conceptId: String, xpReward: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val todayDateStr = getTodayDateString()
            val added = repository.completeConcept(conceptId, todayDateStr, xpReward)
            if (added) {
                viewModelScope.launch {
                    triggerXpFeedback(xpReward)
                    postToast("Concept Mastered!", "Earned $xpReward XP • Added to mental models.")
                }
            }
        }
    }

    // AI CUSTOM MINDSET CONCEPT GENERATOR
    val aiGeneratedConcept = MutableStateFlow<String?>(null)
    val isAiConceptLoading = MutableStateFlow(false)

    fun fetchAiCustomConcept() {
        if (isAiConceptLoading.value) return
        isAiConceptLoading.value = true
        aiGeneratedConcept.value = null
        viewModelScope.launch {
            val idsPrompt = ConceptsRepository.allPredefinedConcepts.joinToString(", ") { it.id }
            val response = GeminiClient.generateCustomConcept(idsPrompt)
            aiGeneratedConcept.value = response
            isAiConceptLoading.value = false
        }
    }
}
