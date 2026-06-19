package nota.android.crash.xp

import android.content.Context
import android.content.Intent

object XposedManagerLauncher {

    private val MANAGER_PACKAGES = listOf(
        "org.lsposed.manager",
        "org.meowcat.edxposed.manager",
        "de.robv.android.xposed.installer",
    )

    @JvmStatic
    fun open(context: Context): Boolean {
        val pm = context.packageManager
        for (packageName in MANAGER_PACKAGES) {
            val intent = pm.getLaunchIntentForPackage(packageName) ?: continue
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (packageName == "org.lsposed.manager") {
                    intent.addCategory("org.lsposed.manager.LAUNCH_MANAGER")
                }
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // try next manager
            }
        }
        return false
    }
}
