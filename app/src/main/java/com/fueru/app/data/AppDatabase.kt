package com.fueru.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fueru.app.data.dao.CharityDao
import com.fueru.app.data.dao.ConsequenceEventDao
import com.fueru.app.data.dao.CustomFoodDao
import com.fueru.app.data.dao.CustomFoodIngredientDao
import com.fueru.app.data.dao.DailyNutritionLogDao
import com.fueru.app.data.dao.ExerciseDao
import com.fueru.app.data.dao.FoodLogEntryDao
import com.fueru.app.data.dao.GuidedSessionDao
import com.fueru.app.data.dao.PracticeDao
import com.fueru.app.data.dao.PracticeLogEntryDao
import com.fueru.app.data.dao.PracticeScheduledSlotDao
import com.fueru.app.data.dao.PrescribedSetDao
import com.fueru.app.data.dao.ProgramDayDao
import com.fueru.app.data.dao.RecurringScheduleDao
import com.fueru.app.data.dao.ResistanceSessionDao
import com.fueru.app.data.dao.ScheduledWorkoutDao
import com.fueru.app.data.dao.ScheduledWorkoutExerciseOverrideDao
import com.fueru.app.data.dao.SetLogDao
import com.fueru.app.data.dao.UserProfileDao
import com.fueru.app.data.entity.Charity
import com.fueru.app.data.entity.ConsequenceEvent
import com.fueru.app.data.entity.CustomFood
import com.fueru.app.data.entity.CustomFoodIngredient
import com.fueru.app.data.entity.DailyNutritionLog
import com.fueru.app.data.entity.Exercise
import com.fueru.app.data.entity.FoodLogEntry
import com.fueru.app.data.entity.GuidedSession
import com.fueru.app.data.entity.Practice
import com.fueru.app.data.entity.PracticeLogEntry
import com.fueru.app.data.entity.PracticeScheduledSlot
import com.fueru.app.data.entity.PrescribedSet
import com.fueru.app.data.entity.ProgramDay
import com.fueru.app.data.entity.RecurringScheduleEntry
import com.fueru.app.data.entity.ResistanceSession
import com.fueru.app.data.entity.ScheduledWorkout
import com.fueru.app.data.entity.ScheduledWorkoutExerciseOverride
import com.fueru.app.data.entity.SetLog
import com.fueru.app.data.entity.UserProfile

@Database(
    entities = [
        UserProfile::class,
        Exercise::class,
        ProgramDay::class,
        PrescribedSet::class,
        ScheduledWorkout::class,
        SetLog::class,
        RecurringScheduleEntry::class,
        DailyNutritionLog::class,
        ScheduledWorkoutExerciseOverride::class,
        FoodLogEntry::class,
        CustomFood::class,
        CustomFoodIngredient::class,
        Practice::class,
        PracticeLogEntry::class,
        PracticeScheduledSlot::class,
        ResistanceSession::class,
        Charity::class,
        ConsequenceEvent::class,
        GuidedSession::class,
    ],
    // 7 -> 8: dropped the points/streak ledger (PointsLedgerEntry + UserProfile's four points/streak
    // fields — the gamification layer removed as part of the practices pivot) and added the new
    // Core Engine tables (Practice, PracticeLogEntry) in the same bump, so this is one combined wipe
    // rather than two separate ones.
    // 8 -> 9: added PracticeScheduledSlot (Scheduler phase, project brief §5).
    // 9 -> 10: added ResistanceSession + Practice.shortFlowEnabled (Resistance Flow phase, §6).
    // 10 -> 11: added Charity + ConsequenceEvent + Practice.stickCharityEnabled (Stage 3/4 pledge mode, §7.3/§7.4).
    // 11 -> 12: added GuidedSession + Practice.guidedSessionEnabled/showAsTab (first practice module,
    // "fuwari" — guided session timer + configurable bottom tabs).
    // 12 -> 13: added Practice.actionVerb (scheduling & escalation alignment pass, §D — "I've
    // started" quick-start button's optional per-practice themed label).
    // 13 -> 14: added UserProfile.goal ("buildMuscle" or "maintain"), feeding TdeeCalculator's
    // surplus decision — follow-up round, onboarding didn't previously ask this.
    // 14 -> 15: added Practice.vacationUntilDate (vacation-practices round).
    version = 15,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun programDayDao(): ProgramDayDao
    abstract fun prescribedSetDao(): PrescribedSetDao
    abstract fun scheduledWorkoutDao(): ScheduledWorkoutDao
    abstract fun setLogDao(): SetLogDao
    abstract fun recurringScheduleDao(): RecurringScheduleDao
    abstract fun dailyNutritionLogDao(): DailyNutritionLogDao
    abstract fun scheduledWorkoutExerciseOverrideDao(): ScheduledWorkoutExerciseOverrideDao
    abstract fun foodLogEntryDao(): FoodLogEntryDao
    abstract fun customFoodDao(): CustomFoodDao
    abstract fun customFoodIngredientDao(): CustomFoodIngredientDao
    abstract fun practiceDao(): PracticeDao
    abstract fun practiceLogEntryDao(): PracticeLogEntryDao
    abstract fun practiceScheduledSlotDao(): PracticeScheduledSlotDao
    abstract fun resistanceSessionDao(): ResistanceSessionDao
    abstract fun charityDao(): CharityDao
    abstract fun consequenceEventDao(): ConsequenceEventDao
    abstract fun guidedSessionDao(): GuidedSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): AppDatabase {
            // Prepopulation (Section 8 program + lean exercise subset) is handled by
            // FueruApplication.onCreate() -> ensureSeeded(), not a RoomDatabase.Callback — a single
            // call site avoids a double-seed race between a callback and any other seed trigger.
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "fueru.db",
            )
                // Dev-time only: no real user data to preserve yet, so a schema bump just wipes
                // and reseeds rather than needing a real Migration. Revisit before shipping.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
