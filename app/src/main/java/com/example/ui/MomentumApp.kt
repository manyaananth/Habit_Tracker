package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DailyLog
import com.example.data.Habit
import com.example.data.HabitLog
import java.text.SimpleDateFormat
import java.util.*

enum class MomentumTab {
    Today, Stats, Mood, Coach, Awards
}

@Composable
fun MomentumApp(viewModel: MomentumViewModel = viewModel()) {
    var currentTab by remember { mutableStateOf(MomentumTab.Today) }
    
    // Collect data streams
    val habitsList by viewModel.habits.collectAsState(initial = emptyList())
    val habitLogsList by viewModel.habitLogs.collectAsState(initial = emptyList())
    val dailyLogsList by viewModel.dailyLogs.collectAsState(initial = emptyList())
    val totalXpVal by viewModel.totalXp.collectAsState(initial = 0)
    
    // Modals & Form States
    var showAddHabitDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0C29), Color(0xFF16213E), Color(0xFF1B1B2F))
                )
            )
    ) {
        // Glowing orbital decorations for visual depth
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .graphicsLayer(alpha = 0.45f)
        ) {
            drawCircle(
                color = Color(0xFF6366F1),
                radius = 280.dp.toPx(),
                center = Offset(size.width * 0.15f, size.height * 0.25f)
            )
            drawCircle(
                color = Color(0xFFEC4899),
                radius = 220.dp.toPx(),
                center = Offset(size.width * 0.85f, size.height * 0.65f)
            )
        }

        val isLoggedIn by viewModel.isLoggedIn.collectAsState()

        if (!isLoggedIn) {
            MomentumLoginScreen(viewModel = viewModel)
        } else {
            // Core Layout Scroll
            Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MomentumHeader(
                    totalXp = totalXpVal,
                    habitsList = habitsList,
                    logsList = habitLogsList,
                    viewModel = viewModel
                )
            },
            bottomBar = {
                MomentumBottomNavigation(
                    selectedTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            },
            floatingActionButton = {
                if (currentTab == MomentumTab.Today) {
                    FloatingActionButton(
                        onClick = { showAddHabitDialog = true },
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .testTag("add_habit_fab")
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom Habit")
                    }
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Content Switcher
                Crossfade(
                    targetState = currentTab,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        MomentumTab.Today -> TodayTabContent(
                            habits = habitsList,
                            logs = habitLogsList,
                            viewModel = viewModel
                        )
                        MomentumTab.Stats -> StatsTabContent(
                            habits = habitsList,
                            logs = habitLogsList,
                            dailyLogs = dailyLogsList,
                            totalXp = totalXpVal,
                            viewModel = viewModel
                        )
                        MomentumTab.Mood -> MoodTabContent(
                            dailyLogs = dailyLogsList,
                            viewModel = viewModel
                        )
                        MomentumTab.Coach -> CoachTabContent(
                            habits = habitsList,
                            logs = habitLogsList,
                            dailyLogs = dailyLogsList,
                            totalXp = totalXpVal,
                            viewModel = viewModel
                        )
                        MomentumTab.Awards -> AwardsTabContent(
                            totalXp = totalXpVal,
                            viewModel = viewModel
                        )
                    }
                }

                // Floating XP feedback overlays
                viewModel.xpFeedbacks.forEach { feedback ->
                    FloatingXpFeedback(feedback = feedback)
                }

                // Styled custom toasts
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    viewModel.toastsQueue.firstOrNull()?.let { toast ->
                        MomentumToast(toast = toast)
                    }
                }
            }
        }

            // Habit Add Modal
            if (showAddHabitDialog) {
                AddHabitDialog(
                    onDismiss = { showAddHabitDialog = false },
                    onAddHabit = { title, freq, count, icon, color ->
                        viewModel.addHabit(title, freq, count, icon, color)
                        showAddHabitDialog = false
                    }
                )
            }
        }
    }
}

