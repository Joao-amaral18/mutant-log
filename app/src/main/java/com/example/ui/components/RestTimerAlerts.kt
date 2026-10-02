package com.example.ui.components

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

internal object RestTimerAlerts {
    const val PREFERENCES = "mutant_timer_preferences"
    const val SOUND_ENABLED_KEY = "rest_timer_alarm_enabled"
    const val TIMER_ACTION_EXTRA = "rest_timer_action"
    const val TIMER_SECONDS_EXTRA = "rest_timer_seconds"
    const val OPEN_WORKOUT_EXTRA = "open_workout_from_timer"
    const val ACTION_ADD_TIME = "com.example.action.REST_TIMER_ADD_TIME"
    const val ACTION_SKIP = "com.example.action.REST_TIMER_SKIP"

    private const val ALARM_CHANNEL_ID = "rest_timer_alarm"
    internal const val RUNNING_CHANNEL_ID = "rest_timer_running"
    private const val ALARM_NOTIFICATION_ID = 4101
    internal const val RUNNING_NOTIFICATION_ID = 4100
    private var completionListener: android.app.AlarmManager.OnAlarmListener? = null

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun prepare(context: Context) = createNotificationChannels(context)

    fun showRunning(context: Context, remainingSeconds: Int, exerciseName: String, nextSet: String, isRunning: Boolean) {
        if (remainingSeconds <= 0 || !hasNotificationPermission(context) || !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            cancelRunning(context)
            return
        }

        createNotificationChannels(context)
        val formatted = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
        val title = if (isRunning) "REST · $formatted" else "REST PAUSED · $formatted"
        val builder = NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(exerciseName)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$exerciseName\nNext: $nextSet"))
            .setContentIntent(activityIntent(context, RUNNING_NOTIFICATION_ID))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_input_add, "+30s", timerActionIntent(context, ACTION_ADD_TIME, 30, 4102))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Skip rest", timerActionIntent(context, ACTION_SKIP, 0, 4103))

        if (isRunning) {
            builder.setWhen(System.currentTimeMillis() + remainingSeconds * 1_000L)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }
        NotificationManagerCompat.from(context).notify(RUNNING_NOTIFICATION_ID, builder.build())
    }

    fun clearSession(context: Context) {
        context.stopService(Intent(context, WorkoutTimerService::class.java))
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .remove(WorkoutTimerService.KEY_REMAINING).remove(WorkoutTimerService.KEY_RUNNING)
            .remove(WorkoutTimerService.KEY_DEADLINE).remove(WorkoutTimerService.KEY_EXERCISE).remove(WorkoutTimerService.KEY_SET).apply()
        context.stopService(Intent(context, WorkoutTimerService::class.java))
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .remove(WorkoutTimerService.KEY_REMAINING)
            .remove(WorkoutTimerService.KEY_RUNNING)
            .remove(WorkoutTimerService.KEY_DEADLINE)
            .remove(WorkoutTimerService.KEY_EXERCISE)
            .remove(WorkoutTimerService.KEY_SET)
            .commit()
        cancelCompletion(context)
        cancelRunning(context)
        NotificationManagerCompat.from(context).cancel(ALARM_NOTIFICATION_ID)
    }

    fun cancelRunning(context: Context) {
        NotificationManagerCompat.from(context).cancel(RUNNING_NOTIFICATION_ID)
    }

    fun scheduleCompletion(context: Context, remainingSeconds: Int) {
        if (remainingSeconds <= 0) return cancelCompletion(context)
        cancelCompletion(context)
        val listener = android.app.AlarmManager.OnAlarmListener {
            completionListener = null
            showRestComplete(context)
        }
        completionListener = listener
        context.getSystemService(android.app.AlarmManager::class.java).setExact(
            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + remainingSeconds * 1_000L,
            "mutant-rest-timer",
            listener,
            Handler(Looper.getMainLooper())
        )
    }

    fun cancelCompletion(context: Context) {
        completionListener?.let { context.getSystemService(android.app.AlarmManager::class.java).cancel(it) }
        completionListener = null
    }

    fun showRestComplete(context: Context) {
        if (!hasNotificationPermission(context) || !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }

        createNotificationChannels(context)
        cancelCompletion(context)
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Rest complete")
            .setContentText("Ready for your next set.")
            .setContentIntent(activityIntent(context, ALARM_NOTIFICATION_ID))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(ALARM_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarmChannel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Workout rest alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Sounds the Android alarm when a workout rest timer ends"
            setSound(alarmUri, audioAttributes)
            enableVibration(true)
        }
        val runningChannel = NotificationChannel(
            RUNNING_CHANNEL_ID,
            "Workout rest countdown",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the running rest countdown on the lock screen"
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(alarmChannel)
            createNotificationChannel(runningChannel)
        }
    }

    private fun activityIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(OPEN_WORKOUT_EXTRA, true)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun timerActionIntent(context: Context, action: String, seconds: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(OPEN_WORKOUT_EXTRA, true)
            putExtra(TIMER_ACTION_EXTRA, action)
            putExtra(TIMER_SECONDS_EXTRA, seconds)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
