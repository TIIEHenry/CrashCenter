package nota.android.crash.xp.app.di

import android.content.Context
import android.content.SharedPreferences
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootServiceRemoteAdapter
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.config.LegacyAppRepository
import nota.android.crash.xp.app.config.ManagedAppRepository
import nota.android.crash.xp.app.config.PackageVisibilityRepository
import nota.android.crash.xp.app.data.CrashLogRepository
import nota.android.crash.xp.app.data.FileCrashLogRepository
import kotlin.reflect.KMutableProperty0

/**
 * Manual service locator for singleton dependencies.
 * Used until a proper DI framework (e.g., Hilt) is introduced.
 */
object ServiceLocator {

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var legacyAppRepository: LegacyAppRepository? = null

    @Volatile
    private var managedAppRepository: ManagedAppRepository? = null

    @Volatile
    private var packageVisibilityRepository: PackageVisibilityRepository? = null

    @Volatile
    private var crashLogRepository: CrashLogRepository? = null

    @Volatile
    private var rootAccessClient: RootAccessClient? = null

    /**
     * Double-checked locking helper. Returns the existing value in [slot],
     * or creates one via [factory] under a synchronized lock and stores it.
     */
    private fun <T> getOrCreate(slot: KMutableProperty0<T?>, factory: () -> T): T {
        return slot.get() ?: synchronized(this) {
            slot.get() ?: factory().also { slot.set(it) }
        }
    }

    fun prefs(context: Context): SharedPreferences =
        getOrCreate(::prefs) {
            context.applicationContext
                .getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        }

    fun legacyAppRepository(context: Context): LegacyAppRepository =
        getOrCreate(::legacyAppRepository) {
            LegacyAppRepository(context.applicationContext, prefs(context))
        }

    fun managedAppRepository(context: Context): ManagedAppRepository =
        getOrCreate(::managedAppRepository) {
            ManagedAppRepository(context.applicationContext, prefs(context))
        }

    fun packageVisibilityRepository(context: Context): PackageVisibilityRepository =
        getOrCreate(::packageVisibilityRepository) {
            PackageVisibilityRepository(context.applicationContext)
        }

    fun crashLogRepository(context: Context): CrashLogRepository =
        getOrCreate(::crashLogRepository) {
            FileCrashLogRepository(context.applicationContext, prefs(context))
        }

    fun rootAccessClient(context: Context): RootAccessClient =
        getOrCreate(::rootAccessClient) {
            RootServiceRemoteAdapter(context.applicationContext)
        }

    /**
     * Clears all singletons. Useful for testing.
     */
    fun clear() {
        synchronized(this) {
            prefs = null
            legacyAppRepository = null
            managedAppRepository = null
            packageVisibilityRepository = null
            crashLogRepository = null
            rootAccessClient = null
        }
    }
}
