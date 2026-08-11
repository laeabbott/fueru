package com.fueru.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.fueru.app.FueruApplication
import com.fueru.app.R
import com.fueru.app.ui.components.FueruBottomNav
import com.fueru.app.ui.components.FueruBottomNavItem
import com.fueru.app.ui.onboarding.OnboardingFlow
import com.fueru.app.ui.screens.CharitiesScreen
import com.fueru.app.ui.screens.ConsequencePledgeScreen
import com.fueru.app.ui.screens.FuelScreen
import com.fueru.app.ui.screens.FuwariQuickStartScreen
import com.fueru.app.ui.screens.HomeScreen
import com.fueru.app.ui.screens.PracticeDetailScreen
import com.fueru.app.ui.screens.PracticesScreen
import com.fueru.app.ui.screens.ProgressScreen
import com.fueru.app.ui.screens.ResistanceFlowScreen
import com.fueru.app.ui.screens.SettingsAboutScreen
import com.fueru.app.ui.screens.SettingsCalendarScreen
import com.fueru.app.ui.screens.SettingsDangerScreen
import com.fueru.app.ui.screens.SettingsFoodScreen
import com.fueru.app.ui.screens.SettingsNotificationsScreen
import com.fueru.app.ui.screens.SettingsPracticesScreen
import com.fueru.app.ui.screens.SettingsProfileScreen
import com.fueru.app.ui.screens.SettingsScreen
import com.fueru.app.ui.screens.SettingsStakesScreen
import com.fueru.app.ui.screens.SplashScreen
import com.fueru.app.ui.screens.ThisWeekScreen
import com.fueru.app.ui.screens.UpcomingWorkoutScreen
import com.fueru.app.ui.screens.WorkoutScreen

private val staticTabRoutes = setOf(FueruRoutes.HOME, FueruRoutes.WORKOUT, FueruRoutes.FUEL, FueruRoutes.PROGRESS)

/**
 * Home / Workout / Fuel / Progress bottom tabs (per the live design system), with Splash,
 * Onboarding, and ThisWeek as full-screen flows outside the tab bar. Fuel is only listed in the
 * tab bar when food tracking is enabled — the route itself is always registered so a direct
 * navigate() still works. Practices with `showAsTab` (Settings' "practice tabs" section, module
 * round 1) get appended dynamically below — see the `tabRoutes`/`currentRoute` handling further
 * down for why a practice's route needs reconstructing rather than compared directly.
 */
