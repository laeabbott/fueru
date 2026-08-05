package com.fueru.app.data.seed

/**
 * Pre-insert representation of the Section 8 program tables — [ProgramDay] and [PrescribedSet]
 * rows need a real auto-generated `programDayId` foreign key, so this intermediate shape gets
 * inserted day-by-day in SeedData.kt rather than built as entities directly.
 *
 * The source tables (Section 8.2-8.4) mark some cells with color (red = superset, yellow = final
 * set is a drop set, blue = tension-focus tempo) per the Section 8.1 legend, but that coloring did
 * not survive into the transcribed markdown tables handed to this build — no cell in the source
 * text is actually marked, so every SeedSet here defaults supersetGroup/isDropSetFinal/isTensionFocus
 * to null/false rather than guessing which rows were highlighted.
 *
 * Farmer's Carries / Suitcase Marches are time-based ("3x60s"), not rep-based — the entity model
 * has no isTimeBased flag, so the duration is folded into [SeedSet.comment] and repsMin/repsMax
 * both hold the seconds value.
 */
data class SeedSet(
    val exerciseId: String,
    val orderInDay: Int,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val tempo: String,
    val comment: String? = null,
    val supersetGroup: String? = null,
    val isDropSetFinal: Boolean = false,
    val isTensionFocus: Boolean = false,
)

data class SeedDay(
    val phase: String,
    val dayLabel: String,
    val sets: List<SeedSet>,
)

object ProgramSeed {

    private const val LUNGES = "Bodyweight_Walking_Lunge"
    private const val HIP_THRUSTS = "Barbell_Hip_Thrust"
    private const val CALF_RAISES = "Standing_Calf_Raises"
    private const val BICEP_CURLS = "Dumbbell_Bicep_Curl"
    private const val TRICEP_EXTENSIONS = "Standing_Dumbbell_Triceps_Extension"
    private const val PUSH_UPS = "Pushups"
    private const val KNEE_TUCKS = "knee_tucks"
    private const val SQUATS = "Barbell_Squat"
    private const val HAMSTRING_CURLS = "Lying_Leg_Curls"
    private const val LEG_ABDUCTIONS = "Thigh_Abductor"
    private const val LOW_ROWS = "Seated_Cable_Rows"
    private const val BENCH_PRESS = "Barbell_Bench_Press_-_Medium_Grip"
    private const val ASSISTED_PULL_UPS = "Band_Assisted_Pull-Up"
    private const val FOREARM_CURLS = "Seated_Palm-Up_Barbell_Wrist_Curl"
    private const val RDLS = "Romanian_Deadlift"
    private const val FARMERS_CARRIES = "Farmers_Walk"
    private const val LEG_EXTENSIONS = "Leg_Extensions"
    private const val DUMBBELL_INCLINE_BENCH = "Incline_Dumbbell_Press"
    private const val TRICEP_SKULLCRUSHERS = "EZ-Bar_Skullcrusher"
    private const val LEG_RAISES = "Flat_Bench_Lying_Leg_Raise"
    private const val DEADLIFTS = "Barbell_Deadlift"
    private const val ROWS = "Bent_Over_Barbell_Row"
    private const val ABDUCTORS_ADDUCTORS = "Thigh_Adductor"
    private const val INCLINE_BENCH = "Barbell_Incline_Bench_Press_-_Medium_Grip"
    private const val BEHIND_THE_BACK_TRICEP_EXTENSIONS = "behind_the_back_tricep_extension"
    private const val SUITCASE_MARCHES = "Farmers_Walk"
    private const val LAT_PULLDOWNS = "Wide-Grip_Lat_Pulldown"
    private const val CHEST_SUPPORTED_ROWS = "Incline_Bench_Pull"
    private const val LATERAL_RAISES = "Side_Lateral_Raise"
    private const val SHOULDER_PRESSES = "Dumbbell_Shoulder_Press"

    private const val PHASE_0_6 = "0-6"
    private const val PHASE_6_12 = "6-12"
    private const val PHASE_12_24 = "12-24"

