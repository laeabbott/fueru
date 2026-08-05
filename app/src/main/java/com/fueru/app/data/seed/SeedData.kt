package com.fueru.app.data.seed

import android.content.Context
import com.fueru.app.data.AppDatabase
import com.fueru.app.data.entity.PrescribedSet
import com.fueru.app.data.entity.ProgramDay

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
