package com.fueru.app.data.seed

import android.content.Context
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.entity.PrescribedSet
import com.fueru.app.data.entity.Practice
import com.fueru.app.data.entity.ProgramDay

/** The one foundational, always-seeded practice — see ensureFuwariSeeded. */
const val FUWARI_PRACTICE_NAME = "fuwari"

/**
 * Defensive re-check run on every app start (see FueruApplication.onCreate) — RoomDatabase.Callback.onCreate
 * is fire-and-forget and only fires on a true first-create or a destructive-migration recreate, so this
 * catches any edge case where the program table ended up empty without relying on that callback alone.
 * Also tops up the exercise catalog beyond the lean hand-picked set — see ensureExerciseCatalogSeeded.
 */
suspend fun ensureSeeded(context: Context, database: AppDatabase) {
    if (database.programDayDao().count() == 0) {
        seedDatabase(database)
    }
    ensureExerciseCatalogSeeded(context, database)
    ensureFuwariSeeded(database)
}

/**
 * fuwari round — meditation is a foundational, default-on tab, not something the user has to build
 * themselves the way every other practice is. Idempotent by name (checked, not inserted
 * unconditionally, on every app start) rather than a one-shot RoomDatabase.Callback — same reasoning
 * as [ensureSeeded]'s own top-level check, and it means a "wipe all data" reseed brings fuwari back
 * automatically. Seeded with guidedSessionEnabled + showAsTab both true — reuses every bit of the
 * guided-session/bottom-nav-tab machinery already built for user-created practices, no special-casing
 * anywhere else needed.
 */
suspend fun ensureFuwariSeeded(database: AppDatabase) {
    if (database.practiceDao().getByName(FUWARI_PRACTICE_NAME) != null) return
    database.practiceDao().insert(
        Practice(
            name = FUWARI_PRACTICE_NAME,
            targetFrequencyType = "per_week",
            targetFrequencyCount = 3,
            guidedSessionEnabled = true,
            showAsTab = true,
            actionVerb = "fuwariing",
            createdDate = System.currentTimeMillis(),
        ),
    )
}

/** Runs once, from [AppDatabase]'s RoomDatabase.Callback.onCreate, on first launch only. */
suspend fun seedDatabase(database: AppDatabase) {
    database.exerciseDao().insertAll(ExerciseSeed.all)

    ProgramSeed.days.forEach { seedDay ->
        val programDayId = database.programDayDao()
            .insertAll(listOf(ProgramDay(phase = seedDay.phase, dayLabel = seedDay.dayLabel)))
            .first()

        val prescribedSets = seedDay.sets.map { seedSet ->
            PrescribedSet(
                programDayId = programDayId,
                exerciseId = seedSet.exerciseId,
                orderInDay = seedSet.orderInDay,
                sets = seedSet.sets,
                repsMin = seedSet.repsMin,
                repsMax = seedSet.repsMax,
                tempo = seedSet.tempo,
                comment = seedSet.comment,
                supersetGroup = seedSet.supersetGroup,
                isDropSetFinal = seedSet.isDropSetFinal,
                isTensionFocus = seedSet.isTensionFocus,
            )
        }
        database.prescribedSetDao().insertAll(prescribedSets)
    }
}
