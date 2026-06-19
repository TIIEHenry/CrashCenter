package nota.android.crash.xp.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Detects whether the app can see the full installed-app list on Android 11+ and
 * opens App info so the user can grant [android.Manifest.permission.QUERY_ALL_PACKAGES].
 *
 * QUERY_ALL_PACKAGES is a normal (install-time) permission — there is no
 * [android.app.Activity.requestPermissions] dialog. Users must enable it manually
 * in system settings (or reinstall if the permission was never granted).
 */
object PackageVisibilityHelper {

    /** Package that is invisible when package visibility is restricted. */
    private const val PROBE_PACKAGE = "com.android.settings"

    data class Status(
        val hasFullVisibility: Boolean,
        val permissionGranted: Boolean,
        val needsUserAction: Boolean,
        val visiblePackageCount: Int = 0,
    )

    fun check(context: Context): Status {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return Status(
                hasFullVisibility = true,
                permissionGranted = true,
                needsUserAction = false,
            )
        }

        val pm = context.packageManager
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.QUERY_ALL_PACKAGES,
        ) == PackageManager.PERMISSION_GRANTED

        val visiblePackageCount = countInstalledPackages(pm)
        val probeVisible = isPackageVisible(pm, PROBE_PACKAGE)

        if (!permissionGranted) {
            return Status(
                hasFullVisibility = false,
                permissionGranted = false,
                needsUserAction = true,
                visiblePackageCount = visiblePackageCount,
            )
        }

        val hasFullVisibility = probeVisible && !isLikelyIncompleteList(pm, visiblePackageCount)
        return Status(
            hasFullVisibility = hasFullVisibility,
            permissionGranted = true,
            needsUserAction = !hasFullVisibility,
            visiblePackageCount = visiblePackageCount,
        )
    }

    /** Secondary check after app list loads (user-facing count excludes self). */
    fun checkAfterLoad(context: Context, loadedAppCount: Int): Status {
        val base = check(context)
        if (base.needsUserAction || base.hasFullVisibility) {
            return base
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return base
        }

        val pm = context.packageManager
        if (isLikelyIncompleteList(pm, loadedAppCount + 1)) {
            return base.copy(
                hasFullVisibility = false,
                needsUserAction = true,
                visiblePackageCount = loadedAppCount + 1,
            )
        }
        return base
    }

    fun openAppSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns all installed packages using the correct API-level flag.
     * On [Build.VERSION_CODES.N]+ uses [PackageManager.MATCH_UNINSTALLED_PACKAGES];
     * on older API levels falls back to [PackageManager.GET_META_DATA].
     */
    fun getInstalledPackagesCompat(pm: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } else {
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        }
    }

    private fun countInstalledPackages(pm: PackageManager): Int {
        return try {
            getInstalledPackagesCompat(pm).size
        } catch (_: Exception) {
            0
        }
    }

    private fun isPackageVisible(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Compare visible packages to launcher activities; a large gap suggests filtering.
     */
    private fun isLikelyIncompleteList(pm: PackageManager, visibleCount: Int): Boolean {
        val launcherCount = countLauncherActivities(pm)
        if (launcherCount <= 0) {
            return visibleCount < MIN_PACKAGES_WITHOUT_LAUNCHER
        }
        val threshold = maxOf(MIN_PACKAGES_WITH_LAUNCHER, launcherCount / 2)
        return visibleCount < threshold
    }

    private fun countLauncherActivities(pm: PackageManager): Int {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size
        } catch (_: Exception) {
            0
        }
    }

    private const val MIN_PACKAGES_WITHOUT_LAUNCHER = 8
    private const val MIN_PACKAGES_WITH_LAUNCHER = 10
}
