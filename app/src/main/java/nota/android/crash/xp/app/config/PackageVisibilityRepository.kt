package nota.android.crash.xp.app.config

import android.content.Context
import nota.android.crash.xp.app.PackageVisibilityHelper

/**
 * Handles package visibility detection.
 */
class PackageVisibilityRepository(context: Context) {

    private val appContext = context.applicationContext

    fun detectPackageVisibility(): PackageVisibilityHelper.Status =
        PackageVisibilityHelper.check(appContext)

    fun detectPackageVisibilityAfterLoad(loadedCount: Int): PackageVisibilityHelper.Status =
        PackageVisibilityHelper.checkAfterLoad(appContext, loadedCount)
}
