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
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import de.robv.android.xposed.XposedBridge
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.R
import java.util.Random

/**
 * Hook-side Toast + Notification feedback (ADR-011).
 * Failures are logged only; never [System.exit] or rethrow.
 */
object CrashFeedbackFacade {

    private const val CRASH_INFO_CLASS = "nota.android.crash.ActivityCrashInfo"
    private const val CHANNEL_ID = "catch_exception"
    private const val CHANNEL_NAME = "catch exception[Xposed]"

    fun show(
        application: Application,
        packageName: String,
        appInfo: ApplicationInfo,
        throwable: Throwable,
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
                showNotification(packageName, application, appInfo, throwable)
            } catch (t: Throwable) {
                XposedBridge.log(t)
            }
        }
    }

    private fun showNotification(
        pkgName: String,
        application: Application,
        appInfo: ApplicationInfo,
        throwable: Throwable,
    ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(PrefManager.PACKAGE_NAME, CRASH_INFO_CLASS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            putExtra("Exception", Log.getStackTraceString(throwable))
        }

        val appName = appInfo.loadLabel(application.packageManager).toString()
        val root = throwable.cause ?: throwable
        val stackTrace = buildString {
            for (element in root.stackTrace) {
                append("  at ").append(element).append("\r\n")
            }
        }

        val notificationManager =
            application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val moduleContext = AndroidAppHelper.currentApplication()
            .createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY)

        var pendingFlags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingFlags = pendingFlags or PendingIntent.FLAG_IMMUTABLE
        }
        val contentIntent = PendingIntent.getActivity(application, 0, intent, pendingFlags)

        val title = moduleContext.getString(R.string.crash_tip) + " - " + appName
        val bigText = (throwable.localizedMessage ?: "") + "\n-----\n" + stackTrace

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            appInfo.targetSdkVersion >= Build.VERSION_CODES.O
        ) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW),
            )
            Notification.Builder(application, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(application)
        }
            .setContentTitle(title)
            .setContentText(throwable.localizedMessage)
            .setStyle(Notification.BigTextStyle().bigText(bigText))
            .setSmallIcon(appInfo.icon)
            .setContentIntent(contentIntent)
            .build()

        notificationManager.notify(Random().nextInt(), notification)
    }

    private fun crashTipPrefix(packageName: String): String {
        return try {
            val context = AndroidAppHelper.currentApplication()
                .createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            context.getString(R.string.crash_tip) + ": "
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }
}