@Composable
fun FueruNavGraph(
    navController: NavHostController,
    pendingResistanceFlowPracticeId: Long? = null,
    onPendingResistanceFlowConsumed: () -> Unit = {},
    pendingConsequenceEventId: Long? = null,
    onPendingConsequenceConsumed: () -> Unit = {},
) {
    val application = LocalContext.current.applicationContext as FueruApplication
    val userProfile by application.database.userProfileDao().observe().collectAsState(initial = null)
    val foodTrackingEnabled = userProfile?.foodTrackingEnabled == true
    val practices by application.database.practiceDao().observeAll().collectAsState(initial = emptyList())
    val tabPractices = remember(practices) { practices.filter { it.showAsTab } }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val rawRoute = backStackEntry?.destination?.route
    // Compose Navigation's destination.route is the route *pattern* (e.g.
    // "practice_detail/{practiceId}"), not the filled-in path — so comparing it directly against a
    // concrete practice tab's route (e.g. "practice_detail/5") would never match, and that tab would
    // never show as active. Reconstruct the concrete route from the actual practiceId argument
    // whenever we're on that pattern, before doing any tab comparisons below.
    val currentRoute = if (rawRoute == FueruRoutes.PRACTICE_DETAIL_PATTERN) {
        backStackEntry?.arguments?.getLong("practiceId")?.let { FueruRoutes.practiceDetail(it) } ?: rawRoute
    } else {
        rawRoute
    }
    val tabRoutes = remember(tabPractices) {
        staticTabRoutes + tabPractices.map { FueruRoutes.practiceDetail(it.id) }
    }
    // Immersive mode round — Workout is a static tab route (so the nav shows during its normal
    // preview/done states), but an *active* session shouldn't have the bottom nav competing for
    // space or offering an un-confirmed way out. Resistance Flow (the meditation timer's route)
    // needs no equivalent flag: it was never in tabRoutes to begin with, so it's already immersive
    // for every step, not just the timer.
    var workoutSessionActive by remember { mutableStateOf(false) }
    val showBottomNav = currentRoute in tabRoutes && !(currentRoute == FueruRoutes.WORKOUT && workoutSessionActive)

    // An escalation notification tap (Stage 0/1, see NotificationHelper.resistanceFlowPendingIntent)
    // lands here. Known gap, not fixed this round: if this fires before Splash has finished routing
    // (a cold start straight from a notification), this navigate() can race with Splash's own
    // popUpTo — low-probability for a personal app that's normally already been opened once, not
    // engineered around this pass.
    LaunchedEffect(pendingResistanceFlowPracticeId) {
        if (pendingResistanceFlowPracticeId != null) {
            navController.navigate(FueruRoutes.resistanceFlow(pendingResistanceFlowPracticeId, false))
            onPendingResistanceFlowConsumed()
        }
    }

    // A Stage 4 (or offline-resolved) notification tap lands here — same mechanism, same known
    // cold-start race caveat as the resistance-flow deep link above.
    LaunchedEffect(pendingConsequenceEventId) {
        if (pendingConsequenceEventId != null) {
            navController.navigate(FueruRoutes.consequencePledge(pendingConsequenceEventId))
            onPendingConsequenceConsumed()
        }
    }

    val navItems = buildList {
        add(FueruBottomNavItem("Home", R.drawable.ic_house, R.drawable.ic_house_fill, FueruRoutes.HOME))
        add(FueruBottomNavItem("Workout", R.drawable.ic_barbell, R.drawable.ic_barbell_fill, FueruRoutes.WORKOUT))
        if (foodTrackingEnabled) {
            add(FueruBottomNavItem("Fuel", R.drawable.ic_fork_knife, R.drawable.ic_fork_knife_fill, FueruRoutes.FUEL))
        }
        // fuwari round — Progress always stays the rightmost tab, so every practice tab (fuwari,
        // now foundational and default-on, plus any other showAsTab practice) is inserted *before*
        // it rather than after. No existing drawable is generic enough for an arbitrary user-named
        // practice (the set above is all screen-specific) — reusing the fire-branded flame icon for
        // every practice tab rather than building an icon-picker system nobody asked for.
        tabPractices.forEach { practice ->
            add(FueruBottomNavItem(practice.name, R.drawable.ic_flame_fill, R.drawable.ic_flame_fill, FueruRoutes.practiceDetail(practice.id)))
        }
        add(FueruBottomNavItem("Progress", R.drawable.ic_chart_line_up, R.drawable.ic_chart_line_up_fill, FueruRoutes.PROGRESS))
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                FueruBottomNav(items = navItems, activeRoute = currentRoute, onSelect = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = FueruRoutes.SPLASH,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(FueruRoutes.SPLASH) {
                SplashScreen(
                    onHasProfile = {
                        navController.navigate(FueruRoutes.HOME) {
                            popUpTo(FueruRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNoProfile = {
                        navController.navigate(FueruRoutes.ONBOARDING) {
                            popUpTo(FueruRoutes.SPLASH) { inclusive = true }
                        }
                    },
                )
            }
            composable(FueruRoutes.ONBOARDING) {
                OnboardingFlow(
                    onDone = {
                        navController.navigate(FueruRoutes.HOME) {
                            popUpTo(FueruRoutes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(FueruRoutes.HOME) {
                HomeScreen(
                    onOpenThisWeek = { navController.navigate(FueruRoutes.THIS_WEEK) },
                    onStartWorkout = { navController.navigate(FueruRoutes.WORKOUT) { launchSingleTop = true } },
                    onOpenSettings = { navController.navigate(FueruRoutes.SETTINGS) },
                    onOpenPractices = { navController.navigate(FueruRoutes.PRACTICES) },
                    onOpenPractice = { practiceId -> navController.navigate(FueruRoutes.practiceDetail(practiceId)) },
                    onOpenFuel = {
                        navController.navigate(FueruRoutes.FUEL) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onStartFuwari = { navController.navigate(FueruRoutes.FUWARI_QUICKSTART) },
                )
            }
            composable(FueruRoutes.FUWARI_QUICKSTART) {
                FuwariQuickStartScreen(onDone = { navController.popBackStack() })
            }
            composable(FueruRoutes.WORKOUT) {
                WorkoutScreen(
                    onOpenThisWeek = { navController.navigate(FueruRoutes.THIS_WEEK) },
                    onSessionActiveChange = { workoutSessionActive = it },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(FueruRoutes.FUEL) { FuelScreen() }
            composable(FueruRoutes.PROGRESS) { ProgressScreen() }
            composable(FueruRoutes.THIS_WEEK) {
                ThisWeekScreen(
                    onBack = { navController.popBackStack() },
                    onViewExercises = { scheduledWorkoutId ->
                        navController.navigate(FueruRoutes.upcomingWorkout(scheduledWorkoutId))
                    },
                )
            }
            composable(
                route = FueruRoutes.UPCOMING_WORKOUT_PATTERN,
                arguments = listOf(navArgument("scheduledWorkoutId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val scheduledWorkoutId = backStackEntry.arguments?.getLong("scheduledWorkoutId") ?: return@composable
                UpcomingWorkoutScreen(
                    scheduledWorkoutId = scheduledWorkoutId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(FueruRoutes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCategory = { route -> navController.navigate(route) },
                )
            }
            composable(FueruRoutes.SETTINGS_PROFILE) {
                SettingsProfileScreen(onBack = { navController.popBackStack() })
            }
            composable(FueruRoutes.SETTINGS_NOTIFICATIONS) {
                SettingsNotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(FueruRoutes.SETTINGS_FOOD) {
                SettingsFoodScreen(onBack = { navController.popBackStack() })
            }
            composable(FueruRoutes.SETTINGS_CALENDAR) {
                SettingsCalendarScreen(onBack = { navController.popBackStack() })
            }
            composable(FueruRoutes.SETTINGS_PRACTICES) {
                SettingsPracticesScreen(onBack = { navController.popBackStack() })
            }
            composable(FueruRoutes.SETTINGS_STAKES) {
                SettingsStakesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCharities = { navController.navigate(FueruRoutes.CHARITIES) },
                )
            }
            composable(FueruRoutes.SETTINGS_ABOUT) {
                SettingsAboutScreen(onBack = { navController.popBackStack() })
            }
            composable(FueruRoutes.SETTINGS_DANGER) {
                SettingsDangerScreen(
                    onBack = { navController.popBackStack() },
                    onDataWiped = {
                        navController.navigate(FueruRoutes.SPLASH) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            composable(FueruRoutes.CHARITIES) {
                CharitiesScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = FueruRoutes.CONSEQUENCE_PLEDGE_PATTERN,
                arguments = listOf(navArgument("eventId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getLong("eventId") ?: return@composable
                ConsequencePledgeScreen(
                    eventId = eventId,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(FueruRoutes.PRACTICES) {
                PracticesScreen(
                    onOpenPractice = { practiceId -> navController.navigate(FueruRoutes.practiceDetail(practiceId)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = FueruRoutes.PRACTICE_DETAIL_PATTERN,
                arguments = listOf(navArgument("practiceId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val practiceId = backStackEntry.arguments?.getLong("practiceId") ?: return@composable
                PracticeDetailScreen(
                    practiceId = practiceId,
                    onBack = { navController.popBackStack() },
                    onStartResistanceFlow = { startAtIgnite ->
                        navController.navigate(FueruRoutes.resistanceFlow(practiceId, startAtIgnite))
                    },
                )
            }
            composable(
                route = FueruRoutes.RESISTANCE_FLOW_PATTERN,
                arguments = listOf(
                    navArgument("practiceId") { type = NavType.LongType },
                    navArgument("startAtIgnite") { type = NavType.BoolType },
                ),
            ) { backStackEntry ->
                val practiceId = backStackEntry.arguments?.getLong("practiceId") ?: return@composable
                val startAtIgnite = backStackEntry.arguments?.getBoolean("startAtIgnite") ?: false
                ResistanceFlowScreen(
                    practiceId = practiceId,
                    startAtIgnite = startAtIgnite,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
