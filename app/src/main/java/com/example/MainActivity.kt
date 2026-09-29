package com.example

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantMotion
import com.example.ui.designsystem.MutantPalette
import com.example.ui.designsystem.MutantTypeScale
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MutantViewModel
import com.example.ui.components.RestTimerAlerts
import kotlinx.coroutines.flow.MutableSharedFlow

enum class NavDestination(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("Protocol", Icons.Default.CalendarToday, "nav_home"),
    WORKOUT("Session", Icons.Default.FitnessCenter, "nav_workout"),
    HISTORY("Histórico", Icons.Default.History, "nav_dossier"),
    VOLUME("Volume", Icons.Default.BarChart, "nav_volume"),
    CARDIO("Cardio", Icons.Default.DirectionsRun, "nav_cardio")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MutantViewModel by viewModels()
    private var requestedNotificationPermission = false
    private val notificationPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    private val workoutNavigationRequests = MutableSharedFlow<Unit>(replay = 1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleRestTimerIntent(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeWorkoutSession.collect { session ->
                    if (session != null && !requestedNotificationPermission && android.os.Build.VERSION.SDK_INT >= 33 &&
                        !RestTimerAlerts.hasNotificationPermission(this@MainActivity)) {
                        requestedNotificationPermission = true
                        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MutantApp(viewModel = viewModel, workoutNavigationRequests = workoutNavigationRequests)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRestTimerIntent(intent)
    }

    private fun handleRestTimerIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(com.example.ui.components.WorkoutTimerService.OPEN_FINISH_EXTRA, false) == true) {
            viewModel.requestFinishWorkout()
            intent.removeExtra(com.example.ui.components.WorkoutTimerService.OPEN_FINISH_EXTRA)
        }
        val action = intent?.getStringExtra(RestTimerAlerts.TIMER_ACTION_EXTRA)
        when (action) {
            RestTimerAlerts.ACTION_ADD_TIME -> {
                val seconds = intent.getIntExtra(RestTimerAlerts.TIMER_SECONDS_EXTRA, 30)
                if (viewModel.activeWorkoutUiState.value.restTimerRemainingSeconds <= 0) {
                    viewModel.startRestTimer(seconds)
                } else {
                    viewModel.adjustRestTimer(seconds)
                }
            }
            RestTimerAlerts.ACTION_SKIP -> viewModel.skipRestTimer()
        }
        if (intent?.getBooleanExtra(RestTimerAlerts.OPEN_WORKOUT_EXTRA, false) == true || action != null) {
            workoutNavigationRequests.tryEmit(Unit)
        }
    }
}

@Composable
fun MutantApp(viewModel: MutantViewModel, workoutNavigationRequests: MutableSharedFlow<Unit>) {
    val draft by viewModel.workoutDraft.collectAsState()
    var currentScreen by remember { mutableStateOf(NavDestination.HOME) }
    val activeSession by viewModel.activeWorkoutSession.collectAsState(initial = null)

    LaunchedEffect(workoutNavigationRequests) {
        workoutNavigationRequests.collect { currentScreen = NavDestination.WORKOUT }
    }

    // BackHandler: return to HOME if currently on sub-screens
    if (currentScreen != NavDestination.HOME) {
        BackHandler {
            currentScreen = NavDestination.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MutantBlack,
        bottomBar = {
            val hideBottomNav = currentScreen == NavDestination.WORKOUT && activeSession != null
            if (!hideBottomNav) {
                NavigationBar(
                    containerColor = MutantPalette.SurfaceNormal,
                    tonalElevation = 4.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("mutant_bottom_bar")
                ) {
                    NavDestination.values().forEach { destination ->
                        val isSelected = currentScreen == destination
                        val hasActiveSessionBadge = destination == NavDestination.WORKOUT && activeSession != null

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = destination },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (hasActiveSessionBadge) {
                                            Badge(
                                                containerColor = MutantPalette.PrimaryPurple,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.onPrimary)
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title,
                                        tint = if (isSelected) MutantPalette.PrimaryPurple else MutantPalette.TextSecondary
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = MutantTypeScale.label
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MutantPalette.PrimaryPurple,
                                selectedTextColor = MutantPalette.PrimaryPurple,
                                unselectedIconColor = MutantPalette.TextSecondary,
                                unselectedTextColor = MutantPalette.TextMetadata,
                                indicatorColor = MutantPalette.PrimaryContainer
                            ),
                            modifier = Modifier.testTag(destination.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { MutantMotion.ScreenFadeThroughSpec },
                label = "ScreenFadeThrough"
            ) { destination ->
                when (destination) {
                    NavDestination.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToActiveWorkout = { currentScreen = NavDestination.WORKOUT }
                    )
                    NavDestination.WORKOUT -> ActiveWorkoutScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = NavDestination.HOME }
                    )
                    NavDestination.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = NavDestination.HOME }
                    )
                    NavDestination.VOLUME -> VolumeAnalyticsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = NavDestination.HOME }
                    )
                    NavDestination.CARDIO -> CardioScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = NavDestination.HOME }
                    )
                }
            }
        }
    }
    if (draft != null) {
        CompletedWorkoutEditor(viewModel)
    }
}
