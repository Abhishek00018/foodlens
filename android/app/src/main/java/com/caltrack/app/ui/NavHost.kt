package com.caltrack.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.caltrack.app.ui.auth.LoginScreen
import com.caltrack.app.ui.auth.StartupViewModel
import com.caltrack.app.ui.camera.CameraScreen
import com.caltrack.app.ui.camera.CameraViewModel
import com.caltrack.app.ui.camera.ScanResultSheet
import com.caltrack.app.ui.camera.ScanUiState
import com.caltrack.app.ui.dashboard.DashboardScreen
import com.caltrack.app.ui.history.HistoryScreen
import com.caltrack.app.ui.meal.MealDetailScreen
import com.caltrack.app.ui.onboarding.OnboardingScreen
import com.caltrack.app.ui.profile.ProfileScreen
import com.caltrack.app.ui.theme.DarkSurface
import com.caltrack.app.ui.theme.NeonLime
import com.caltrack.app.ui.theme.TextSecondary
import kotlinx.serialization.Serializable

@Serializable object LoginRoute
@Serializable object OnboardingRoute
@Serializable object DashboardRoute
@Serializable object CameraRoute
@Serializable object HistoryRoute
@Serializable object ProfileRoute
@Serializable data class MealDetailRoute(val mealId: String)

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any,
)

private const val ANIM_DURATION = 350

@Composable
fun CaltrackNavHost() {
    val startupViewModel: StartupViewModel = hiltViewModel()
    val startDest by startupViewModel.startDestination.collectAsStateWithLifecycle()

    // Show splash until start destination is determined
    if (startDest == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NeonLime)
        }
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", Icons.Default.Home, DashboardRoute),
        BottomNavItem("History", Icons.AutoMirrored.Filled.List, HistoryRoute),
    )

    val showBottomBar = currentDestination?.let { dest ->
        bottomNavItems.any { dest.hasRoute(it.route::class) }
    } ?: false

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(DashboardRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonLime,
                                selectedTextColor = NeonLime,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = NeonLime.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest!!,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Login
            composable<LoginRoute> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(OnboardingRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    }
                )
            }

            // Onboarding
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(OnboardingRoute) { inclusive = true }
                        }
                    }
                )
            }

            // Dashboard
            composable<DashboardRoute>(
                enterTransition = { fadeIn(tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(tween(ANIM_DURATION)) },
                popEnterTransition = { fadeIn(tween(ANIM_DURATION)) },
                popExitTransition = { fadeOut(tween(ANIM_DURATION)) }
            ) {
                DashboardScreen(
                    onCameraClick = { navController.navigate(CameraRoute) },
                    onProfileClick = { navController.navigate(ProfileRoute) },
                    onMealClick = { mealId -> navController.navigate(MealDetailRoute(mealId)) }
                )
            }

            // Camera — full scan flow: capture → scan API → ScanResultSheet → log
            composable<CameraRoute>(
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(ANIM_DURATION))
                },
                exitTransition = { fadeOut(tween(200)) },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(200))
                }
            ) {
                val cameraViewModel: CameraViewModel = hiltViewModel()
                val scanState by cameraViewModel.scanState.collectAsStateWithLifecycle()
                val context = LocalContext.current

                // Navigate back to Dashboard once meal is logged
                LaunchedEffect(scanState) {
                    if (scanState is ScanUiState.MealLogged) {
                        cameraViewModel.reset()
                        navController.popBackStack(DashboardRoute, false)
                    }
                }

                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    CameraScreen(
                        onBack = { navController.popBackStack() },
                        onPhotoTaken = { uri ->
                            cameraViewModel.scanImage(context = context, imageUri = uri)
                        }
                    )

                    // Scanning overlay
                    if (scanState is ScanUiState.Scanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeonLime)
                        }
                    }

                    // Error snackbar
                    if (scanState is ScanUiState.Error) {
                        val msg = (scanState as ScanUiState.Error).message
                        LaunchedEffect(msg) {
                            snackbarHostState.showSnackbar(msg)
                            cameraViewModel.clearError()
                        }
                    }
                }

                // Scan result bottom sheet
                if (scanState is ScanUiState.Success || scanState is ScanUiState.LoggingMeal) {
                    if (scanState is ScanUiState.Success) {
                        val s = scanState as ScanUiState.Success
                        ScanResultSheet(
                            result = s.scanResponse,
                            localImageUri = s.localImageUri,
                            isLogging = false,
                            onConfirm = {
                                cameraViewModel.logMeal(s.scanResponse, s.localImageUri)
                            },
                            onRetry = { cameraViewModel.reset() },
                            onDismiss = { cameraViewModel.reset() }
                        )
                    } else if (scanState is ScanUiState.LoggingMeal) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeonLime)
                        }
                    }
                }
            }

            // History
            composable<HistoryRoute>(
                enterTransition = { fadeIn(tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(tween(ANIM_DURATION)) },
                popEnterTransition = { fadeIn(tween(ANIM_DURATION)) },
                popExitTransition = { fadeOut(tween(ANIM_DURATION)) }
            ) {
                HistoryScreen()
            }

            // Profile — slide in from right
            composable<ProfileRoute>(
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    )
                }
            ) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(LoginRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Meal Detail — slide in from right
            composable<MealDetailRoute>(
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    )
                }
            ) {
                MealDetailScreen(
                    onBack = { navController.popBackStack() },
                    onDelete = { navController.popBackStack() },
                    onEdit = { /* TODO: edit screen */ }
                )
            }
        }
    }
}
