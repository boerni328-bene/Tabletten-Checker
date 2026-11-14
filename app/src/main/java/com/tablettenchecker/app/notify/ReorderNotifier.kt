package com.tablettenchecker.app.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tablettenchecker.app.MainActivity
import com.tablettenchecker.app.R
import com.tablettenchecker.app.data.AppSettings
import com.tablettenchecker.app.data.MedicationStore
import com.tablettenchecker.app.data.ReorderCheck
import com.tablettenchecker.app.data.StockCalculator
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Nachkauf-Hinweis: einmal am Tag zur eingestellten Uhrzeit prüfen und bei Bedarf eine Benachrichtigung zeigen.
 * Ungefährer Wecker (AlarmManager, ohne Berechtigung für genaue Wecker); nach einem Neustart und nach einem
 * Update stellt [StartReceiver] ihn neu.
 */
object ReorderNotifier {

    private const val CHANNEL = "nachkauf"
    private const val NOTIFICATION_ID = 1
    private const val KEY_DAY = "hinweis_tag"
    private const val KEY_IDS = "hinweis_ids"

    /** Öffnet die App direkt mit der Einkaufsliste. */
    const val EXTRA_PAGE = "seite"
    const val PAGE_SHOPPING = "einkauf"

    fun loadSettings(context: Context): AppSettings = store(context).load().settings

    /** Täglichen Wecker stellen (oder entfernen, wenn der Hinweis aus ist). */
    fun schedule(context: Context, settings: AppSettings) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReorderAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarms.cancel(pending)
        if (!settings.notify) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return
        }
        val now = ZonedDateTime.now()
        val time = LocalTime.of((settings.notifyMinutes / 60).coerceIn(0, 23), (settings.notifyMinutes % 60).coerceIn(0, 59))
        var next = now.toLocalDate().atTime(time).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        alarms.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            pending,
        )
    }

    /**
     * Prüft den Nachkauf. Mit [post] = true (täglicher Wecker) kommt bei Bedarf eine Benachrichtigung;
     * sonst wird nur ein überholter Hinweis entfernt, z. B. nachdem gekauft wurde.
     */
    // Die Berechtigung wird direkt vor dem Anzeigen geprüft (ab Android 13 nötig).
    @SuppressLint("MissingPermission")
    fun check(context: Context, post: Boolean) {
        val data = store(context).load()
        val manager = NotificationManagerCompat.from(context)
        val prefs = context.getSharedPreferences("einstellungen", Context.MODE_PRIVATE)
        val settings = data.settings
        val plan = StockCalculator.Plan.of(settings)
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val rows = data.medications.map { it to StockCalculator.status(it, now, plan = plan) }
        val result = if (settings.notify) ReorderCheck.due(rows, today, plan.leadDays) else null
        if (result == null) {
            manager.cancel(NOTIFICATION_ID)
            prefs.edit().remove(KEY_DAY).remove(KEY_IDS).apply()
            return
        }
        if (!post) return

        val ids = result.due.map { it.id }.toSet()
        val lastDay = if (prefs.contains(KEY_DAY)) LocalDate.ofEpochDay(prefs.getLong(KEY_DAY, 0L)) else null
        val lastIds = prefs.getStringSet(KEY_IDS, null).orEmpty()
        if (!ReorderCheck.shouldNotify(ids, today, lastDay, lastIds)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context)
        val dueNames = result.due.joinToString(", ") { it.name }
        val text = if (result.along.isEmpty()) {
            dueNames
        } else {
            dueNames + "\n" + context.getString(R.string.notify_along, result.along.joinToString(", ") { it.name })
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_PAGE, PAGE_SHOPPING)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notify_title))
            .setContentText(dueNames)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        prefs.edit().putLong(KEY_DAY, today.toEpochDay()).putStringSet(KEY_IDS, ids).apply()
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.notify_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun store(context: Context) = MedicationStore(File(context.filesDir, "tabletten.json"))
}

/** Täglicher Wecker: Nachkauf prüfen. Nicht von außen erreichbar. */
class ReorderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReorderNotifier.check(context, post = true)
    }
}

/** Nach einem Neustart des Handys und nach einem Update der App den Wecker neu stellen. */
class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ReorderNotifier.schedule(context, ReorderNotifier.loadSettings(context))
        }
    }
}
