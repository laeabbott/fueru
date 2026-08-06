package com.fueru.app.data

import androidx.room.migration.Migration

/**
 * Real Room migrations, starting from schema version 15 — the point this app switched from
 * `fallbackToDestructiveMigration` (wipe-and-reseed on every bump) to actually preserving on-device
 * data across an update. Versions before 15 are permanently unmigrated (that data was deliberately
 * abandoned the day this policy took effect) — [AppDatabase]'s builder still falls back to a
 * destructive wipe for anything older than 15, but from here on every version bump needs a real
 * entry in [ALL] or the app will throw on launch after an update instead of silently deleting data.
 * That's deliberate: a crash is loud and immediately obvious (fix it, ship a patch); a silent
 * destructive-migration wipe is exactly the failure mode this file exists to prevent.
 *
 * How to add the next one, when the schema next changes:
 * 1. Bump `AppDatabase`'s `version =` and add a one-line comment to its version-history block, same
 *    as every prior bump already does.
 * 2. Build once so Room exports the new `app/schemas/com.fueru.app.data.AppDatabase/<N>.json` —
 *    diff it against the previous version's json (`diff <(python3 -m json.tool <old>.json) <(python3
 *    -m json.tool <new>.json)` works well) to see exactly what changed: new columns, new tables,
 *    changed types. That diff is the source of truth for what SQL this migration needs to run —
 *    don't hand-derive it from the Kotlin entity diff alone, the two can disagree in subtle ways
 *    (e.g. a Kotlin default parameter value is NOT the same as a SQL DEFAULT constraint unless the
 *    field is also annotated with @ColumnInfo(defaultValue = ...) — check the exported json's
 *    "defaultValue" per column, not the Kotlin source, before writing an ADD COLUMN statement).
 * 3. Add a `Migration(N - 1, N) { database -> database.execSQL("...") }` below, in [ALL].
 * 4. Verify for real, not just by reading the SQL: install the *old* build, create some real data
 *    (a practice, a logged set, whatever the new column touches), then install the *new* build
 *    over it with `adb install -r` (no uninstall) and confirm both that the app doesn't crash and
 *    that the pre-existing data is actually still there and the new field behaves sanely (usually
 *    null/0/false, whatever an absent value should mean).
 */
object Migrations {

    // Example of the shape each entry takes, for whenever the next one is needed (this project's
    // own 14 -> 15 bump — one new nullable column — would have looked like this):
    //
    // private val MIGRATION_15_16 = object : Migration(15, 16) {
    //     override fun migrate(database: SupportSQLiteDatabase) {
    //         database.execSQL("ALTER TABLE practice ADD COLUMN vacationUntilDate TEXT")
    //     }
    // }

    // 15 -> 16, 16 -> 17, ... land here as they're written. Empty for now — nothing has bumped the
    // schema since this policy started.
    val ALL: Array<Migration> = arrayOf()
}
