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
import com.example.data.db.plannedRir
import com.example.data.db.repMax
import com.example.data.db.repMin
import com.example.data.db.plannedSets
import com.example.data.db.isAdHoc
import com.example.data.db.workSetCount
import com.example.data.model.*
import com.example.data.repository.AnalysisExportRepository
import com.example.data.repository.AnalysisExportRepositoryImpl
import com.example.data.repository.BackupImportRepository
import com.example.data.repository.ImportException
import com.example.data.repository.ImportMode
import com.example.data.repository.ImportPreview
import com.example.data.repository.ImportResult
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SystemStatus(
    val todayWorkoutTitle: String = "No active program",
    val todayDayCode: String = "SEG",
    val lastSessionTitle: String = "No workouts yet",
    val lastSessionDaysAgo: Int = 0,
    val recoveryDaysText: String = "N/A",
    val fatigueStatus: String = "Normal",
    val nextScheduledTitle: String = "Create program to schedule",
    val recommendedProgramDayId: Long? = null,
    val recommendationBasis: RecommendationBasis = RecommendationBasis.UNAVAILABLE,
    val hasProgram: Boolean = false,
    val lastWorkoutTitle: String? = null,
    val lastWorkoutDaysAgo: Int? = null,
    val lastReadinessScore: Int? = null
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
    val restTimerRecommended: String = "2–3 min",
    val restTimerTotalSeconds: Int = 0,
    // Work sets from the last finished session of the current exercise.
    val previousWorkSets: List<WorkoutSet> = emptyList(),
    // The same for the exercise after the current one, for the Up next card.
    val nextProgression: ProgressionRecommendation? = null,
    val nextPreviousWorkSets: List<WorkoutSet> = emptyList()
)

