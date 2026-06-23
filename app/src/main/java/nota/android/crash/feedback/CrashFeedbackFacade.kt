package nota.android.crash.feedback

import android.app.AndroidAppHelper
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import de.robv.android.xposed.XposedBridge
import nota.android.crash.xp.PrefManager
import androidx.core.app.NotificationCompat

/**
 * Hook-side Toast + Notification feedback (ADR-011).
 * Failures are logged only; never [System.exit] or rethrow.
 */
object CrashFeedbackFacade {

    private const val CRASH_INFO_CLASS = "nota.android.crash.ActivityCrashInfo"
    private const val CHANNEL_ID = "catch_exception"
    private const val CHANNEL_NAME = "catch exception[Xposed]"
    private const val NOTIFY_ID_BASE = 0x4E43_4100  // "NCA" prefix in ASCII

    /**
     * Deterministic notification ID for a package.
     * Same app => same ID, so later crashes update the existing notification
     * instead of spamming new ones. Different apps get different IDs.
     */
    private fun notificationIdFor(packageName: String): Int {
        return NOTIFY_ID_BASE + packageName.hashCode()
    }
    private const val PREF_NAME = "notification_crash_counts"

    fun show(
        application: Application,
        packageName: String,
        appInfo: ApplicationInfo,
        throwable: Throwable,
        crashId: String,
        showNotify: Boolean,
    ) {
        if (!showNotify) return
        Handler(application.mainLooper).post {
            try {
                val label = appInfo.loadLabel(application.packageManager).toString()
                val tip = crashTipPrefix(packageName)
                Toast.makeText(
                    application,
                    tip + label + " " + throwable.localizedMessage,
                    Toast.LENGTH_LONG,
                ).show()
                showNotification(packageName, application, appInfo, label, throwable, crashId)
            } catch (t: Throwable) {
                try {
                    XposedBridge.log(t)
                } catch (_: Throwable) {
                    Log.e("CrashFeedbackFacade", "Error showing crash feedback", t)
                }
            }
        }
    }

    private fun showNotification(
        pkgName: String,
        application: Application,
        appInfo: ApplicationInfo,
        appName: String,
        throwable: Throwable,
        crashId: String,
    ) {
        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (!notificationManager.areNotificationsEnabled()) return
            } catch (_: SecurityException) {
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(PrefManager.PACKAGE_NAME, CRASH_INFO_CLASS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            putExtra("crash_id", crashId)
        }
        val root = throwable.cause ?: throwable
        val stackTrace = buildString {
            for (element in root.stackTrace) {
                append("  at ").append(element).append("\r\n")
            }
        }

        val moduleContext = try {
            AndroidAppHelper.currentApplication()
                .createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY)
        } catch (_: Throwable) {
            application
        }

        var pendingFlags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingFlags = pendingFlags or PendingIntent.FLAG_IMMUTABLE
        }
        val contentIntent = PendingIntent.getActivity(application, 0, intent, pendingFlags)

        val count = incrementCrashCount(pkgName)
        val countText = resolveString(moduleContext, "notif_intercepted_count")
            ?.let { String.format(it, count) }
            ?: "Intercepted $count times"

        val title = resolveCrashTip(moduleContext, pkgName)
            ?.let { "$it - $appName" }
            ?: appName
        val bigText = countText + "\n" + (throwable.localizedMessage ?: "") + "\n-----\n" + stackTrace

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW),
            )
        }

        val builder = NotificationCompat.Builder(application, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(countText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setSmallIcon(appInfo.icon)
            .setContentIntent(contentIntent)

        val viewAllLabel = resolveString(moduleContext, "notif_view_all") ?: "View all"
        val viewAllIntent = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(
                PrefManager.PACKAGE_NAME,
                "nota.android.crash.xp.app.observe.PerAppCrashActivity",
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            putExtra("packageName", pkgName)
        }
        val viewAllPending = PendingIntent.getActivity(
            application,
            pkgName.hashCode(),
            viewAllIntent,
            pendingFlags,
        )
        builder.addAction(0, viewAllLabel, viewAllPending)

        notificationManager.notify(notificationIdFor(pkgName), builder.build())
    }

    private fun incrementCrashCount(packageName: String): Int {
        return try {
            val ctx = AndroidAppHelper.currentApplication()
                .createPackageContext(PrefManager.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY)
            val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val newCount = prefs.getInt(packageName, 0) + 1
            prefs.edit().putInt(packageName, newCount).apply()
            newCount
        } catch (_: Throwable) {
            1
        }
    }

    private fun resolveString(context: Context, name: String): String? {
        return try {
            val resId = context.resources.getIdentifier(name, "string", PrefManager.PACKAGE_NAME)
            if (resId != 0) context.getString(resId) else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun crashTipPrefix(packageName: String): String {
        return try {
            val currentApp = AndroidAppHelper.currentApplication() ?: return ""
            val tip = resolveCrashTip(currentApp, packageName) ?: return ""
            "$tip: "
        } catch (_: Throwable) {
            ""
        }
    }

    private fun resolveCrashTip(baseContext: Context, packageName: String): String? {
        return try {
            val context = baseContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            val resId = context.resources.getIdentifier("crash_tip", "string", PrefManager.PACKAGE_NAME)
            if (resId != 0) context.getString(resId) else null
        } catch (_: Throwable) {
            null
        }
    }
}
