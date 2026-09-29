package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.MutantDatabase
import com.example.data.db.ProgramExerciseDetail
import com.example.data.db.WorkoutExerciseDetail
import com.example.data.model.*
import com.example.data.repository.AnalysisExportRepository
import com.example.data.repository.AnalysisExportRepositoryImpl
import com.example.data.repository.LibraryCounts
import com.example.data.repository.MutantRepository
import com.example.data.repository.ReadinessInput
import kotlinx.coroutines.Dispatchers
import com.example.ui.components.RestTimerAlerts
import com.example.ui.components.WorkoutTimerService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SystemStatus(
    val todayWorkoutTitle: String = "No active program",
    val todayDayCode: String = "SEG",
    val lastSessionTitle: String = "nenhum treino concluído",
    val lastSessionDaysAgo: Int = 0,
    val recoveryDaysText: String = "N/A",
    val fatigueStatus: String = "Normal",
    val nextScheduledTitle: String = "Create program to schedule",
    val recommendedProgramDayId: Long? = null,
    val recommendationBasis: RecommendationBasis = RecommendationBasis.UNAVAILABLE,
    val hasProgram: Boolean = false
)

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val session: WorkoutSession? = null,
    val exercises: List<WorkoutExerciseDetail> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val currentProgression: ProgressionRecommendation? = null,
    val elapsedSeconds: Long = 0,
    val restTimerRemainingSeconds: Int = 0,
    val isRestTimerRunning: Boolean = false,
    val restTimerCompleted: Boolean = false,
    val restTimerRecommended: String = "2–3 min"
)

class MutantViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MutantDatabase.getDatabase(application)
    val repository = MutantRepository(database.mutantDao())

    // Reference library flows
    val allExercises = repository.allExercises
    val allMachineCatalog = repository.allMachineCatalog
    val allVariants = repository.allVariants
    val allMuscleGroups = repository.allMuscleGroups
    val allGyms = repository.allGyms

    // User data flows
    val activeProgram = repository.activeProgram
    val allPrograms = repository.allPrograms
    val programDays = repository.allProgramDays
    val activeWorkoutSession = repository.activeWorkoutSession
    val finishedWorkouts = repository.finishedWorkouts
    val lastFinishedWorkout = repository.lastFinishedWorkout
    val lastFinishedRoutineWorkout = repository.lastFinishedRoutineWorkout
    val cardioSessions = repository.allCardioSessions

    private val _libraryCounts = MutableStateFlow(LibraryCounts())
    val libraryCounts: StateFlow<LibraryCounts> = _libraryCounts.asStateFlow()

    private val _selectedGym = MutableStateFlow<Gym?>(null)
    val selectedGym: StateFlow<Gym?> = _selectedGym.asStateFlow()

    private val _activeWorkoutUiState = MutableStateFlow(ActiveWorkoutUiState())
    val activeWorkoutUiState: StateFlow<ActiveWorkoutUiState> = _activeWorkoutUiState.asStateFlow()

    private val _systemStatus = MutableStateFlow(SystemStatus())
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()
    private val _deviceNowMillis = MutableStateFlow(System.currentTimeMillis())

    private val dao = database.mutantDao()
    private val _workoutDraft = MutableStateFlow<WorkoutDraft?>(null)
    val workoutDraft = _workoutDraft.asStateFlow()
    val editError = MutableStateFlow<String?>(null)
    val isSavingWorkout = MutableStateFlow(false)
    val isLoadingWorkout = MutableStateFlow(false)

    fun editWorkout(id: Long) {
        if (isLoadingWorkout.value || isSavingWorkout.value) return
        isLoadingWorkout.value = true
        editError.value = null
        viewModelScope.launch {
            try { _workoutDraft.value = dao.loadWorkoutDraft(id) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { editError.value = "Could not open workout. Try again." }
            finally { isLoadingWorkout.value = false }
        }
    }
    fun updateWorkoutDraft(draft: WorkoutDraft) { if (!isSavingWorkout.value) _workoutDraft.value = draft }
    fun cancelWorkoutEdit() { if (!isSavingWorkout.value) { _workoutDraft.value = null; editError.value = null } }
    fun saveWorkoutEdit() {
        val draft = _workoutDraft.value ?: return
        if (!isSavingWorkout.compareAndSet(false, true)) return
        editError.value = null
        viewModelScope.launch {
            try { dao.saveWorkoutDraft(draft); _workoutDraft.value = null; refreshLibraryCounts() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { editError.value = e.message ?: "Could not save changes. Try again." }
            finally { isSavingWorkout.value = false }
        }
    }

    private fun refreshClock() {
        _activeWorkoutUiState.update { state ->
            val session = state.session ?: return@update state
            val now = System.currentTimeMillis()
            val remaining = session.restSecondsAt(now)
            state.copy(elapsedSeconds = ((now - session.startedAt) / 1000).coerceAtLeast(0),
                restTimerRemainingSeconds = remaining, isRestTimerRunning = session.restDeadline != null && remaining > 0,
                restTimerCompleted = session.restCompleted || (session.restDeadline != null && remaining == 0),
                restTimerRecommended = session.restRecommended)
        }
    }
    private var notifiedSessionId: Long? = null
    private var exerciseObserverJob: Job? = null
    private val workoutMutationMutex = Mutex()
    private var discardedSessionId: Long? = null
    private val _isDiscardingWorkout = MutableStateFlow(false)
    val isDiscardingWorkout = _isDiscardingWorkout.asStateFlow()
    private val _discardError = MutableStateFlow<String?>(null)
    val discardError = _discardError.asStateFlow()

    fun clearDiscardError() { _discardError.value = null }

    private fun launchWorkoutMutation(block: suspend () -> Unit) {
        if (_isDiscardingWorkout.value || _activeWorkoutUiState.value.session == null) return
        val sessionId = _activeWorkoutUiState.value.session?.id
        viewModelScope.launch {
            workoutMutationMutex.withLock {
                if (!_isDiscardingWorkout.value && _activeWorkoutUiState.value.session?.id == sessionId) block()
            }
        }
    }

    fun discardWorkout(onDiscarded: () -> Unit) {
        val session = _activeWorkoutUiState.value.session ?: return
        if (!_isDiscardingWorkout.compareAndSet(false, true)) return
        _discardError.value = null
        viewModelScope.launch {
            try {
                workoutMutationMutex.withLock {
                    check(repository.discardWorkout(session.id)) { "Session is no longer active" }
                    discardedSessionId = session.id
                    exerciseObserverJob?.cancel()
                    stopRestTimer()
                    RestTimerAlerts.clearSession(getApplication())
                    _activeWorkoutUiState.value = ActiveWorkoutUiState(isLoading = false)
                }
                refreshLibraryCounts()
                onDiscarded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _discardError.value = "Could not discard workout. Please try again."
            } finally {
                _isDiscardingWorkout.value = false
            }
        }
    }

    init {
        // One UI clock for all screens; values always derive from persisted timestamps.
        viewModelScope.launch {
            while (true) {
                _deviceNowMillis.value = System.currentTimeMillis()
                refreshClock()
                delay(1000)
            }
        }
        // Preload canonical reference catalog (124 exercises, 186 machines, 15 muscle groups)
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            refreshLibraryCounts()
        }

        // Initialize default gym selection
        viewModelScope.launch {
            allGyms.collect { list ->
                if (_selectedGym.value == null && list.isNotEmpty()) {
                    _selectedGym.value = list.first()
                }
            }
        }

        // Recompute from persisted routine history and the device clock only.
        viewModelScope.launch {
            combine(activeProgram, lastFinishedRoutineWorkout, programDays, _deviceNowMillis) { program, lastSession, days, now ->
                computeSystemStatus(program, lastSession, days, now)
            }.collect { status ->
                _systemStatus.value = status
            }
        }

        // Monitor active workout session
        viewModelScope.launch {
            activeWorkoutSession.collect { session ->
                if (session == null) {
                    notifiedSessionId = null
                    exerciseObserverJob?.cancel()
                    stopRestTimer()
                    RestTimerAlerts.clearSession(getApplication())
                    _activeWorkoutUiState.value = ActiveWorkoutUiState(isLoading = false)
                } else {
                    if (session.id == discardedSessionId) return@collect
                    discardedSessionId = null
                    _activeWorkoutUiState.update { it.copy(session = session, currentExerciseIndex = session.currentExerciseIndex,
                        isLoading = it.isLoading || it.session?.id != session.id,
                        exercises = if (it.session?.id == session.id) it.exercises else emptyList()) }
                    refreshClock()
                    if (notifiedSessionId != session.id) {
                        notifiedSessionId = session.id
                        androidx.core.content.ContextCompat.startForegroundService(getApplication(),
                            Intent(getApplication(), com.example.ui.components.WorkoutTimerService::class.java))
                    }
                    observeActiveWorkoutExercises(session.id)
                }
            }
        }
    }

    fun refreshLibraryCounts() {
        viewModelScope.launch {
            _libraryCounts.value = repository.getLibraryCounts()
        }
    }

    private fun observeActiveWorkoutExercises(sessionId: Long) {
        exerciseObserverJob?.cancel()
        exerciseObserverJob = viewModelScope.launch {
            repository.getWorkoutExercisesWithDetails(sessionId).collect { list ->
                _activeWorkoutUiState.update { current ->
                    if (current.session?.id != sessionId) return@update current
                    val safeIndex = current.currentExerciseIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
                    current.copy(exercises = list, currentExerciseIndex = safeIndex, isLoading = false)
                }
                updateProgressionForCurrentExercise()
            }
        }
    }

    fun selectGym(gym: Gym) {
        _selectedGym.value = gym
    }

    fun setCurrentExerciseIndex(index: Int) {
        val session = _activeWorkoutUiState.value.session ?: return
        launchWorkoutMutation { dao.selectExercise(session.id, index.coerceAtLeast(0)) }
    }

    fun updateProgressionForCurrentExercise() {
        val state = _activeWorkoutUiState.value
        val currentExDetail = state.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val currentEx = currentExDetail.exercise

        viewModelScope.launch {
            val recommendation = ProgressionEngine.computeProgression(
                lastWorkSets = currentExDetail.sets.filter { it.setType == SetType.WORK },
                repMin = currentEx.defaultRepMin,
                repMax = currentEx.defaultRepMax,
                targetRir = currentEx.defaultRir,
                incrementKg = currentEx.defaultIncrementKg,
                lastExecutionQuality = currentExDetail.workoutExercise.executionQuality,
                lastTargetMuscleQuality = currentExDetail.workoutExercise.targetMuscleQuality
            )
            _activeWorkoutUiState.update {
                if (it.session?.id == state.session?.id && it.currentExerciseIndex == state.currentExerciseIndex) {
                    it.copy(currentProgression = recommendation)
                } else it
            }
        }
    }

    // --- PROGRAM WORKFLOWS ---
    fun adoptNickWalkerTemplate() {
        viewModelScope.launch {
            repository.adoptNickWalkerTemplate()
            refreshLibraryCounts()
        }
    }

    fun createCustomProgram(name: String, description: String = "", days: List<ProgramDay>) {
        viewModelScope.launch {
            repository.createProgram(name, description, days)
            refreshLibraryCounts()
        }
    }

    fun addExerciseToProgram(
        dayId: Long,
        exerciseId: Long,
        variantId: String = "",
        sets: Int = 2,
        repMin: Int = 10,
        repMax: Int = 12,
        rir: Int = 0,
        rest: Int = 180
    ) {
        viewModelScope.launch {
            repository.addProgramExercise(
                programDayId = dayId,
                exerciseId = exerciseId,
                variantId = variantId,
                targetSets = sets,
                repMin = repMin,
                repMax = repMax,
                targetRir = rir,
                restSeconds = rest
            )
        }
    }

    fun createCustomVariant(
        exerciseId: Long,
        exerciseStableId: String,
        variantName: String,
        manufacturer: String,
        resistanceType: String,
        incrementKg: Float = 2.5f,
        seat: String = "",
        handle: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.createCustomVariant(
                exerciseId = exerciseId,
                exerciseStableId = exerciseStableId,
                variantName = variantName,
                manufacturer = manufacturer,
                resistanceType = resistanceType,
                incrementKg = incrementKg,
                seat = seat,
                handle = handle,
                notes = notes
            )
            refreshLibraryCounts()
        }
    }

    // --- WORKOUT ACTIONS ---
    private val _isStartingWorkout = MutableStateFlow(false)
    val isStartingWorkout = _isStartingWorkout.asStateFlow()
    private val _startWorkoutError = MutableStateFlow<String?>(null)
    val startWorkoutError = _startWorkoutError.asStateFlow()

    fun startWorkout(programDay: ProgramDay, readiness: ReadinessInput, onStarted: () -> Unit = {}) {
        createWorkout(onStarted) { gymId -> repository.startWorkoutSession(programDay, gymId, readiness) }
    }

    fun startAdHocWorkout(title: String, exerciseIds: List<Long>, readiness: ReadinessInput, onStarted: () -> Unit = {}) {
        createWorkout(onStarted) { gymId -> repository.startAdHocWorkoutSession(title, gymId, exerciseIds, readiness) }
    }

    private fun createWorkout(onStarted: () -> Unit, create: suspend (Long) -> Long) {
        if (!_isStartingWorkout.compareAndSet(false, true)) return
        _startWorkoutError.value = null
        viewModelScope.launch {
            try {
                val id = create(_selectedGym.value?.id ?: 1L)
                // Room has restored the session and its exercise list before navigation.
                activeWorkoutUiState.first { it.session?.id == id && !it.isLoading }
                refreshLibraryCounts()
                onStarted()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _startWorkoutError.value = "Could not start workout. Please try again."
            } finally {
                _isStartingWorkout.value = false
            }
        }
    }

    private val _finishRequested = MutableStateFlow(false)
    val finishRequested = _finishRequested.asStateFlow()
    fun requestFinishWorkout() { _finishRequested.value = true }
    fun consumeFinishRequest() { _finishRequested.value = false }

    fun finishWorkout(notes: String, bodyweight: Float) {
        val session = _activeWorkoutUiState.value.session ?: return
        launchWorkoutMutation {
            repository.finishWorkout(session.id, notes, bodyweight)
            stopRestTimer()
            refreshLibraryCounts()
        }
    }

    fun logSet(
        workoutExerciseId: Long,
        type: SetType,
        weightKg: Float,
        reps: Int,
        rir: Int,
        technique: IntensityTechnique,
        segments: List<SetSegment> = emptyList(),
        defaultRestSeconds: Int = 180,
        muscleGroup: String = "Chest"
    ) {
        launchWorkoutMutation {
            repository.logSet(
                workoutExerciseId = workoutExerciseId,
                setType = type,
                weightKg = weightKg,
                reps = reps,
                rir = rir,
                technique = technique,
                segments = segments
            )
            refreshLibraryCounts()

            // Auto-start rest timer
            val recommendedText = if (muscleGroup.equals("Quads", ignoreCase = true) ||
                muscleGroup.equals("Hamstrings", ignoreCase = true) ||
                muscleGroup.equals("Legs", ignoreCase = true)
            ) {
                "Recommended: 3–5 min"
            } else {
                "Recommended: 2–3 min"
            }
            startRestTimer(defaultRestSeconds, recommendedText)
        }
    }

    fun deleteSet(setId: Long) {
        launchWorkoutMutation {
            repository.deleteSet(setId)
            refreshLibraryCounts()
        }
    }

    fun updateExerciseDetails(
        weId: Long,
        executionQuality: String,
        targetMuscleQuality: String,
        seatPosition: String,
        handlePosition: String,
        notes: String
    ) {
        launchWorkoutMutation {
            repository.updateWorkoutExercise(
                workoutExerciseId = weId,
                executionQuality = executionQuality,
                targetMuscleQuality = targetMuscleQuality,
                seatPosition = seatPosition,
                handlePosition = handlePosition,
                notes = notes
            )
        }
    }

    // Timer commands mutate the same persisted record used by the notification service.
    fun startRestTimer(seconds: Int, recommended: String = "2-3 min") {
        launchWorkoutMutation { dao.changeRestTimer("start", seconds, recommended) }
    }
    fun toggleRestTimer() { launchWorkoutMutation { dao.changeRestTimer("toggle") } }
    fun adjustRestTimer(deltaSeconds: Int) { launchWorkoutMutation { dao.changeRestTimer("adjust", deltaSeconds) } }
    fun skipRestTimer() { launchWorkoutMutation { dao.changeRestTimer("skip") } }
    fun updateNextSet(exerciseId: Long, weight: Float, reps: Int) {
        if (!weight.isFinite() || weight < 0 || reps <= 0) return
        launchWorkoutMutation { dao.updateNextSet(exerciseId, weight, reps) }
    }
    private fun stopRestTimer() {
        _activeWorkoutUiState.update { it.copy(restTimerRemainingSeconds = 0, isRestTimerRunning = false, restTimerCompleted = false) }

    }

    fun logCardio(machine: String, durationMinutes: Int, level: Int, avgHeartRate: Int, rpe: Int, notes: String = "") {
        viewModelScope.launch {
            repository.logCardio(
                CardioSession(
                    machine = machine,
                    durationMinutes = durationMinutes,
                    level = level,
                    avgHeartRate = avgHeartRate,
                    rpe = rpe,
                    notes = notes
                )
            )
            refreshLibraryCounts()
        }
    }

    private fun computeSystemStatus(
        activeProgram: Program?,
        lastWorkout: WorkoutSession?,
        days: List<ProgramDay>,
        nowMillis: Long
    ): SystemStatus {
        if (days.isEmpty()) {
            return SystemStatus(
                todayWorkoutTitle = "No active program",
                nextScheduledTitle = "Create program to schedule"
            )
        }

        val recommendation = WorkoutRecommendationEngine.recommend(
            programDays = days,
            lastWorkout = lastWorkout,
            persistedPosition = activeProgram?.currentRoutinePosition ?: -1,
            nowMillis = nowMillis,
            timeZone = TimeZone.getDefault()
        )
        val daysAgo = recommendation.daysSinceLastWorkout
        val recoveryText = when (daysAgo) {
            null -> "Sem histórico"
            0 -> "Hoje mesmo"
            1 -> "1 dia completo"
            else -> "$daysAgo dias completos"
        }

        val fatigueSignal = when {
            lastWorkout?.readinessStatus?.contains("fatigue", ignoreCase = true) == true -> "Fadiga detectada"
            lastWorkout?.jointDiscomfort in listOf("Moderate", "Severe") -> "Cuidado articular"
            daysAgo != null && daysAgo >= 3 -> "Totalmente recuperado"
            else -> "Normal"
        }

        return SystemStatus(
            todayWorkoutTitle = recommendation.title,
            todayDayCode = recommendation.dayCode,
            lastSessionTitle = recommendation.lastWorkoutSummary,
            lastSessionDaysAgo = daysAgo ?: 0,
            recoveryDaysText = recoveryText,
            fatigueStatus = fatigueSignal,
            nextScheduledTitle = recommendation.title,
            recommendedProgramDayId = recommendation.programDayId,
            recommendationBasis = recommendation.basis,
            hasProgram = true
        )
    }

    // --- DATA EXPORT FOR ANALYSIS ---
    private val exportRepository: AnalysisExportRepository = AnalysisExportRepositoryImpl(database.mutantDao())
    private val jsonSerializer = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val isExporting = MutableStateFlow(false)

    fun shareExportFile(context: Context, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            isExporting.value = true
            try {
                val exportData = exportRepository.buildExport()
                val jsonString = jsonSerializer.encodeToString(AnalysisExport.serializer(), exportData)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val fileName = "mutant-log-analysis-${dateFormat.format(Date())}.json"
                val exportFile = File(context.cacheDir, fileName)
                exportFile.writeText(jsonString)

                withContext(Dispatchers.Main) {
                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        exportFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Mutant Log Analysis — $fileName")
                        putExtra(Intent.EXTRA_TEXT, "Exportação de dados brutos para análise do Mutant Log ($fileName)")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Exportar dados para análise").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isExporting.value = false
            }
        }
    }
}
