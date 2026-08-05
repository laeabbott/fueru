package com.fueru.app.ui.navigation

/**
 * Nav routes. Bottom nav is Home / Workout / Fuel / Progress (per the live design system's
 * ui_kits/app/index.html), not the original spec doc's Home / This Week / Workout / Progress —
 * ThisWeek is a flow reached from Home, not its own tab. Fuel is only shown in the bottom nav
 * when food tracking is enabled (see FueruNavGraph), but the route itself always exists.
 */
object FueruRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val WORKOUT = "workout"
    const val FUEL = "fuel"
    const val PROGRESS = "progress"
    const val THIS_WEEK = "this_week"
    const val SETTINGS = "settings"
    /** Core Engine's practice list — not a bottom-nav tab yet, reached via a link from Home. */
    const val PRACTICES = "practices"

    /** Argument-based route — preview/edit any scheduled workout's exercises, not just today's. */
    const val UPCOMING_WORKOUT_PATTERN = "upcoming_workout/{scheduledWorkoutId}"
    fun upcomingWorkout(scheduledWorkoutId: Long) = "upcoming_workout/$scheduledWorkoutId"

    /** Argument-based route — one practice's heatmap/score/manual-log detail screen. */
    const val PRACTICE_DETAIL_PATTERN = "practice_detail/{practiceId}"
    fun practiceDetail(practiceId: Long) = "practice_detail/$practiceId"

    /** Argument-based route — the guided Resistance Flow (project brief §6) for one practice. startAtIgnite carries the §6.3 fade-unlock skip-ahead state. */
    const val RESISTANCE_FLOW_PATTERN = "resistance_flow/{practiceId}/{startAtIgnite}"
    fun resistanceFlow(practiceId: Long, startAtIgnite: Boolean) = "resistance_flow/$practiceId/$startAtIgnite"

    /** Manage the Stage 3/4 charity list (§7.3), reachable from Settings. */
    const val CHARITIES = "charities"

    /** Argument-based route — the Stage 4 pledge screen (§7.4) for one ConsequenceEvent. */
    const val CONSEQUENCE_PLEDGE_PATTERN = "consequence_pledge/{eventId}"
    fun consequencePledge(eventId: Long) = "consequence_pledge/$eventId"
}
