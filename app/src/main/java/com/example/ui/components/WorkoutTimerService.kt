package com.example.ui.components

import com.example.data.db.plannedSets
import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.WorkoutExerciseDetail
import com.example.data.db.MutantDatabase
import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/** Persisted deadlines drive the system chronometer; ticks refresh notification progress. */
class WorkoutTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dao by lazy { MutantDatabase.getDatabase(this).mutantDao() }
    private var observer: Job? = null
    private var ticker: Job? = null
    private val handler = Handler(Looper.getMainLooper())
    private var completionListener: AlarmManager.OnAlarmListener? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        RestTimerAlerts.prepare(this)
        // Satisfy the foreground-service launch deadline while Room restores the session.
        if (observer == null) publish(NotificationCompat.Builder(this, RestTimerAlerts.RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_workout_notification).setContentTitle("Workout in progress")
            .setOngoing(true).setContentIntent(activityIntent()).build())
        scope.launch {
            when (intent?.action) {
                ACTION_TOGGLE -> dao.changeRestTimer("toggle")
                ACTION_ADD_TIME -> dao.changeRestTimer("adjust", intent.getIntExtra(EXTRA_SECONDS, 30))
                ACTION_SKIP -> dao.changeRestTimer("skip")
            }
        }
        if (observer == null) observer = scope.launch {
            dao.getActiveWorkoutSession().distinctUntilChanged().collectLatest { session ->
                ticker?.cancel()
                cancelCompletion()
                if (session == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    dao.getWorkoutExercisesWithDetails(session.id).collectLatest { exercises ->
                        val current = exercises.getOrNull(session.currentExerciseIndex.coerceIn(0, (exercises.size - 1).coerceAtLeast(0)))
                        publish(buildNotification(session, current))
                        ticker?.cancel()
                        ticker = scope.launch {
                            // The chronometer counts down by itself; only the progress bar needs a refresh, and only
                            // while someone can see it. Re-posting two RemoteViews every second cost SystemUI work
                            // for the whole rest, screen on or off.
                            val power = getSystemService(PowerManager::class.java)
                            while (true) {
                                val deadline = session.restDeadline ?: break
                                val left = deadline - System.currentTimeMillis()
                                if (left <= 0) { completeRest(session.id, deadline); break }
                                delay(minOf(left, PROGRESS_REFRESH_MS))
                                if (System.currentTimeMillis() >= deadline) { completeRest(session.id, deadline); break }
                                if (power?.isInteractive != false) publish(buildNotification(session, current))
                            }
                        }
                        cancelCompletion()
                        session.restDeadline?.let { deadline ->
                            val listener = AlarmManager.OnAlarmListener {
                                completionListener = null
                                scope.launch {
                                    completeRest(session.id, deadline)
                                }
                            }
                            completionListener = listener
                            getSystemService(AlarmManager::class.java).setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                SystemClock.elapsedRealtime() + (deadline - System.currentTimeMillis()).coerceAtLeast(0), "mutant-rest-timer", listener, handler)
                        }
                    }
                }
            }
        }
        return START_STICKY
    }
    private suspend fun completeRest(sessionId: Long, deadline: Long) {
        if (dao.completeRestIfDue(sessionId, deadline, System.currentTimeMillis()) > 0) {
            RestTimerAlerts.showRestComplete(this)
        }
    }
    private fun publish(notification: Notification) {
        ServiceCompat.startForeground(this, RestTimerAlerts.RUNNING_NOTIFICATION_ID, notification,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
    }
    internal fun buildNotification(session: WorkoutSession, current: WorkoutExerciseDetail?): Notification {
        val remaining = session.restSecondsAt(System.currentTimeMillis())
        val running = session.restDeadline != null && remaining > 0
        val exercise = current?.exercise?.name ?: "Select exercise"
        val next = current?.workoutExercise
        val nextSet = if (next?.nextSetWeightKg != null && next.nextSetReps != null)
            "Next set: ${next.nextSetWeightKg.formatLoad()} kg \u00d7 ${next.nextSetReps}"
        else "Set your next weight and reps in the workout"
        val title = when {
            remaining > 0 -> "${if (running) "Rest" else "Rest paused"} \u00b7 $exercise"
            session.restCompleted -> "Ready \u00b7 $exercise"
            else -> exercise
        }
        val body = if (!running && remaining > 0) "${remaining / 60}:${"%02d".format(remaining % 60)} \u00b7 $nextSet" else nextSet
        val builder = NotificationCompat.Builder(this, RestTimerAlerts.RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_workout_notification).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationViews(session, current, false))
            .setCustomBigContentView(notificationViews(session, current, true))
            // A colorized foreground-service notification gets one solid card, header included: the closest an app
            // can get to a media player's custom background without being a media session.
            .setColor(0xFF17121F.toInt()).setColorized(true)
            .setSubText("Workout active")
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(activityIntent()).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT).setOngoing(true).setOnlyAlertOnce(true)
            .setWhen(if (running) session.restDeadline!! else session.startedAt)
            .setUsesChronometer(running).setChronometerCountDown(running).setShowWhen(running)
            .setProgress(if (remaining > 0) session.restRemainingSeconds.coerceAtLeast(remaining) else 0, remaining, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        return builder.build()
    }
    private fun notificationViews(session: WorkoutSession, current: WorkoutExerciseDetail?, expanded: Boolean): RemoteViews {
        val remaining = session.restSecondsAt(System.currentTimeMillis())
        val running = session.restDeadline != null && remaining > 0
        val restState = when {
            remaining > 0 -> if (running) "Rest" else "Rest paused"
            session.restCompleted -> "Ready"
            else -> "Training"
        }
        val plan = current?.workoutExercise
        val nextSet = if (plan?.nextSetWeightKg != null && plan.nextSetReps != null)
            "Next: ${plan.nextSetWeightKg.formatLoad()} kg \u00d7 ${plan.nextSetReps}"
        else "Choose next weight and reps"
        return RemoteViews(packageName, if (expanded) R.layout.notification_workout_expanded else R.layout.notification_workout_compact).apply {
            setTextViewText(R.id.notification_exercise, current?.exercise?.name ?: "Select exercise")
            setTextViewText(R.id.notification_next_set, nextSet)
            setViewVisibility(R.id.notification_countdown, if (running) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.notification_idle, if (running) View.GONE else View.VISIBLE)
            setTextViewText(R.id.notification_idle, if (remaining > 0) "%02d:%02d".format(remaining / 60, remaining % 60) else restState)
            setChronometer(R.id.notification_countdown, SystemClock.elapsedRealtime() + ((session.restDeadline ?: System.currentTimeMillis()) - System.currentTimeMillis()).coerceAtLeast(0), "%s", running)
            setBoolean(R.id.notification_countdown, "setCountDown", true)
            setViewVisibility(R.id.notification_action_pause, if (remaining > 0) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.notification_action_skip, if (remaining > 0) View.VISIBLE else View.GONE)
            setContentDescription(R.id.notification_action_pause, if (running) "Pause rest" else "Resume rest")
            setOnClickPendingIntent(R.id.notification_action_pause, serviceAction(ACTION_TOGGLE, 4301))
            setOnClickPendingIntent(R.id.notification_action_skip, serviceAction(ACTION_SKIP, 4302))
            if (!expanded) setImageViewResource(R.id.notification_action_pause, if (running) R.drawable.ic_notification_pause else R.drawable.ic_notification_play)
            if (expanded) {
                val completedSets = current?.sets?.count { it.setType == SetType.WORK } ?: 0
                val targetSets = current?.plannedSets ?: 0
                val setNumber = (completedSets + 1).coerceAtMost(targetSets.coerceAtLeast(1))
                setTextViewText(R.id.notification_status,
                    if (targetSets == 0) "Set ${completedSets + 1} \u00b7 $restState" else "Set $setNumber of $targetSets \u00b7 $restState")
                setProgressBar(R.id.notification_rest_progress, session.restRemainingSeconds.coerceAtLeast(remaining).coerceAtLeast(1), remaining, false)
                setViewVisibility(R.id.notification_rest_progress, if (remaining > 0) View.VISIBLE else View.GONE)
                setTextViewText(R.id.notification_label_pause, if (running) "Pause" else "Resume")
                setImageViewResource(R.id.notification_icon_pause, if (running) R.drawable.ic_notification_pause else R.drawable.ic_notification_play)
                setOnClickPendingIntent(R.id.notification_action_finish, finishIntent())
            }
        }
    }
    private fun finishIntent(): PendingIntent = PendingIntent.getActivity(this, 4104,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(RestTimerAlerts.OPEN_WORKOUT_EXTRA, true)
            putExtra(OPEN_FINISH_EXTRA, true)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun activityIntent(): PendingIntent = PendingIntent.getActivity(this, 4100,
        Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP; putExtra(RestTimerAlerts.OPEN_WORKOUT_EXTRA, true) },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun serviceAction(action: String, code: Int, seconds: Int = 0): PendingIntent = PendingIntent.getService(this, code,
        Intent(this, WorkoutTimerService::class.java).setAction(action).putExtra(EXTRA_SECONDS, seconds), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun cancelCompletion() { completionListener?.let { getSystemService(AlarmManager::class.java).cancel(it) }; completionListener = null }
    override fun onDestroy() { cancelCompletion(); scope.cancel(); super.onDestroy() }
    companion object {
        private const val PROGRESS_REFRESH_MS = 5_000L
        const val OPEN_FINISH_EXTRA = "open_finish_workout_confirmation"
        const val ACTION_START_OR_UPDATE = "com.example.action.TIMER_START_OR_UPDATE"
        const val ACTION_TOGGLE = "com.example.action.TIMER_TOGGLE"
        const val ACTION_ADD_TIME = "com.example.action.TIMER_ADD_TIME"
        const val ACTION_SKIP = "com.example.action.TIMER_SKIP"
        const val ACTION_STATE_CHANGED = "com.example.action.TIMER_STATE_CHANGED"
        const val EXTRA_REMAINING = "timer_remaining"
        const val EXTRA_RUNNING = "timer_running"
        const val EXTRA_EXERCISE = "timer_exercise"
        const val EXTRA_SET = "timer_set"
        const val EXTRA_SECONDS = "timer_seconds"
        const val EXTRA_FROM_UI = "timer_from_ui"
        const val EXTRA_COMPLETED = "timer_completed"
        const val KEY_REMAINING = "service_timer_remaining"
        const val KEY_RUNNING = "service_timer_running"
        const val KEY_DEADLINE = "service_timer_deadline"
        const val KEY_EXERCISE = "service_timer_exercise"
        const val KEY_SET = "service_timer_set"
    }
}
