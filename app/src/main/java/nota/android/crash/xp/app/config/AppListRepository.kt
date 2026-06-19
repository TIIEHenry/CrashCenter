package nota.android.crash.xp.app.config

import android.content.Context
import android.content.pm.ApplicationInfo
import nota.android.crash.xp.PrefManager.ITSELF
import nota.android.crash.xp.PrefManager.PREF_HANDLE_SYSTEM
import nota.android.crash.xp.PrefManager.PREF_NAME
import nota.android.crash.xp.PrefManager.PREF_PACKAGE_LIST
import nota.android.crash.xp.PrefManager.PREF_SCOPE_MODE
import nota.android.crash.xp.PrefManager.PREF_SHOW_SYSTEM_UI
import nota.android.crash.xp.app.PackageVisibilityHelper

class AppListRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val managedAppRepository = ManagedAppRepository(appContext)

    fun isLegacyMode(): Boolean = managedAppRepository.isLegacyMode()

    fun readScopeMode(): Boolean = prefs.getBoolean(PREF_SCOPE_MODE, false)

    fun readHandleSystem(): Boolean = prefs.getBoolean(PREF_HANDLE_SYSTEM, false)

    fun readShowSystemUi(): Boolean = prefs.getBoolean(PREF_SHOW_SYSTEM_UI, false)

    fun setScopeMode(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SCOPE_MODE, enabled).apply()
    }

    fun setHandleSystem(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_HANDLE_SYSTEM, enabled).apply()
    }

    fun setShowSystemUi(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SHOW_SYSTEM_UI, enabled).apply()
    }

    fun detectPackageVisibility(): PackageVisibilityHelper.Status =
        PackageVisibilityHelper.check(appContext)

    fun detectPackageVisibilityAfterLoad(loadedCount: Int): PackageVisibilityHelper.Status =
        PackageVisibilityHelper.checkAfterLoad(appContext, loadedCount)

    fun loadInstalledApps(): List<AppItem> {
        if (!isLegacyMode()) {
            managedAppRepository.pruneUninstalled()
            return managedAppRepository.loadManagedApps().map { managed ->
                AppItem(
                    name = managed.label,
                    icon = managed.icon,
                    hookEnabled = managed.switchChecked,
                    packageName = managed.packageName,
                    isSystemApp = managed.isSystem,
                    updateTime = managed.updateTime,
                    installTime = managed.installTime,
                )
            }
        }

        val prefWhiteList = prefs.getStringSet(PREF_PACKAGE_LIST, null)
        val packageManager = appContext.packageManager
        val installedPackages = PackageVisibilityHelper.getInstalledPackagesCompat(packageManager)
        return installedPackages.map { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@map null
            AppItem(
                name = appInfo.loadLabel(packageManager).toString(),
                icon = appInfo.loadIcon(packageManager),
                hookEnabled = prefWhiteList == null || !prefWhiteList.contains(packageInfo.packageName),
                packageName = packageInfo.packageName,
                isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                updateTime = packageInfo.lastUpdateTime,
                installTime = packageInfo.firstInstallTime,
            )
        }.filterNotNull().filter { app -> app.packageName != ITSELF }
    }

    fun persistHookStates(apps: List<AppItem>) {
        if (!isLegacyMode()) {
            for (app in apps) {
                managedAppRepository.setInterventionEnabled(app.packageName, app.hookEnabled)
            }
            return
        }

        val disabled = HashSet<String>()
        for (app in apps) {
            if (!app.hookEnabled) {
                disabled.add(app.packageName)
            }
        }
        prefs.edit().putStringSet(PREF_PACKAGE_LIST, disabled).apply()
    }
}
