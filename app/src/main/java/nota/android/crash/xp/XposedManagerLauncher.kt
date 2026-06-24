package nota.android.crash.xp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object XposedManagerLauncher {

    private const val LSPOSED_PACKAGE = "org.lsposed.manager"
    private const val LSPOSED_MAIN_ACTIVITY = "org.lsposed.manager.ui.activity.MainActivity"

    private val MANAGER_PACKAGES = listOf(
        LSPOSED_PACKAGE,
        "org.meowcat.edxposed.manager",
        "de.robv.android.xposed.installer",
    )

    fun open(context: Context): Boolean {
        val pm = context.packageManager
        for (packageName in MANAGER_PACKAGES) {
            val intents = buildManagerIntents(context, packageName)
            for (intent in intents) {
                if (launch(context, packageName, intent)) {
                    return true
                }
            }
        }
        return false
    }

    private fun buildManagerIntents(context: Context, packageName: String): List<Intent> {
        val pm = context.packageManager
        val intents = mutableListOf<Intent>()

        pm.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            if (packageName == LSPOSED_PACKAGE) {
                launchIntent.addCategory("$LSPOSED_PACKAGE.LAUNCH_MANAGER")
            }
            intents += launchIntent
        }

        if (packageName == LSPOSED_PACKAGE) {
            intents += Intent(Intent.ACTION_MAIN).setComponent(
                ComponentName(LSPOSED_PACKAGE, LSPOSED_MAIN_ACTIVITY),
            )
            intents += Intent(Intent.ACTION_VIEW, Uri.parse("lsposed://manager"))
        }

        return intents
    }

    private fun launch(context: Context, packageName: String, intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w("XposedManagerLauncher", "Failed to launch manager: $packageName", e)
            false
        }
    }
}
