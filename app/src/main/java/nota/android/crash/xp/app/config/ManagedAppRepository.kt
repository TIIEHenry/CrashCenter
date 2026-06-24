package nota.android.crash.xp.app.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.PackageVisibilityHelper

class ManagedAppRepository(
    context: Context,
    private val prefs: SharedPreferences,
) {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun readInterceptEnabledPackages(): Set<String> =
        prefs.getStringSet(PrefManager.PREF_MANAGED_PACKAGES, emptySet()) ?: emptySet()

    fun setInterceptEnabled(packageName: String, enabled: Boolean) {
        if (PackageInfoLoader.isItself(packageName)) return
        val current = readInterceptEnabledPackages()
        val updated = if (enabled) current + packageName else current - packageName
        prefs.edit { putStringSet(PrefManager.PREF_MANAGED_PACKAGES, updated) }
    }

    fun loadInstalledApps(): List<AppItem> {
        val enabledSet = readInterceptEnabledPackages()
        val installedPackages = PackageVisibilityHelper.getInstalledPackagesCompat(packageManager)
        return installedPackages.mapNotNull { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
            val packageName = packageInfo.packageName
            AppItem(
                packageName = packageName,
                label = PackageInfoLoader.loadLabel(packageManager, appInfo),
                appInfo = appInfo,
                isSystem = PackageInfoLoader.isSystemApp(appInfo),
                interceptEnabled = packageName in enabledSet,
                updateTime = packageInfo.lastUpdateTime,
                installTime = packageInfo.firstInstallTime,
            )
        }
    }

    fun readHandleSystem(): Boolean = prefs.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, false)

    fun readShowSystemUi(): Boolean = prefs.getBoolean(PrefManager.PREF_SHOW_SYSTEM_UI, false)

    fun setHandleSystem(enabled: Boolean) {
        prefs.edit { putBoolean(PrefManager.PREF_HANDLE_SYSTEM, enabled) }
    }

    fun setShowSystemUi(enabled: Boolean) {
        prefs.edit { putBoolean(PrefManager.PREF_SHOW_SYSTEM_UI, enabled) }
    }
}
