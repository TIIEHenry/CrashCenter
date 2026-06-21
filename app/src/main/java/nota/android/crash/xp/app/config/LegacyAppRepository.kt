package nota.android.crash.xp.app.config

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.edit
import nota.android.crash.xp.PrefManager.ITSELF
import nota.android.crash.xp.PrefManager.PREF_HANDLE_SYSTEM
import nota.android.crash.xp.PrefManager.PREF_NAME
import nota.android.crash.xp.PrefManager.PREF_PACKAGE_LIST
import nota.android.crash.xp.PrefManager.PREF_SCOPE_MODE
import nota.android.crash.xp.PrefManager.PREF_SHOW_SYSTEM_UI
import nota.android.crash.xp.app.PackageVisibilityHelper

/**
 * Handles legacy app list loading and hook states persistence.
 */
class LegacyAppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val packageManager = appContext.packageManager

    // ─── Shared preferences (scope / system UI) ───

    fun readScopeMode(): Boolean = prefs.getBoolean(PREF_SCOPE_MODE, false)

    fun readHandleSystem(): Boolean = prefs.getBoolean(PREF_HANDLE_SYSTEM, false)

    fun readShowSystemUi(): Boolean = prefs.getBoolean(PREF_SHOW_SYSTEM_UI, false)

    fun setScopeMode(enabled: Boolean) {
        prefs.edit { putBoolean(PREF_SCOPE_MODE, enabled) }
    }

    fun setHandleSystem(enabled: Boolean) {
        prefs.edit { putBoolean(PREF_HANDLE_SYSTEM, enabled) }
    }

    fun setShowSystemUi(enabled: Boolean) {
        prefs.edit { putBoolean(PREF_SHOW_SYSTEM_UI, enabled) }
    }

    // ─── Legacy: installed apps with hook states ───

    fun loadInstalledApps(): List<AppItem> {
        val prefWhiteList = prefs.getStringSet(PREF_PACKAGE_LIST, null)
        val installedPackages = PackageVisibilityHelper.getInstalledPackagesCompat(packageManager)
        return installedPackages.map { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@map null
            AppItem(
                label = PackageInfoLoader.loadLabel(packageManager, appInfo),
                appInfo = appInfo,
                hookEnabled = prefWhiteList == null || !prefWhiteList.contains(packageInfo.packageName),
                packageName = packageInfo.packageName,
                isSystem = PackageInfoLoader.isSystemApp(appInfo),
                updateTime = packageInfo.lastUpdateTime,
                installTime = packageInfo.firstInstallTime,
            )
        }.filterNotNull().filter { app -> !PackageInfoLoader.isItself(app.packageName) }
    }

    fun persistHookStates(apps: List<AppItem>) {
        val disabled = buildSet {
            for (app in apps) {
                if (!app.hookEnabled) {
                    add(app.packageName)
                }
            }
        }
        prefs.edit { putStringSet(PREF_PACKAGE_LIST, disabled) }
    }

    companion object {
        fun passesSystemFilter(isSystemApp: Boolean, handleSystem: Boolean): Boolean =
            !isSystemApp || handleSystem

        fun isSystemPackage(context: Context, packageName: String): Boolean {
            return try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                PackageInfoLoader.isSystemApp(appInfo)
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }

        fun enumerateInstalledPackageNames(context: Context): Set<String> {
            val packageManager = context.packageManager
            val installedPackages = PackageVisibilityHelper.getInstalledPackagesCompat(packageManager)
            return installedPackages.map { it.packageName }
                .filter { packageName -> !PackageInfoLoader.isItself(packageName) }
                .toSet()
        }
    }
}
