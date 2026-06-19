package nota.android.crash.xp.app

/**
 * Xposed self-hook replaces [isModuleActive] with `true` when the module is loaded.
 * Default `false` means inactive when hook is not applied.
 */
object ModuleActivation {

    @JvmStatic
    fun isModuleActive(): Boolean = false
}