    val days: List<SeedDay> = listOf(
        // ---- Phase 0-6 months ----
        SeedDay(
            PHASE_0_6, "Day 1",
            listOf(
                SeedSet(LUNGES, 1, sets = 4, repsMin = 10, repsMax = 10, tempo = "2-1-2", comment = "10 reps per leg. Focus on moving upward rather than forward"),
                SeedSet(HIP_THRUSTS, 2, sets = 3, repsMin = 15, repsMax = 15, tempo = "2-0-3", comment = "Slow and controlled"),
                SeedSet(CALF_RAISES, 3, sets = 4, repsMin = 12, repsMax = 12, tempo = "1-0-3", comment = "Quick ascent, slow descent"),
            ),
        ),
        SeedDay(
            PHASE_0_6, "Day 2",
            listOf(
                SeedSet(BICEP_CURLS, 1, sets = 3, repsMin = 8, repsMax = 8, tempo = "3-1-2"),
                SeedSet(TRICEP_EXTENSIONS, 2, sets = 3, repsMin = 8, repsMax = 8, tempo = "3-1-2"),
                SeedSet(PUSH_UPS, 3, sets = 4, repsMin = 12, repsMax = 12, tempo = "2-0-2"),
                SeedSet(KNEE_TUCKS, 4, sets = 3, repsMin = 15, repsMax = 15, tempo = "1-1-3"),
            ),
        ),
        SeedDay(
            PHASE_0_6, "Day 3",
            listOf(
                SeedSet(SQUATS, 1, sets = 4, repsMin = 6, repsMax = 6, tempo = "2-1-2", comment = "Heavier — testing limits"),
                SeedSet(HIP_THRUSTS, 2, sets = 3, repsMin = 6, repsMax = 6, tempo = "2-0-3"),
                SeedSet(HAMSTRING_CURLS, 3, sets = 4, repsMin = 10, repsMax = 12, tempo = "2-1-3"),
                SeedSet(LEG_ABDUCTIONS, 4, sets = 3, repsMin = 12, repsMax = 15, tempo = "1-1-1"),
            ),
        ),
        SeedDay(
            PHASE_0_6, "Day 4",
            listOf(
                SeedSet(LOW_ROWS, 1, sets = 4, repsMin = 8, repsMax = 10, tempo = "1-0-2"),
                SeedSet(BENCH_PRESS, 2, sets = 2, repsMin = 12, repsMax = 15, tempo = "2-1-2"),
                SeedSet(ASSISTED_PULL_UPS, 3, sets = 4, repsMin = 8, repsMax = 10, tempo = "2-0-2"),
                SeedSet(KNEE_TUCKS, 4, sets = 3, repsMin = 15, repsMax = 15, tempo = "1-1-3"),
            ),
        ),

        // ---- Phase 6-12 months ----
        SeedDay(
            PHASE_6_12, "Day 1",
            listOf(
                SeedSet(SQUATS, 1, sets = 4, repsMin = 6, repsMax = 10, tempo = "2-0-2"),
                SeedSet(HIP_THRUSTS, 2, sets = 3, repsMin = 12, repsMax = 15, tempo = "1-1-1"),
                SeedSet(HAMSTRING_CURLS, 3, sets = 3, repsMin = 8, repsMax = 15, tempo = "1-1-3"),
                SeedSet(CALF_RAISES, 4, sets = 4, repsMin = 8, repsMax = 12, tempo = "2-0-2"),
            ),
        ),
        SeedDay(
            PHASE_6_12, "Day 2",
            listOf(
                SeedSet(BENCH_PRESS, 1, sets = 5, repsMin = 6, repsMax = 10, tempo = "1-0-2", comment = "Flat bench press"),
                SeedSet(FOREARM_CURLS, 2, sets = 3, repsMin = 10, repsMax = 18, tempo = "1-1-2"),
                SeedSet(TRICEP_EXTENSIONS, 3, sets = 2, repsMin = 10, repsMax = 15, tempo = "1-1-2"),
                SeedSet(BICEP_CURLS, 4, sets = 3, repsMin = 6, repsMax = 12, tempo = "2-0-2"),
            ),
        ),
        SeedDay(
            PHASE_6_12, "Day 3",
            listOf(
                SeedSet(RDLS, 1, sets = 4, repsMin = 10, repsMax = 15, tempo = "2-1-2"),
                SeedSet(FARMERS_CARRIES, 2, sets = 3, repsMin = 60, repsMax = 60, tempo = "N/A", comment = "60 seconds per set"),
                SeedSet(LEG_EXTENSIONS, 3, sets = 3, repsMin = 8, repsMax = 12, tempo = "1-1-2"),
                SeedSet(HAMSTRING_CURLS, 4, sets = 3, repsMin = 8, repsMax = 12, tempo = "1-1-3"),
            ),
        ),
        SeedDay(
            PHASE_6_12, "Day 4",
            listOf(
                SeedSet(DUMBBELL_INCLINE_BENCH, 1, sets = 3, repsMin = 6, repsMax = 8, tempo = "2-0-2"),
                SeedSet(LOW_ROWS, 2, sets = 4, repsMin = 8, repsMax = 10, tempo = "2-0-2"),
                SeedSet(BICEP_CURLS, 3, sets = 3, repsMin = 10, repsMax = 12, tempo = "3-1-2"),
                SeedSet(TRICEP_SKULLCRUSHERS, 4, sets = 3, repsMin = 10, repsMax = 12, tempo = "3-1-2"),
                SeedSet(LEG_RAISES, 5, sets = 3, repsMin = 15, repsMax = 15, tempo = "1-1-3"),
            ),
        ),

        // ---- Phase 12-24 months ----
        SeedDay(
            PHASE_12_24, "Day 1",
            listOf(
                SeedSet(SQUATS, 1, sets = 5, repsMin = 8, repsMax = 12, tempo = "1-0-2", comment = "Moderate weight day"),
                SeedSet(RDLS, 2, sets = 3, repsMin = 6, repsMax = 10, tempo = "2-1-1"),
                SeedSet(FARMERS_CARRIES, 3, sets = 3, repsMin = 60, repsMax = 60, tempo = "N/A", comment = "60 seconds per set"),
            ),
        ),
        SeedDay(
            PHASE_12_24, "Day 2",
            listOf(
                SeedSet(BENCH_PRESS, 1, sets = 5, repsMin = 6, repsMax = 8, tempo = "2-0-2", comment = "Normal working weight"),
                SeedSet(ROWS, 2, sets = 5, repsMin = 6, repsMax = 8, tempo = "3-1-2"),
                SeedSet(TRICEP_SKULLCRUSHERS, 3, sets = 4, repsMin = 10, repsMax = 12, tempo = "3-1-2"),
                SeedSet(BICEP_CURLS, 4, sets = 4, repsMin = 10, repsMax = 12, tempo = "3-1-2"),
            ),
        ),
        SeedDay(
            PHASE_12_24, "Day 3",
            listOf(
                SeedSet(DEADLIFTS, 1, sets = 5, repsMin = 3, repsMax = 5, tempo = "form focus", comment = "Heavy"),
                SeedSet(RDLS, 2, sets = 3, repsMin = 10, repsMax = 12, tempo = "2-1-2", comment = "Light"),
                SeedSet(HIP_THRUSTS, 3, sets = 3, repsMin = 12, repsMax = 15, tempo = "2-0-3"),
                SeedSet(ABDUCTORS_ADDUCTORS, 4, sets = 3, repsMin = 10, repsMax = 12, tempo = "1-1-3"),
            ),
        ),
        SeedDay(
            PHASE_12_24, "Day 4",
            listOf(
                SeedSet(INCLINE_BENCH, 1, sets = 5, repsMin = 6, repsMax = 10, tempo = "1-0-1", comment = "Steady control"),
                SeedSet(BICEP_CURLS, 2, sets = 4, repsMin = 10, repsMax = 12, tempo = "2-1-2"),
                SeedSet(BEHIND_THE_BACK_TRICEP_EXTENSIONS, 3, sets = 4, repsMin = 10, repsMax = 12, tempo = "2-1-2"),
                SeedSet(SUITCASE_MARCHES, 4, sets = 3, repsMin = 60, repsMax = 60, tempo = "N/A", comment = "60 seconds per set. ⅓–½ bodyweight load"),
                SeedSet(LEG_RAISES, 5, sets = 4, repsMin = 10, repsMax = 10, tempo = "1-0-2"),
            ),
        ),
        SeedDay(
            PHASE_12_24, "Day 5",
            listOf(
                SeedSet(LAT_PULLDOWNS, 1, sets = 4, repsMin = 8, repsMax = 12, tempo = "1-1-3", comment = "Mid-width grip, controlled eccentric"),
                SeedSet(CHEST_SUPPORTED_ROWS, 2, sets = 4, repsMin = 5, repsMax = 10, tempo = "1-1-2"),
                SeedSet(LATERAL_RAISES, 3, sets = 5, repsMin = 12, repsMax = 15, tempo = "1-0-2"),
                SeedSet(SHOULDER_PRESSES, 4, sets = 5, repsMin = 10, repsMax = 15, tempo = "1-0-2"),
            ),
        ),
    )
}
