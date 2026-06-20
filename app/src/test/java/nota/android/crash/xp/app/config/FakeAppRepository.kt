package nota.android.crash.xp.app.config

import nota.android.crash.xp.app.PackageVisibilityHelper

class FakeAppRepository : AppRepositoryInterface {

    var legacyMode: Boolean = true
    var _scopeMode: Boolean = false
    var _handleSystem: Boolean = false
    var _showSystemUi: Boolean = false
    var packageVisibilityStatus: PackageVisibilityHelper.Status = PackageVisibilityHelper.Status(
        hasFullVisibility = true,
        permissionGranted = true,
        needsUserAction = false,
    )

    var installedApps: List<AppItem> = emptyList()
    var managedAppsList: List<ManagedApp> = emptyList()
    var pickableAppsList: List<PickableApp> = emptyList()

    private var _persistedHookStates: List<AppItem>? = null
    private var _profiles: MutableMap<String, AppInterventionProfile> = mutableMapOf()
    private var _interventionEnabled: MutableMap<String, Boolean> = mutableMapOf()
    private var _managedPackages: MutableSet<String> = mutableSetOf()

    fun setProfile(packageName: String, profile: AppInterventionProfile) {
        _profiles[packageName] = profile
    }

    override fun isLegacyMode(): Boolean = legacyMode

    override fun readScopeMode(): Boolean = _scopeMode
    override fun readHandleSystem(): Boolean = _handleSystem
    override fun readShowSystemUi(): Boolean = _showSystemUi

    override fun setScopeMode(enabled: Boolean) { _scopeMode = enabled }
    override fun setHandleSystem(enabled: Boolean) { _handleSystem = enabled }
    override fun setShowSystemUi(enabled: Boolean) { _showSystemUi = enabled }

    override fun detectPackageVisibility(): PackageVisibilityHelper.Status = packageVisibilityStatus
    override fun detectPackageVisibilityAfterLoad(loadedCount: Int): PackageVisibilityHelper.Status = packageVisibilityStatus

    override fun loadInstalledApps(): List<AppItem> = installedApps

    override fun persistHookStates(apps: List<AppItem>) {
        _persistedHookStates = apps.toList()
    }

    fun getPersistedHookStates(): List<AppItem>? = _persistedHookStates

    override fun loadManagedApps(): List<ManagedApp> = managedAppsList

    override fun loadPickableApps(): List<PickableApp> = pickableAppsList

    override fun readManagedPackageNames(): Set<String>? =
        if (legacyMode) null else _managedPackages.toSet()

    override fun addManagedPackages(packages: Collection<String>) {
        _managedPackages.addAll(packages)
    }

    override fun removeManagedPackage(packageName: String) {
        _managedPackages.remove(packageName)
    }

    override fun pruneUninstalled(): Int = 0

    override fun setInterventionEnabled(packageName: String, enabled: Boolean) {
        _interventionEnabled[packageName] = enabled
        val currentProfile = _profiles[packageName] ?: AppInterventionProfile.EMPTY
        if (currentProfile.rules.isEmpty()) {
            _profiles[packageName] = AppInterventionProfile(
                rules = listOf(InterventionRule.defaultCatchAll(enabled = enabled)),
            )
        } else {
            _profiles[packageName] = currentProfile.copy(
                rules = currentProfile.rules.map { it.copy(enabled = enabled) },
            )
        }
        // Update managedAppsList so loadManagedApps reflects the change
        managedAppsList = managedAppsList.map { app ->
            if (app.packageName == packageName) {
                val enabledCount = if (enabled) 1 else 0
                app.copy(
                    switchChecked = enabled,
                    interventionStatus = if (enabled) InterventionStatus.ENABLED else InterventionStatus.PENDING,
                    enabledRuleCount = enabledCount,
                )
            } else {
                app
            }
        }
    }

    fun isInterventionEnabled(packageName: String): Boolean =
        _interventionEnabled[packageName] ?: false

    override fun getProfile(packageName: String): AppInterventionProfile =
        _profiles[packageName] ?: AppInterventionProfile.EMPTY

    override fun saveProfile(packageName: String, profile: AppInterventionProfile) {
        _profiles[packageName] = profile
    }
}