// ======================== HEADER BLOCK ========================
@Composable
fun MomentumHeader(
    totalXp: Int,
    habitsList: List<Habit>,
    logsList: List<HabitLog>,
    viewModel: MomentumViewModel
) {
    val levelInfo = viewModel.calculateLevel(totalXp)
    val nextLevelProgress = run {
        val totalNeeded = levelInfo.maxXp - levelInfo.minXp
        val currentGained = totalXp - levelInfo.minXp
        if (totalNeeded <= 0) 1.0f else (currentGained.toFloat() / totalNeeded.toFloat()).coerceIn(0f, 1f)
    }

    // Calculating dynamic counts
    val todayDateStr = viewModel.getTodayDateString()
    
    // Daily completes
    val dailyHabits = habitsList.filter { it.frequency == "Daily" }
    val dailyCompletions = dailyHabits.filter { h ->
        logsList.any { it.habitId == h.id && it.dateString == todayDateStr }
    }.size

    // Weekly completes
    val weeklyHabits = habitsList.filter { it.frequency == "Weekly" }
    var weeklyCompletions = 0
    var weeklyTargetTotal = 0
    weeklyHabits.forEach { h ->
        weeklyTargetTotal += h.targetCount
        val logsThisWeek = logsList.filter { it.habitId == h.id && isDateInCurrentWeek(it.dateString) }.size
        weeklyCompletions += logsThisWeek.coerceAtMost(h.targetCount)
    }

    // Monthly completes
    val monthlyHabits = habitsList.filter { it.frequency == "Monthly" }
    var monthlyCompletions = 0
    var monthlyTargetTotal = 0
    monthlyHabits.forEach { h ->
        monthlyTargetTotal += h.targetCount
        val logsThisMonth = logsList.filter { it.habitId == h.id && isDateInCurrentMonth(it.dateString) }.size
        monthlyCompletions += logsThisMonth.coerceAtMost(h.targetCount)
    }

    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncText by viewModel.lastSyncTime.collectAsState()
    var showSyncDialog by remember { mutableStateOf(false) }

    if (showSyncDialog) {
        CloudProfileSyncDialog(viewModel = viewModel, onDismiss = { showSyncDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1F0B0C1E))
            .drawBehind {
                drawLine(
                    color = Color(0x1AFFFFFF),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        // App Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = { showSyncDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSyncing) Icons.Default.Refresh else Icons.Default.AccountCircle,
                        contentDescription = "Sync Cloud Status",
                        tint = if (isSyncing) Color(0xFF6366F1) else Color(0xFF22C55E),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Momentum",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 0.2.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = viewModel.getTodayDisplayString().uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // RPG Badge Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LVL ${levelInfo.level}",
                        color = Color(0xFFA5B4FC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = levelInfo.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Level Linear Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$totalXp XP",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(nextLevelProgress)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${levelInfo.maxXp} XP",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Target summaries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderMetricCard(
                label = "Daily Sync",
                done = dailyCompletions,
                target = dailyHabits.size,
                accentColor = Color(0xFF6366F1),
                modifier = Modifier.weight(1f)
            )
            HeaderMetricCard(
                label = "Weekly Goals",
                done = weeklyCompletions,
                target = weeklyTargetTotal,
                accentColor = Color(0xFFEC4899),
                modifier = Modifier.weight(1f)
            )
            HeaderMetricCard(
                label = "Monthly Goals",
                done = monthlyCompletions,
                target = monthlyTargetTotal,
                accentColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HeaderMetricCard(
    label: String,
    done: Int,
    target: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$done/$target",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (target > 0 && done >= target) accentColor else Color(0xFF475569))
                )
            }
        }
    }
}


// ======================== TAB CONTENT: TODAY ========================
@Composable
fun TodayTabContent(
    habits: List<Habit>,
    logs: List<HabitLog>,
    viewModel: MomentumViewModel
) {
    val activeFilter by viewModel.selectedFilter.collectAsState()
    val todayDateStr = viewModel.getTodayDateString()

    // Filtered lists
    val displayedHabits = habits.filter { h ->
        when (activeFilter) {
            HabitFilter.All -> true
            HabitFilter.Daily -> h.frequency == "Daily"
            HabitFilter.Weekly -> h.frequency == "Weekly"
            HabitFilter.Monthly -> h.frequency == "Monthly"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Filter Horizontal Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HabitFilter.values().forEach { filter ->
                val active = activeFilter == filter
                val displayName = when (filter) {
                    HabitFilter.All -> "All Today"
                    else -> filter.name
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (active) Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(24.dp)
                        )
                        .border(
                            1.dp,
                            if (active) Color.Transparent else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { viewModel.selectedFilter.value = filter }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = displayName,
                        color = if (active) Color.White else Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mindset Science Learning Block
            item {
                DailyLearningConceptsCard(viewModel = viewModel)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Habits Section Header
            item {
                Text(
                    text = "ACTIVE HABITS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (displayedHabits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔮", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your loop is empty.",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Build a dynamic custom habit loop below",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            } else {
                items(displayedHabits, key = { it.id }) { habit ->
                    // Find completed logs for period
                    val isChecked = logs.any { it.habitId == habit.id && it.dateString == todayDateStr }
                    
                    val periodCompletionsCount = when (habit.frequency) {
                        "Daily" -> if (isChecked) 1 else 0
                        "Weekly" -> logs.filter { it.habitId == habit.id && isDateInCurrentWeek(it.dateString) }.size
                        "Monthly" -> logs.filter { it.habitId == habit.id && isDateInCurrentMonth(it.dateString) }.size
                        else -> 0
                    }

                    HabitListItemCard(
                        habit = habit,
                        isChecked = isChecked,
                        periodCompletions = periodCompletionsCount,
                        onCheckedChange = {
                            viewModel.toggleHabitCompletion(habit, todayDateStr, isChecked)
                        },
                        onDeleteClick = {
                            viewModel.deleteHabit(habit.id)
                        },
                        logs = logs,
                        viewModel = viewModel
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB & Nav
            }
        }
    }
}

@Composable
fun HabitListItemCard(
    habit: Habit,
    isChecked: Boolean,
    periodCompletions: Int,
    onCheckedChange: () -> Unit,
    onDeleteClick: () -> Unit,
    logs: List<HabitLog>,
    viewModel: MomentumViewModel
) {
    val habitColor = run {
        try {
            Color(android.graphics.Color.parseColor(habit.colorHex))
        } catch (e: Exception) {
            Color(0xFF6366F1)
        }
    }

    // Dynamic scale and alpha transitions
    val animatedProgressFraction = run {
        if (habit.targetCount <= 0) 1.0f
        else (periodCompletions.toFloat() / habit.targetCount.toFloat()).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Details Row
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(habitColor.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = habit.iconEmoji,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = habit.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${habit.frequency} • $periodCompletions/${habit.targetCount} times",
                            color = habitColor.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Actions Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Habit",
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isChecked) Color(0xFF22C55E) else Color.Transparent)
                            .border(if (isChecked) 0.dp else 2.dp, if (isChecked) Color.Transparent else Color(0xFF475569), CircleShape)
                            .clickable { onCheckedChange() }
                            .testTag("checkbox_habit_${habit.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Task Completed",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic progress slider details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgressFraction)
                            .clip(CircleShape)
                            .background(habitColor)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(animatedProgressFraction * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
            }

            // If daily habit, show mini 7-day completes checklist
            if (habit.frequency == "Daily") {
                Spacer(modifier = Modifier.height(10.dp))
                HabitMiniDotHistory(habitId = habit.id, logs = logs, color = habitColor, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HabitMiniDotHistory(
    habitId: Int,
    logs: List<HabitLog>,
    color: Color,
    viewModel: MomentumViewModel
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val last7Days = remember {
        (0..6).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            sdf.format(cal.time)
        }.reversed()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Last 7 Days:",
            fontSize = 9.sp,
            color = Color.Gray,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            last7Days.forEach { dateStr ->
                val completed = logs.any { it.habitId == habitId && it.dateString == dateStr }
                val isToday = dateStr == viewModel.getTodayDateString()
                
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (completed) color else Color(0xFF334155)
                        )
                        .border(
                            1.dp,
                            if (isToday) Color.White else Color.Transparent,
                            CircleShape
                        )
                )
            }
        }
    }
}


// ======================== TAB CONTENT: STATS ========================
@Composable
fun StatsTabContent(
    habits: List<Habit>,
    logs: List<HabitLog>,
    dailyLogs: List<DailyLog>,
    totalXp: Int,
    viewModel: MomentumViewModel
) {
    val scrollState = rememberScrollState()

    // Aggregate Analytics
    val totalDone = logs.size
    val bestStreakInfo = habits.filter { it.frequency == "Daily" }.maxOfOrNull { it.currentStreak } ?: 0

    val weeklyGoalsMet = run {
        habits.filter { it.frequency == "Weekly" }.count { h ->
            val count = logs.filter { it.habitId == h.id && isDateInCurrentWeek(it.dateString) }.size
            count >= h.targetCount
        }
    }

    val monthlyGoalsMet = run {
        habits.filter { it.frequency == "Monthly" }.count { h ->
            val count = logs.filter { it.habitId == h.id && isDateInCurrentMonth(it.dateString) }.size
            count >= h.targetCount
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Summary Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsCard(label = "Streak Peak", value = "$bestStreakInfo d", icon = "🔥", modifier = Modifier.weight(1f))
            StatsCard(label = "Total Logs", value = "$totalDone hits", icon = "📈", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsCard(label = "Weekly Met", value = "$weeklyGoalsMet habits", icon = "🏆", modifier = Modifier.weight(1f))
            StatsCard(label = "Monthly Met", value = "$monthlyGoalsMet habits", icon = "👑", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 7-day completion chart
        Text(
            text = "7-Day Habits Checkins",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        HabitCompletionsWeeklyChart(logs = logs)

        Spacer(modifier = Modifier.height(18.dp))

        // This Week's Target Progress
        Text(
            text = "Weekly Habit Syncs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        WeeklyTargetSection(habits = habits, logs = logs)

        Spacer(modifier = Modifier.height(18.dp))

        // Contribution year heatmap
        Text(
            text = "Year Growth Heatmap",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        ContributionYearHeatmap(logs = logs)

        Spacer(modifier = Modifier.height(80.dp)) // buffer padding at bottom
    }
}

@Composable
fun StatsCard(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, fontSize = 11.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                Text(text = icon, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun HabitCompletionsWeeklyChart(logs: List<HabitLog>) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayLabelSdf = SimpleDateFormat("EEE", Locale.US)

    // Last 7 days counting
    val last7WeekEntries = remember(logs) {
        (0..6).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val code = sdf.format(cal.time)
            val label = dayLabelSdf.format(cal.time)
            val hitsCount = logs.count { it.dateString == code }
            Pair(label, hitsCount)
        }.reversed()
    }

    val maxHits = last7WeekEntries.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            last7WeekEntries.forEach { entry ->
                val fraction = entry.second.toFloat() / maxHits.toFloat()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.85f)
                            .width(16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Empty pillar track bg
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                        // Active colored pillar fill
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFEC4899), Color(0xFF6366F1))
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = entry.second.toString(), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = entry.first, fontSize = 9.sp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun WeeklyTargetSection(habits: List<Habit>, logs: List<HabitLog>) {
    val weeklyHabits = habits.filter { h -> h.frequency == "Weekly" }

    if (weeklyHabits.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No Weekly Habits configured yet", color = Color.Gray, fontSize = 12.sp)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            weeklyHabits.forEach { habit ->
                val logsThisWeek = logs.filter { it.habitId == habit.id && isDateInCurrentWeek(it.dateString) }.size
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = habit.iconEmoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = habit.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Done $logsThisWeek of ${habit.targetCount}x",
                            color = Color.LightGray,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Dot Checkboxes Row representing targeted limits
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (0 until habit.targetCount).forEach { index ->
                            val active = index < logsThisWeek
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Color(0xFF6366F1) else Color(0x22FFFFFF))
                                    .border(1.dp, Color(0xFF6366F1), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContributionYearHeatmap(logs: List<HabitLog>) {
    val columnsCount = 20 // 20 calendar columns (~140 days)
    val totalDays = columnsCount * 7
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Calculate grid timestamps
    val dates = remember(logs) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        // We backtrack totalDays days behind
        (0 until totalDays).map { i ->
            val tempCal = Calendar.getInstance()
            tempCal.firstDayOfWeek = Calendar.MONDAY
            tempCal.add(Calendar.DAY_OF_YEAR, - (totalDays - 1 - i))
            sdf.format(tempCal.time)
        }
    }

    // Chunk into list of columns (7 entries per column)
    val weeksColumns = dates.chunked(7)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(weeksColumns) { weekDates ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekDates.forEach { dateCode ->
                            val completionsCount = logs.count { it.dateString == dateCode }
                            
                            val cellColor = when {
                                completionsCount == 0 -> Color.White.copy(alpha = 0.07f)
                                completionsCount == 1 -> Color(0xFF403B8C).copy(alpha = 0.65f)
                                completionsCount == 2 -> Color(0xFF6366F1)
                                completionsCount == 3 -> Color(0xFF8B5CF6)
                                else -> Color(0xFFEC4899)
                            }

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Graph legend keys
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = "Less", fontSize = 8.sp, color = Color.Gray)
                listOf(
                    Color.White.copy(alpha = 0.07f),
                    Color(0xFF403B8C).copy(alpha = 0.65f),
                    Color(0xFF6366F1),
                    Color(0xFF8B5CF6),
                    Color(0xFFEC4899)
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
                Text(text = "More", fontSize = 8.sp, color = Color.Gray)
            }
        }
    }
}


// ======================== TAB CONTENT: MOOD ========================
@Composable
fun MoodTabContent(
    dailyLogs: List<DailyLog>,
    viewModel: MomentumViewModel
) {
    val todayDateStr = viewModel.getTodayDateString()
    
    // Fetch today's logging info
    val todaysLog = dailyLogs.find { it.dateString == todayDateStr }
    var selectedMood by remember(todaysLog) { mutableStateOf(todaysLog?.moodLevel ?: 0) }
    var journalTextState by remember(todaysLog) { mutableStateOf(todaysLog?.journalText ?: "") }

    val moodEmojis = listOf("😔", "😐", "🙂", "😊", "🤩")
    val moodColors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFEC4899))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Today Mood Card Setup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x12FFFFFF), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Reflections & Energy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Log daily energy and write state reflections to earn up to +20 XP!",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mood selector dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    moodEmojis.forEachIndexed { index, emoji ->
                        val moodScaleCode = index + 1
                        val active = selectedMood == moodScaleCode
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedMood = moodScaleCode }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (active) moodColors[index].copy(alpha = 0.25f) else Color(0x0CFFFFFF))
                                    .border(
                                        2.dp,
                                        if (active) moodColors[index] else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reflection Input text box
                OutlinedTextField(
                    value = journalTextState,
                    onValueChange = { journalTextState = it },
                    placeholder = { Text("Pen your thoughts or bullet points ...", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("reflection_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.07f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.13f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.saveMoodAndJournal(selectedMood, journalTextState)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_mood_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save State Reflection", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 14-day Mood trend history block
        Text(
            text = "14-Day Mood Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        MoodMetricsChart(dailyLogs = dailyLogs)

        Spacer(modifier = Modifier.height(100.dp)) // bottom overflow space
    }
}

@Composable
fun MoodMetricsChart(dailyLogs: List<DailyLog>) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val shortSdf = SimpleDateFormat("dd", Locale.US)

    // Calculate dates of last 14 days
    val last14MoodEntries = remember(dailyLogs) {
        (0..13).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateCode = sdf.format(cal.time)
            val shortDay = shortSdf.format(cal.time)
            val matchedLog = dailyLogs.find { it.dateString == dateCode }
            Pair(shortDay, matchedLog?.moodLevel ?: 0)
        }.reversed()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            last14MoodEntries.forEach { entry ->
                // fraction scale (0 to 5 max)
                val fraction = if (entry.second <= 0) 0.05f else entry.second.toFloat() / 5.0f
                val pillarColor = when (entry.second) {
                    1 -> Color(0xFFEF4444)
                    2 -> Color(0xFFF59E0B)
                    3 -> Color(0xFF3B82F6)
                    4 -> Color(0xFF10B981)
                    5 -> Color(0xFFEC4899)
                    else -> Color.White.copy(alpha = 0.07f)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.8f)
                            .width(8.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(fraction)
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(pillarColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = if (entry.second > 0) entry.second.toString() else "•", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = entry.first, fontSize = 8.sp, color = Color.Gray)
                }
            }
        }
    }
}


// ======================== TAB CONTENT: COACH ========================
@Composable
fun CoachTabContent(
    habits: List<Habit>,
    logs: List<HabitLog>,
    dailyLogs: List<DailyLog>,
    totalXp: Int,
    viewModel: MomentumViewModel
) {
    val aiResponse by viewModel.coachResponse.collectAsState()
    val isCoachingLoading by viewModel.isCoachLoading.collectAsState()

    val context = LocalContext.current

    // Coaching science tips static library
    val scienceTips = listOf(
        Pair("The 2-Minute Scale", "Shrink your habit sizes until they take under 120 seconds. Master consistency before dialing intensity."),
        Pair("Anchor Habit Stack", "Link fresh routines directly behind steady activities: 'After I complete Coffee [Cue], I will write 1 line [Routine]'."),
        Pair("Environmental Architect", "Make triggers massive and clear. Remove triggers for items you wish to cease."),
        Pair("The Streak Anchor", "Missing once is an accident. Missing consecutive cycles breeds a fresh counter-routine loop. Shield the chain."),
        Pair("Identity Shift Loop", "Prioritize the avatar you seek to evolve. Frame routines around 'I am a writer' rather than writing targets.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // AI Coach Button & Console Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x12FFFFFF), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🧠", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Momentum Mindset Coach",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "AI customized advice calibrated from current tracking data",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.queryAICoach(habits, logs, dailyLogs, totalXp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ai_coach_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEC4899)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isCoachingLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reading progressions...", color = Color.White)
                    } else {
                        Text("Query AI Mentor", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Coach Advice display container
                aiResponse?.let { responseText ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x1CFFFFFF), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🛡️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Coach Recommendation:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEC4899)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = responseText,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Habit Science Guidelines Header
        Text(
            text = "Behavourial Science Loops",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Apply atomic coaching strategies to supercharge streaks",
            fontSize = 11.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(10.dp))

        // List of science tip structures
        scienceTips.forEach { tip ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .background(Color(0x0CFFFFFF), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Guideline Symbol",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tip.first,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tip.second,
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // safety overflow values
    }
}


// ======================== TAB CONTENT: AWARDS ========================
@Composable
fun AwardsTabContent(
    totalXp: Int,
    viewModel: MomentumViewModel
) {
    val levelInfo = viewModel.calculateLevel(totalXp)
    val achievementsList by viewModel.achievements.collectAsState(initial = emptyList())

    val countUnlocked = achievementsList.count { it.isUnlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Quest Level Progress Overview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x2EEC4899), Color(0x02FFFFFF)),
                        center = Offset(200f, 100f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .background(Color(0x12FFFFFF), RoundedCornerShape(20.dp))
                .border(2.dp, Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899))), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "👑", fontSize = 42.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LEVEL ${levelInfo.level} AVATAR",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = levelInfo.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Numerical progression text
                val textProgression = if (levelInfo.level == 11) "Maxed Core" else "$totalXp / ${levelInfo.maxXp} XP"
                Text(text = textProgression, fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(6.dp))

                // Styled level percentage bar
                val percentProgress = run {
                    val poolRange = levelInfo.maxXp - levelInfo.minXp
                    val earnedInPool = totalXp - levelInfo.minXp
                    if (poolRange <= 0) 1f else (earnedInPool.toFloat() / poolRange.toFloat()).coerceIn(0f, 1f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentProgress)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899))))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Unlocked $countUnlocked of 10 Master achievements",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // XP Reward Guide Section
        Text(
            text = "Character XP Matrix",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EarnXpRow(action = "Check off Habit Logs", reward = "+10 XP", color = Color(0xFF6366F1))
                EarnXpRow(action = "Construct Custom Habits", reward = "+20 XP", color = Color(0xFFEC4899))
                EarnXpRow(action = "Log Energy Mood Scales", reward = "+5 XP", color = Color(0xFFF59E0B))
                EarnXpRow(action = "Pen Diary State Reflections", reward = "+15 XP", color = Color(0xFF10B981))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Achievement badge list
        Text(
            text = " RPG Achievements",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        achievementsList.forEach { ach ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        if (ach.isUnlocked) Color(0x1CFFFFFF) else Color(0x07FFFFFF),
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        1.dp,
                        if (ach.isUnlocked) Color(0x2EFFFFFF) else Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dim down emoji symbols for locked elements
                val opacityScale = if (ach.isUnlocked) 1.0f else 0.35f
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (ach.isUnlocked) Color(0x1E6366F1) else Color(0x0CFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ach.icon,
                        fontSize = 20.sp,
                        modifier = Modifier.graphicsLayer(alpha = opacityScale)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ach.title,
                        color = if (ach.isUnlocked) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = ach.description,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Active status indicators
                if (ach.isUnlocked) {
                    Text(
                        text = "UNLOCKED",
                        fontSize = 9.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Quest Locked",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // safe scroll heights
    }
}

@Composable
fun EarnXpRow(action: String, reward: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = action, fontSize = 11.sp, color = Color.LightGray)
        }
        Text(text = reward, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color)
    }
}


// ======================== SUB DIALOGS: ADD HABIT FORM ========================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onAddHabit: (title: String, freq: String, count: Int, icon: String, color: String) -> Unit
) {
    var titleState by remember { mutableStateOf("") }
    var selectedFreq by remember { mutableStateOf("Daily") }
    var selectedCount by remember { mutableStateOf(1) }

    // Emojis (25 total specified)
    val emojiLibrary = listOf(
        "🎯", "🏋️", "💧", "🧠", "📚", "🥗", "🧘", "🚶", "🏃", "🏊",
        "🚴", "🛌", "🍎", "🥦", "☕", "🍵", "🎨", "🎸", "💻", "✍️",
        "💼", "🧹", "💰", "🌿", "⭐"
    )
    var selectedEmoji by remember { mutableStateOf("🎯") }

    // Color options (10 total specified)
    val basePaletteHexValues = listOf(
        "#6366F1", "#EC4899", "#10B981", "#F59E0B", "#EF4444",
        "#3B82F6", "#8B5CF6", "#14B8A6", "#F43F5E", "#06B6D4"
    )
    var selectedColorCode by remember { mutableStateOf("#6366F1") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161E35), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CONSTRUCT LOOP",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )

                // Title TextInput
                OutlinedTextField(
                    value = titleState,
                    onValueChange = { titleState = it },
                    label = { Text("Habit Title", color = Color.Gray, fontSize = 11.sp) },
                    placeholder = { Text("e.g. Meditate daily", color = Color.Gray, fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_title_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                // Frequency picker
                Text(text = "Repeat Interval:", fontSize = 11.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Daily", "Weekly", "Monthly").forEach { freq ->
                        val isSel = selectedFreq == freq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSel) Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .border(1.dp, if (isSel) Color.Transparent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedFreq = freq
                                    selectedCount = 1 // Reset count adjustments
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = freq, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Target multiplier counts selection (weekly/monthly)
                if (selectedFreq != "Daily") {
                    val availableMultipliers = if (selectedFreq == "Weekly") listOf(1, 2, 3) else listOf(1, 2, 4)
                    
                    Text(text = "Completions per block Target:", fontSize = 11.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableMultipliers.forEach { multiplier ->
                            val isSel = selectedCount == multiplier
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) Color(0xFFEC4899) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .border(1.dp, if (isSel) Color.Transparent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { selectedCount = multiplier }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "${multiplier}x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Emoji picker grid
                Text(text = "Emblem Emoji:", fontSize = 11.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .verticalScroll(rememberScrollState()),
                    maxItemsInEachRow = 6,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    emojiLibrary.forEach { emoji ->
                        val isSel = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSel) Color(0xFF6366F1).copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (isSel) Color(0xFF6366F1) else Color.Transparent, CircleShape)
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }

                // Color picker
                Text(text = "Hex Color Node:", fontSize = 11.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    basePaletteHexValues.forEach { colorString ->
                        val isSel = selectedColorCode == colorString
                        val nodeColor = remember { Color(android.graphics.Color.parseColor(colorString)) }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(nodeColor)
                                .border(
                                    2.dp,
                                    if (isSel) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColorCode = colorString }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Create and Cancel button rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Abate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (titleState.isNotBlank()) {
                                onAddHabit(titleState, selectedFreq, selectedCount, selectedEmoji, selectedColorCode)
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_habit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Cycle", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}


// ======================== STYLED SUB-ASSETS: TOASTS, XP FLOATING ========================
@Composable
fun MomentumToast(toast: ToastMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .background(Color(0xFF1B1B35).copy(alpha = 0.95f), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFEC4899).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AEC4899)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Notification Icon",
                    tint = Color(0xFFEC4899),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = toast.message,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                if (toast.secondaryMessage.isNotEmpty()) {
                    Text(
                        text = toast.secondaryMessage,
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingXpFeedback(feedback: XpFeedback) {
    var itemOffset by remember { mutableStateOf(0f) }
    var itemAlpha by remember { mutableStateOf(1f) }

    // Floating translation + fading animations
    LaunchedEffect(key1 = feedback.id) {
        animate(
            initialValue = 0f,
            targetValue = -120f,
            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
        ) { value, _ ->
            itemOffset = value
        }
    }

    LaunchedEffect(key1 = feedback.id) {
        animate(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        ) { value, _ ->
            itemAlpha = value
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = feedback.text,
            color = Color(0xFFEC4899),
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            modifier = Modifier
                .offset(
                    x = feedback.xOffset.dp,
                    y = (itemOffset + feedback.yOffset).dp
                )
                .graphicsLayer(alpha = itemAlpha)
        )
    }
}


// ======================== BOTTOM NAVIGATION COLUMN BAR ========================
@Composable
fun MomentumBottomNavigation(
    selectedTab: MomentumTab,
    onTabSelected: (MomentumTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16213E).copy(alpha = 0.80f))
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MomentumTab.values().forEach { tab ->
                val active = selectedTab == tab
                
                val iconImage = when (tab) {
                    MomentumTab.Today -> Icons.Default.DateRange
                    MomentumTab.Stats -> Icons.Default.Menu
                    MomentumTab.Mood -> Icons.Default.Face
                    MomentumTab.Coach -> Icons.Default.Info
                    MomentumTab.Awards -> Icons.Default.Star
                }

                val activeColor = Color(0xFF6366F1)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTabSelected(tab) }
                        .padding(horizontal = 8.dp)
                        .testTag("tab_button_${tab.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = iconImage,
                        contentDescription = tab.name,
                        tint = if (active) activeColor else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.name,
                        fontSize = 9.sp,
                        fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
                        color = if (active) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}


// ======================== DAILY MINDSET LEARNING ========================
@Composable
fun DailyLearningConceptsCard(viewModel: MomentumViewModel) {
    val todayDateStr = viewModel.getTodayDateString()
    val completions by viewModel.conceptCompletions.collectAsState(initial = emptyList())
    val concepts = remember { com.example.data.ConceptsRepository.getDailyConceptsForToday() }
    
    var expandedConcept by remember { mutableStateOf<com.example.data.Concept?>(null) }
    var showAiExplanationDialog by remember { mutableStateOf(false) }

    // State for generated AI custom concept
    val aiGeneratedConcept by viewModel.aiGeneratedConcept.collectAsState()
    val isAiLoading by viewModel.isAiConceptLoading.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧠", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                    Column {
                        Text(
                            text = "Daily Mindset Science",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "1-2 premium insights daily",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF6366F1).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEW TODAY",
                        color = Color(0xFFA5B4FC),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Concepts list
            concepts.forEachIndexed { index, concept ->
                val isDone = completions.any { it.conceptId == concept.id && it.dateString == todayDateStr }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .clickable { expandedConcept = concept }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = concept.emoji,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Insight ${index + 1}: ${concept.title}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = concept.summary,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Check Indicator or Icon arrow
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isDone) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(Color(0xFF22C55E), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Read",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "+${concept.xpReward} XP",
                                color = Color(0xFFF59E0B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Learn More",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
                if (index < concepts.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI custom concept block
            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            if (aiGeneratedConcept != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6366F1).copy(alpha = 0.15f), Color(0xFFEC4899).copy(alpha = 0.15f))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(colors = listOf(Color(0xFF6366F1).copy(alpha = 0.3f), Color(0xFFEC4899).copy(alpha = 0.3f))),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { showAiExplanationDialog = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Custom Concept Ready",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Tap to read custom AI generation",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Read AI Concept",
                        tint = Color.White
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.fetchAiCustomConcept() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personalizing AI Concept...", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        val iconStarStr = "✨"
                        Text("$iconStarStr Generate AI Custom Insight (+15 XP)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Standard Concept Dialog
    if (expandedConcept != null) {
        ConceptDetailDialog(
            concept = expandedConcept!!,
            isCompleted = completions.any { it.conceptId == expandedConcept!!.id && it.dateString == todayDateStr },
            onCheckoff = {
                viewModel.completeConceptToday(expandedConcept!!.id, expandedConcept!!.xpReward)
                expandedConcept = null
            },
            onDismiss = { expandedConcept = null }
        )
    }

    // AI Generated Concept Dialog
    if (showAiExplanationDialog && aiGeneratedConcept != null) {
        val parsed = parseAiConcept(aiGeneratedConcept!!)
        ConceptDetailDialog(
            concept = parsed,
            isCompleted = completions.any { it.conceptId == parsed.id && it.dateString == todayDateStr },
            onCheckoff = {
                viewModel.completeConceptToday(parsed.id, parsed.xpReward)
                showAiExplanationDialog = false
            },
            onDismiss = { showAiExplanationDialog = false }
        )
    }
}

fun parseAiConcept(raw: String): com.example.data.Concept {
    var title = "Nuance Flow"
    var category = "Focus Synthesis"
    var emoji = "🧬"
    var summary = "Optimize environmental factors to initiate flow states."
    var longDescription = "Optimize focus environments by minimizing sensory background distraction."
    var fact = "Scientific research shows that reducing environmental context shifting speeds task resumption."

    try {
        val lines = raw.lines()
        for (line in lines) {
            val clean = line.trim()
            if (clean.startsWith("[TITLE]")) {
                title = clean.removePrefix("[TITLE]").trim()
            } else if (clean.startsWith("[CATEGORY]")) {
                category = clean.removePrefix("[CATEGORY]").trim()
            } else if (clean.startsWith("[EMOJI]")) {
                emoji = clean.removePrefix("[EMOJI]").trim()
            } else if (clean.startsWith("[SUMMARY]")) {
                summary = clean.removePrefix("[SUMMARY]").trim()
            } else if (clean.startsWith("[LONG_TEXT]")) {
                longDescription = clean.removePrefix("[LONG_TEXT]").trim()
            } else if (clean.startsWith("[FACT]")) {
                fact = clean.removePrefix("[FACT]").trim()
            }
        }
    } catch (e: Exception) {
        // fallback
    }

    return com.example.data.Concept(
        id = "ai_concept_${title.lowercase().replace(" ", "_").hashCode()}",
        title = title,
        category = category,
        emoji = emoji,
        summary = summary,
        longDescription = longDescription,
        scientificFact = fact
    )
}

@Composable
fun ConceptDetailDialog(
    concept: com.example.data.Concept,
    isCompleted: Boolean,
    onCheckoff: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13112E))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header emoji and close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = concept.emoji, fontSize = 28.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Titles
                Text(
                    text = concept.category.uppercase(),
                    color = Color(0xFFA5B4FC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = concept.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Summary Block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = concept.summary,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.ui.text.TextStyle(lineHeight = 18.sp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Explanation
                Text(
                    text = "THE MECHANISM",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = concept.longDescription,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    style = androidx.compose.ui.text.TextStyle(lineHeight = 19.sp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Research / Quote Block
                Text(
                    text = "LOGISTICS & SCIENCE",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = concept.scientificFact,
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp,
                            style = androidx.compose.ui.text.TextStyle(lineHeight = 16.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Button complete
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .background(Color(0xFF22C55E).copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Done", tint = Color(0xFF22C55E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fully Mastered For Today • XP Awarded", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = onCheckoff,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Text(
                            text = "Digest & Applying Mindset (+${concept.xpReward} XP)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


// ======================== CLOUD ACCOUNT MANAGEMENT ========================
@Composable
fun CloudProfileSyncDialog(
    viewModel: MomentumViewModel,
    onDismiss: () -> Unit
) {
    val email by viewModel.userEmail.collectAsState()
    val username by viewModel.username.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0C29))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Topic Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLOUD ACCOUNT",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar simulation
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(colors = listOf(Color(0xFF6366F1), Color(0xFFEC4899))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = username.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = username,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = email,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Session Status Block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Connection Integrity:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("Secured Sync Active", color = Color(0xFF22C55E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Last Server Backup:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(lastSyncTime, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Button options
                Button(
                    onClick = { viewModel.manualSync() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading SQLite profiles...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backup Now & Cloud Sync (+10 XP)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sign Out Option
                OutlinedButton(
                    onClick = {
                        viewModel.logOut()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Log Out", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect & Log Out", color = Color(0xFFEF4444).copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Logging in with this account on other mobiles instantly synchronizes your character XP details, customization values, and mindset analytics.",
                    color = Color(0xFF64748B),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(lineHeight = 12.sp)
                )
            }
        }
    }
}


// ======================== AUTHENTICATION PORTAL SERVICE ========================
@Composable
fun MomentumLoginScreen(viewModel: MomentumViewModel) {
    var isSignUpMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var usernameVal by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var successText by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13112E).copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Application Branding Logo
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Brush.linearGradient(colors = listOf(Color(0xFF6366F1), Color(0xFFEC4899))),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⚡", fontSize = 32.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MOMENTUM",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color.White
                )

                Text(
                    text = "Sync habits & learn daily mindset science",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Selector tabs TabRow alternative
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (!isSignUpMode) Color(0xFF6366F1) else Color.Transparent)
                            .clickable {
                                isSignUpMode = false
                                errorText = ""
                                successText = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Log In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSignUpMode) Color(0xFFEC4899) else Color.Transparent)
                            .clickable {
                                isSignUpMode = true
                                errorText = ""
                                successText = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sign Up", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Input components
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorText = "" },
                    label = { Text("Email Address", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("username@domain.com") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.03f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                        focusedIndicatorColor = Color(0xFF6366F1),
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_field"),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 1
                )

                if (isSignUpMode) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = usernameVal,
                        onValueChange = { usernameVal = it; errorText = "" },
                        label = { Text("Username", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("Your explorer name") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.03f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            focusedIndicatorColor = Color(0xFFEC4899),
                            unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_username_field"),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = "" },
                    label = { Text("Secure Password", color = Color(0xFF94A3B8)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.03f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                        focusedIndicatorColor = if (isSignUpMode) Color(0xFFEC4899) else Color(0xFF6366F1),
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_field"),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 1
                )

                // Validation indicators
                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ $errorText",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                if (successText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "✓ $successText",
                        color = Color(0xFF22C55E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Core Submit Button
                Button(
                    onClick = {
                        if (isWorking) return@Button
                        isWorking = true
                        errorText = ""
                        successText = ""
                        
                        if (isSignUpMode) {
                            viewModel.signUp(email, usernameVal, password) { success, msg ->
                                isWorking = false
                                if (success) {
                                    successText = msg
                                } else {
                                    errorText = msg
                                }
                            }
                        } else {
                            viewModel.login(email, password) { success, msg ->
                                isWorking = false
                                if (success) {
                                    successText = msg
                                } else {
                                    errorText = msg
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.linearGradient(
                                colors = if (isSignUpMode) {
                                    listOf(Color(0xFFEC4899), Color(0xFF6366F1))
                                } else {
                                    listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                                }
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isSignUpMode) "Register & Start Syncing" else "Sync Login Session",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick preview card for quick access
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .clickable {
                            email = "demo@momentum.app"
                            password = "demo"
                            if (isSignUpMode) {
                                usernameVal = "Explorer"
                                isSignUpMode = false
                            }
                        }
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "💡 Tap For Autocomplete Demo Explorer Account",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "demo@momentum.app • Password: demo",
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}


// ======================== UTILITIES ========================
fun isDateInCurrentWeek(dateStr: String): Boolean {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = format.parse(dateStr) ?: return false
        
        val nowCal = Calendar.getInstance()
        nowCal.firstDayOfWeek = Calendar.MONDAY
        
        // Find Monday of the current week
        val startOfWeek = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        
        // Find Sunday of the current week (7 days out)
        val endOfWeek = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = startOfWeek.timeInMillis
            add(Calendar.DAY_OF_YEAR, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        date.time >= startOfWeek.timeInMillis && date.time <= endOfWeek.timeInMillis
    } catch (e: Exception) {
        false
    }
}

fun isDateInCurrentMonth(dateStr: String): Boolean {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = format.parse(dateStr) ?: return false
        
        val dateCal = Calendar.getInstance().apply { time = date }
        val currentCal = Calendar.getInstance()
        
        dateCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                dateCal.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH)
    } catch (e: Exception) {
        false
    }
}
