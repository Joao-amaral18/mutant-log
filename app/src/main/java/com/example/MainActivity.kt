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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
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
import com.example.ui.designsystem.MutantType
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MutantViewModel
import com.example.ui.components.RestTimerAlerts
import kotlinx.coroutines.flow.MutableSharedFlow

enum class NavDestination(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Protocol", Icons.Outlined.CalendarToday, Icons.Filled.CalendarToday, "nav_home"),
    WORKOUT("Session", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter, "nav_workout"),
    HISTORY("History", Icons.Outlined.History, Icons.Filled.History, "nav_dossier"),
    CARDIO("Cardio", Icons.AutoMirrored.Outlined.DirectionsRun, Icons.AutoMirrored.Filled.DirectionsRun, "nav_cardio")
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

    val toastState = remember { com.example.ui.designsystem.components.MutantToastState() }
    CompositionLocalProvider(com.example.ui.designsystem.components.LocalMutantToast provides toastState) {
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MutantPalette.AppBackground,
        bottomBar = {
            val hideBottomNav = currentScreen == NavDestination.WORKOUT && activeSession != null
            if (!hideBottomNav) {
                MutantNavigationBar(
                    current = currentScreen,
                    hasActiveSession = activeSession != null,
                    onSelect = { currentScreen = it }
                )
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
                        onNavigateToActiveWorkout = { currentScreen = NavDestination.WORKOUT },
                        onNavigateToHistory = { currentScreen = NavDestination.HISTORY }
                    )
                    NavDestination.WORKOUT -> ActiveWorkoutScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = NavDestination.HOME },
                        onNavigateToHistory = { currentScreen = NavDestination.HISTORY }
                    )
                    NavDestination.HISTORY -> HistoryScreen(
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
    val isStartingWorkout by viewModel.isStartingWorkout.collectAsState()
    val isLoadingWorkout by viewModel.isLoadingWorkout.collectAsState()

    androidx.compose.animation.AnimatedVisibility(
        visible = draft != null,
        enter = androidx.compose.animation.slideInVertically(
            initialOffsetY = { it },
            animationSpec = androidx.compose.animation.core.tween(MutantMotion.Container, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ) + androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(MutantMotion.Navigation, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        ),
        exit = androidx.compose.animation.slideOutVertically(
            targetOffsetY = { it },
            animationSpec = androidx.compose.animation.core.tween(MutantMotion.State, easing = androidx.compose.animation.core.FastOutLinearInEasing)
        ) + androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(MutantMotion.Feedback, easing = androidx.compose.animation.core.FastOutLinearInEasing)
        )
    ) {
        CompletedWorkoutEditor(viewModel)
    }

    com.example.ui.designsystem.components.MutantLoadingOverlay(
        visible = isStartingWorkout,
        message = "Starting workout…",
        subMessage = "Setting up exercises and sets",
        testTag = "starting_workout_overlay"
    )

    com.example.ui.designsystem.components.MutantLoadingOverlay(
        visible = isLoadingWorkout,
        message = "Opening workout…",
        testTag = "loading_workout_overlay"
    )

    com.example.ui.designsystem.components.MutantToastHost(
        state = toastState,
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp)
    )
    }
    }
}

@Composable
private fun MutantNavigationBar(current: NavDestination, hasActiveSession: Boolean, onSelect: (NavDestination) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MutantPalette.SurfaceContainerLow).testTag("mutant_bottom_bar")) {
        HorizontalDivider(color = MutantPalette.SurfaceContainerHigh)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
        ) {
            NavDestination.entries.forEach { destination ->
                val selected = current == destination
                val tint = if (selected) MutantPalette.TextPrimary else MutantPalette.TextSecondary
                Column(
                    Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(destination) })
                        .testTag(destination.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
                ) {
                    Box(
                        Modifier
                            .size(width = 60.dp, height = 30.dp)
                            .background(if (selected) MutantPalette.PrimaryIndicator else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = null,
                            tint = tint, modifier = Modifier.size(22.dp))
                        if (destination == NavDestination.WORKOUT && hasActiveSession) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 3.dp, end = 14.dp)
                                    .size(7.dp)
                                    .background(MutantPalette.Primary, CircleShape)
                                    .testTag("nav_active_session_dot")
                            )
                        }
                    }
                    Text(destination.title, style = MutantType.Chip.copy(fontSize = 11.5.sp), color = tint)
                }
            }
        }
    }
}