/** Import flow shown by the settings sheet. */
sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Reading : ImportUiState
    data class Preview(val preview: ImportPreview) : ImportUiState
    data class Running(val mode: ImportMode) : ImportUiState
    data class Done(val result: ImportResult, val mode: ImportMode, val safetyBackup: File?) : ImportUiState
    data class Error(val message: String) : ImportUiState
}

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
    val programDaySummaries = repository.programDaySummaries
        .map { rows -> rows.associateBy { it.programDayId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val historyReload = MutableStateFlow(0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val workoutHistory = historyReload.flatMapLatest {
        repository.workoutHistory.map { HistoryUiState(workouts = it, loading = false) }
            .onStart { emit(HistoryUiState()) }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(HistoryUiState(loading = false, error = "Não foi possível carregar o histórico."))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun reloadHistory() { historyReload.update { it + 1 } }

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
                restTimerRecommended = session.restRecommended,
                restTimerTotalSeconds = maxOf(session.restTotalSeconds, remaining))
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
                    _activeWorkoutUiState.update {
                        val sameExercise = it.session?.id == session.id && it.currentExerciseIndex == session.currentExerciseIndex
                        it.copy(session = session, currentExerciseIndex = session.currentExerciseIndex,
                            isLoading = it.isLoading || it.session?.id != session.id,
                            exercises = if (it.session?.id == session.id) it.exercises else emptyList(),
                            currentProgression = if (sameExercise) it.currentProgression else null,
                            previousWorkSets = if (sameExercise) it.previousWorkSets else emptyList(),
                            nextProgression = if (sameExercise) it.nextProgression else null,
                            nextPreviousWorkSets = if (sameExercise) it.nextPreviousWorkSets else emptyList())
                    }
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
            // Double progression reads the last finished session, never the one in progress.
            val recommendation = repository.getProgressionSuggestion(
                exerciseId = currentEx.id,
                repMin = currentExDetail.repMin,
                repMax = currentExDetail.repMax,
                targetRir = currentExDetail.plannedRir,
                incrementKg = currentEx.defaultIncrementKg
            )
            val previous = repository.getPreviousWorkSets(currentEx.id)
            val nextDetail = state.exercises.getOrNull(state.currentExerciseIndex + 1)
            val nextRecommendation = nextDetail?.let { next ->
                repository.getProgressionSuggestion(
                    exerciseId = next.exercise.id,
                    repMin = next.repMin,
                    repMax = next.repMax,
                    targetRir = next.plannedRir,
                    incrementKg = next.exercise.defaultIncrementKg
                )
            }
            val nextPrevious = nextDetail?.let { repository.getPreviousWorkSets(it.exercise.id) } ?: emptyList()
            _activeWorkoutUiState.update {
                if (it.session?.id == state.session?.id && it.currentExerciseIndex == state.currentExerciseIndex) {
                    it.copy(currentProgression = recommendation, previousWorkSets = previous,
                        nextProgression = nextRecommendation, nextPreviousWorkSets = nextPrevious)
                } else it
            }
        }
    }

    // --- PROGRAM WORKFLOWS ---
    private val _isAdoptingTemplate = MutableStateFlow(false)
    val isAdoptingTemplate = _isAdoptingTemplate.asStateFlow()

    fun adoptNickWalkerTemplate(onDone: () -> Unit = {}) {
        if (!_isAdoptingTemplate.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                repository.adoptNickWalkerTemplate()
                refreshLibraryCounts()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isAdoptingTemplate.value = false
            }
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

    private val _isFinishingWorkout = MutableStateFlow(false)
    val isFinishingWorkout = _isFinishingWorkout.asStateFlow()

    fun finishWorkout(notes: String, bodyweight: Float, onError: (String) -> Unit = {}, onFinished: () -> Unit = {}) {
        val session = _activeWorkoutUiState.value.session ?: return
        if (!_isFinishingWorkout.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                workoutMutationMutex.withLock {
                    repository.finishWorkout(session.id, notes, bodyweight)
                    stopRestTimer()
                    refreshLibraryCounts()
                }
                onFinished()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("Could not save the workout. Your sets are still here; try again.")
            } finally {
                _isFinishingWorkout.value = false
            }
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

    // --- TODAY-ONLY SESSION EDITS (the program is untouched) ---
    private var lastRemovedExercise: com.example.data.db.RemovedSessionExercise? = null

    fun addExerciseToSession(exerciseId: Long, afterCurrent: Boolean, onAdded: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val state = _activeWorkoutUiState.value
        val session = state.session ?: return
        val position = if (afterCurrent) state.currentExerciseIndex + 1 else state.exercises.size
        launchWorkoutMutation {
            try {
                repository.addExerciseToSession(session.id, exerciseId, position)
                onAdded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("Could not add the exercise.")
            }
        }
    }

    /** [onRemoved] runs on success; an Undo can then call [undoRemoveExercise]. */
    fun removeExerciseFromSession(workoutExerciseId: Long, onRemoved: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val session = _activeWorkoutUiState.value.session ?: return
        launchWorkoutMutation {
            try {
                lastRemovedExercise = repository.removeExerciseFromSession(session.id, workoutExerciseId)
                onRemoved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: "Could not remove the exercise.")
            }
        }
    }

    fun undoRemoveExercise(onError: (String) -> Unit = {}) {
        val removed = lastRemovedExercise ?: return
        lastRemovedExercise = null
        launchWorkoutMutation {
            try {
                if (_activeWorkoutUiState.value.session?.id == removed.sessionId) repository.restoreSessionExercise(removed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("Could not restore the exercise.")
            }
        }
    }

    // Set edits keep a single undo step, like exercise removal.
    private var lastSetUndo: (suspend () -> Unit)? = null

    /** Deletes a logged set; [onRemoved] runs on success and [undoSetChange] puts it back. */
    fun removeLoggedSet(setId: Long, onRemoved: () -> Unit = {}, onError: (String) -> Unit = {}) {
        launchWorkoutMutation {
            try {
                val removed = repository.removeLoggedSet(setId)
                lastSetUndo = { repository.restoreLoggedSet(removed) }
                refreshLibraryCounts()
                onRemoved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("Could not remove the set.")
            }
        }
    }

    /** Drops one not-yet-logged set from today's plan. Keeps at least one set and every logged one. */
    fun removePlannedSet(workoutExerciseId: Long, onRemoved: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val detail = _activeWorkoutUiState.value.exercises.firstOrNull { it.workoutExercise.id == workoutExerciseId } ?: return
        val planned = detail.plannedSets
        if (detail.isAdHoc || planned <= maxOf(1, detail.workSetCount)) return
        launchWorkoutMutation {
            try {
                repository.setSessionTargetSets(workoutExerciseId, planned - 1)
                lastSetUndo = { repository.setSessionTargetSets(workoutExerciseId, planned) }
                onRemoved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("Could not remove the set.")
            }
        }
    }

    /** Adds one set to today's plan for this exercise. */
    fun addPlannedSet(workoutExerciseId: Long, onError: (String) -> Unit = {}) {
        val detail = _activeWorkoutUiState.value.exercises.firstOrNull { it.workoutExercise.id == workoutExerciseId } ?: return
        if (detail.isAdHoc) return
        val sets = maxOf(detail.plannedSets, detail.workSetCount) + 1
        launchWorkoutMutation {
            try {
                repository.setSessionTargetSets(workoutExerciseId, sets)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("Could not add a set.")
            }
        }
    }

    fun undoSetChange(onError: (String) -> Unit = {}) {
        val undo = lastSetUndo ?: return
        lastSetUndo = null
        launchWorkoutMutation {
            try {
                undo()
                refreshLibraryCounts()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("Could not undo.")
            }
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

    fun logCardio(
        machine: String, durationMinutes: Int, level: Int, avgHeartRate: Int, rpe: Int, notes: String = "",
        onSaved: () -> Unit = {}, onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
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
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("Could not save cardio. Try again.")
            }
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
            null -> "No history"
            0 -> "Same day"
            1 -> "1 day"
            else -> "$daysAgo days"
        }

        val fatigueSignal = when {
            lastWorkout?.readinessStatus?.contains("fatigue", ignoreCase = true) == true -> "High fatigue"
            lastWorkout?.jointDiscomfort in listOf("Moderate", "Severe") -> "Joint caution"
            lastWorkout?.readinessScore?.let { readinessBand(it) } == ReadinessBand.MODERATE -> "Moderate"
            daysAgo != null && daysAgo >= 3 -> "Fully recovered"
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
            hasProgram = true,
            lastWorkoutTitle = recommendation.lastWorkoutTitle,
            lastWorkoutDaysAgo = daysAgo,
            lastReadinessScore = lastWorkout?.readinessScore
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

    fun exportFileName(): String = "mutant-log-analysis-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json"

    fun shareExportFile(context: Context, onError: (String) -> Unit = {}, onComplete: (() -> Unit)? = null) {
        if (_importState.value is ImportUiState.Running) return
        viewModelScope.launch(Dispatchers.IO) {
            isExporting.value = true
            try {
                val exportData = exportRepository.buildExport()
                val jsonString = jsonSerializer.encodeToString(AnalysisExport.serializer(), exportData)
                val fileName = exportFileName()
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
                        putExtra(Intent.EXTRA_TEXT, "Mutant Log raw data export for analysis ($fileName)")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Export data for analysis").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    onComplete?.invoke()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Export failed. Check free storage and try again.") }
            } finally {
                isExporting.value = false
            }
        }
    }

    // --- IMPORT / RESTORE ---
    private val importRepository = BackupImportRepository(database)
    private val _importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    /** Parses the picked file off the main thread and shows what it contains. */
    fun previewImport(uri: Uri) {
        if (isExporting.value || _importState.value is ImportUiState.Running) return
        _importState.value = ImportUiState.Reading
        viewModelScope.launch(Dispatchers.IO) {
            _importState.value = try {
                val resolver = getApplication<Application>().contentResolver
                val size = resolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
                }
                val stream = resolver.openInputStream(uri) ?: throw ImportException("Could not open that file.")
                ImportUiState.Preview(stream.use { importRepository.readPreview(it, size) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: ImportException) {
                ImportUiState.Error(e.message ?: "Import failed.")
            } catch (e: Exception) {
                ImportUiState.Error("Could not read that file.")
            }
        }
    }

    fun confirmImport(mode: ImportMode) {
        val preview = (_importState.value as? ImportUiState.Preview)?.preview ?: return
        _importState.value = ImportUiState.Running(mode)
        viewModelScope.launch(Dispatchers.IO) {
            _importState.value = try {
                // Serialized with workout mutations; the repository also refuses while a session is active.
                workoutMutationMutex.withLock {
                    val safety = if (mode == ImportMode.REPLACE) writeSafetyBackup() else null
                    val result = importRepository.apply(preview, mode)
                    val previous = _selectedGym.value
                    val gyms = dao.getEveryGymSync().filter { !it.isArchived }
                    _selectedGym.value = gyms.firstOrNull { it.name == previous?.name } ?: gyms.firstOrNull()
                    ImportUiState.Done(result, mode, safety)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ImportException) {
                ImportUiState.Error(e.message ?: "Import failed.")
            } catch (e: Exception) {
                ImportUiState.Error("Import failed. Nothing was changed.")
            }
            refreshLibraryCounts()
        }
    }

    fun dismissImport() {
        if (_importState.value !is ImportUiState.Running) _importState.value = ImportUiState.Idle
    }

    /** Full export written before a Replace, so the previous data can still be recovered. Keeps the newest 3. */
    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun writeSafetyBackup(): File {
        val dir = File(getApplication<Application>().filesDir, "backups").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "mutant-log-before-import-$stamp.json")
        val export = exportRepository.buildExport()
        file.outputStream().buffered().use { jsonSerializer.encodeToStream(AnalysisExport.serializer(), export, it) }
        dir.listFiles { f -> f.name.startsWith("mutant-log-before-import-") }
            ?.sortedByDescending { it.name }?.drop(SAFETY_BACKUPS_KEPT)?.forEach { it.delete() }
        return file
    }

    fun shareSafetyBackup(context: Context, file: File) {
        val app = getApplication<Application>()
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Mutant Log safety backup — ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Save safety backup"))
    }

    private companion object {
        const val SAFETY_BACKUPS_KEPT = 3
    }
}
