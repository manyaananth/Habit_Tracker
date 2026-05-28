package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Int)

    // Habit Log Queries
    @Query("SELECT * FROM habit_logs ORDER BY timestamp DESC")
    fun getAllHabitLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteLogByHabitAndDate(habitId: Int, dateString: String)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsByHabitId(habitId: Int)
}

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs ORDER BY dateString DESC")
    fun getAllDailyLogs(): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs WHERE dateString = :dateString LIMIT 1")
    suspend fun getDailyLogByDate(dateString: String): DailyLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(log: DailyLog)

    @Query("DELETE FROM daily_logs WHERE dateString = :dateString")
    suspend fun deleteDailyLogByDate(dateString: String)
}

@Dao
interface XpLogDao {
    @Query("SELECT * FROM xp_logs ORDER BY timestamp DESC")
    fun getAllXpLogs(): Flow<List<XpLog>>

    @Query("SELECT SUM(points) FROM xp_logs")
    fun getTotalXpFlow(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertXpLog(log: XpLog): Long

    @Query("SELECT EXISTS(SELECT 1 FROM xp_logs WHERE referenceId = :referenceId)")
    suspend fun hasXpLogWithRef(referenceId: String): Boolean
}

@Dao
interface ConceptCompletionDao {
    @Query("SELECT * FROM concept_completions ORDER BY timestamp DESC")
    fun getAllCompletions(): Flow<List<ConceptCompletion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: ConceptCompletion)

    @Query("SELECT EXISTS(SELECT 1 FROM concept_completions WHERE compositeKey = :compositeKey)")
    suspend fun isConceptCompleted(compositeKey: String): Boolean
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerUser(user: UserAccount)
}

@Database(
    entities = [Habit::class, HabitLog::class, DailyLog::class, XpLog::class, ConceptCompletion::class, UserAccount::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun xpLogDao(): XpLogDao
    abstract fun conceptCompletionDao(): ConceptCompletionDao
    abstract fun userAccountDao(): UserAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "momentum_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
