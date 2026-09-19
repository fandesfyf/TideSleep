package com.tidesleep.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tidesleep.app.session.SessionPhase
import com.tidesleep.app.ui.components.OnboardingDialog
import com.tidesleep.app.ui.screens.AboutScreen
import com.tidesleep.app.ui.screens.DevicesScreen
import com.tidesleep.app.ui.screens.ProfileScreen
import com.tidesleep.app.ui.screens.RecordsScreen
import com.tidesleep.app.ui.screens.SafetySettingsScreen
import com.tidesleep.app.ui.screens.ScienceAndMiJiaGuideScreen
import com.tidesleep.app.ui.screens.SessionActiveScreen
import com.tidesleep.app.ui.screens.TonightHomeScreen
import com.tidesleep.app.viewmodel.TideSleepViewModel

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(TideSleepRoutes.Tonight, "今晚", Icons.Outlined.Bedtime),
    BottomTab(TideSleepRoutes.Devices, "设备", Icons.Outlined.Watch),
    BottomTab(TideSleepRoutes.Records, "记录", Icons.Outlined.History),
    BottomTab(TideSleepRoutes.Profile, "我的", Icons.Outlined.Person),
)

@Composable
fun TideSleepNavHost(viewModel: TideSleepViewModel) {
    val navController = rememberNavController()
    val session by viewModel.sessionSnapshot.collectAsStateWithLifecycle()
    val disclaimerAccepted by viewModel.disclaimerAccepted.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (!disclaimerAccepted) {
        OnboardingDialog(
            onAccept = { viewModel.acceptDisclaimer() },
            onPlayCalibration = { viewModel.playCalibrationPulse() },
        )
    }

    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    LaunchedEffect(session.phase) {
        val active = session.phase == SessionPhase.Stimulating ||
            session.phase == SessionPhase.WaitingSleep ||
            session.phase == SessionPhase.WaitingDelay ||
            session.phase == SessionPhase.Arming
        if (active && currentRoute != TideSleepRoutes.SessionActive) {
            navController.navigate(TideSleepRoutes.SessionActive) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TideSleepRoutes.Tonight,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TideSleepRoutes.Tonight) {
                TonightHomeScreen(
                    viewModel = viewModel,
                    onNavigateToScience = {
                        navController.navigate(TideSleepRoutes.ScienceGuide)
                    },
                    onNavigateToSafety = {
                        navController.navigate(TideSleepRoutes.SafetySettings)
                    },
                )
            }
            composable(TideSleepRoutes.Devices) {
                DevicesScreen(
                    viewModel = viewModel,
                    onNavigateToScience = {
                        navController.navigate(TideSleepRoutes.ScienceGuide)
                    },
                )
            }
            composable(TideSleepRoutes.Records) {
                RecordsScreen(viewModel = viewModel)
            }
            composable(TideSleepRoutes.Profile) {
                ProfileScreen(
                    onNavigateToSafety = {
                        navController.navigate(TideSleepRoutes.SafetySettings)
                    },
                    onNavigateToScience = {
                        navController.navigate(TideSleepRoutes.ScienceGuide)
                    },
                    onNavigateToAbout = {
                        navController.navigate(TideSleepRoutes.About)
                    },
                )
            }
            composable(TideSleepRoutes.SessionActive) {
                SessionActiveScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack(TideSleepRoutes.Tonight, false)
                    },
                )
            }
            composable(TideSleepRoutes.SafetySettings) {
                SafetySettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(TideSleepRoutes.ScienceGuide) {
                ScienceAndMiJiaGuideScreen(onBack = { navController.popBackStack() })
            }
            composable(TideSleepRoutes.About) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
