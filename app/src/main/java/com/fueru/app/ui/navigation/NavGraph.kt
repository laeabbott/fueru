package com.fueru.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.fueru.app.ui.screens.HomeScreen
import com.fueru.app.ui.screens.PracticeDetailScreen
import com.fueru.app.ui.screens.PracticesScreen
import com.fueru.app.ui.screens.ProgressScreen
import com.fueru.app.ui.screens.ResistanceFlowScreen
import com.fueru.app.ui.screens.SettingsScreen
import com.fueru.app.ui.screens.SplashScreen
import com.fueru.app.ui.screens.ThisWeekScreen
import com.fueru.app.ui.screens.UpcomingWorkoutScreen
import com.fueru.app.ui.screens.WorkoutScreen

private val tabRoutes = setOf(FueruRoutes.HOME, FueruRoutes.WORKOUT, FueruRoutes.FUEL, FueruRoutes.PROGRESS)

/**
 * Home / Workout / Fuel / Progress bottom tabs (per the live design system), with Splash,
 * Onboarding, and ThisWeek as full-screen flows outside the tab bar. Fuel is only listed in the
 * tab bar when food tracking is enabled — the route itself is always registered so a direct
 * navigate() still works.
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

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomNav = currentRoute in tabRoutes

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
                )
            }
            composable(FueruRoutes.WORKOUT) {
                WorkoutScreen(onOpenThisWeek = { navController.navigate(FueruRoutes.THIS_WEEK) })
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
                    onDataWiped = {
                        navController.navigate(FueruRoutes.SPLASH) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onOpenCharities = { navController.navigate(FueruRoutes.CHARITIES) },
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
