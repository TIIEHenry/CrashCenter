package nota.android.crash.xp.app.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import nota.android.crash.xp.app.PackageVisibilityHelper

interface AppRepositoryInterface {
    fun isLegacyMode(): Boolean
    fun readScopeMode(): Boolean
    fun readHandleSystem(): Boolean
    fun readShowSystemUi(): Boolean
    fun setScopeMode(enabled: Boolean)
    fun setHandleSystem(enabled: Boolean)
    fun setShowSystemUi(enabled: Boolean)
    fun detectPackageVisibility(): PackageVisibilityHelper.Status
    fun detectPackageVisibilityAfterLoad(loadedCount: Int): PackageVisibilityHelper.Status
    fun loadInstalledApps(): Flow<List<AppItem>>
    fun persistHookStates(apps: List<AppItem>)
    fun loadManagedApps(): Flow<List<ManagedApp>>
    fun loadPickableApps(): Flow<List<PickableApp>>
    fun readManagedPackageNames(): Set<String>?
    fun addManagedPackages(packages: Collection<String>)
    fun removeManagedPackage(packageName: String)
    fun pruneUninstalled(): Int
    fun getProfile(packageName: String): AppInterventionProfile
    fun saveProfile(packageName: String, profile: AppInterventionProfile)
    fun setInterventionEnabled(packageName: String, enabled: Boolean)
}

/**
 * Facade that delegates to focused repositories.
 * Kept for backward compatibility; new code should inject the specific repository directly.
 */
class AppRepository(context: Context) : AppRepositoryInterface {

    private val legacyRepo = LegacyAppRepository(context)
    private val managedRepo = ManagedAppRepository(context)
    private val visibilityRepo = PackageVisibilityRepository(context)

    override fun isLegacyMode(): Boolean = managedRepo.isLegacyMode()

    override fun readScopeMode(): Boolean = legacyRepo.readScopeMode()
    override fun readHandleSystem(): Boolean = legacyRepo.readHandleSystem()
    override fun readShowSystemUi(): Boolean = legacyRepo.readShowSystemUi()

    override fun setScopeMode(enabled: Boolean) = legacyRepo.setScopeMode(enabled)
    override fun setHandleSystem(enabled: Boolean) = legacyRepo.setHandleSystem(enabled)
    override fun setShowSystemUi(enabled: Boolean) = legacyRepo.setShowSystemUi(enabled)

    override fun detectPackageVisibility(): PackageVisibilityHelper.Status =
        visibilityRepo.detectPackageVisibility()

    override fun detectPackageVisibilityAfterLoad(loadedCount: Int): PackageVisibilityHelper.Status =
        visibilityRepo.detectPackageVisibilityAfterLoad(loadedCount)

    override fun loadInstalledApps(): Flow<List<AppItem>> = flow {
        if (!isLegacyMode()) {
            pruneUninstalled()
            emit(managedRepo.loadManagedApps().map { managed ->
                AppItem(
                    name = managed.label,
                    appInfo = managed.appInfo,
                    hookEnabled = managed.switchChecked,
                    packageName = managed.packageName,
                    isSystemApp = managed.isSystem,
                    updateTime = managed.updateTime,
                    installTime = managed.installTime,
                )
            })
        } else {
            emit(legacyRepo.loadInstalledApps())
        }
    }.flowOn(Dispatchers.IO)

    override fun persistHookStates(apps: List<AppItem>) {
        if (!isLegacyMode()) {
            for (app in apps) {
                setInterventionEnabled(app.packageName, app.hookEnabled)
            }
            return
        }
        legacyRepo.persistHookStates(apps)
    }

    override fun loadManagedApps(): Flow<List<ManagedApp>> = flow {
        emit(managedRepo.loadManagedApps())
    }.flowOn(Dispatchers.IO)

    override fun loadPickableApps(): Flow<List<PickableApp>> = flow {
        emit(managedRepo.loadPickableApps())
    }.flowOn(Dispatchers.IO)

    override fun readManagedPackageNames(): Set<String>? = managedRepo.readManagedPackageNames()

    override fun addManagedPackages(packages: Collection<String>) =
        managedRepo.addManagedPackages(packages)

    override fun removeManagedPackage(packageName: String) =
        managedRepo.removeManagedPackage(packageName)

    override fun pruneUninstalled(): Int = managedRepo.pruneUninstalled()

    override fun getProfile(packageName: String): AppInterventionProfile =
        managedRepo.getProfile(packageName)

    override fun saveProfile(packageName: String, profile: AppInterventionProfile) =
        managedRepo.saveProfile(packageName, profile)

    override fun setInterventionEnabled(packageName: String, enabled: Boolean) =
        managedRepo.setInterventionEnabled(packageName, enabled)

    companion object {
        fun passesSystemFilter(isSystemApp: Boolean, handleSystem: Boolean): Boolean =
            LegacyAppRepository.passesSystemFilter(isSystemApp, handleSystem)

        fun isSystemPackage(context: Context, packageName: String): Boolean =
            LegacyAppRepository.isSystemPackage(context, packageName)
    }
}
