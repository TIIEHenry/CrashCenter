package nota.android.crash.xp.app.config

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.PackageVisibilityHelper

object PackageInfoLoader {

    fun loadAppInfo(
        packageManager: PackageManager,
        packageName: String,
    ): Pair<PackageInfo, ApplicationInfo>? {
        val packageInfo = try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val appInfo = packageInfo.applicationInfo ?: return null
        return packageInfo to appInfo
    }

    fun loadLabel(packageManager: PackageManager, appInfo: ApplicationInfo): String =
        appInfo.loadLabel(packageManager).toString()

    fun isSystemApp(appInfo: ApplicationInfo): Boolean =
        appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

    fun isItself(packageName: String): Boolean = packageName == PrefManager.ITSELF

    fun passesSystemFilter(isSystemApp: Boolean, handleSystem: Boolean): Boolean =
        !isSystemApp || handleSystem

    fun isSystemPackage(context: Context, packageName: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            isSystemApp(appInfo)
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun enumerateInstalledPackageNames(context: Context): Set<String> {
        val packageManager = context.packageManager
        val installedPackages = PackageVisibilityHelper.getInstalledPackagesCompat(packageManager)
        return installedPackages.map { it.packageName }
            .filter { packageName -> !isItself(packageName) }
            .toSet()
    }
}
