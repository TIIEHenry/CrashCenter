package nota.android.crash.xp.app.di

import android.content.Context
import android.content.SharedPreferences
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.config.LegacyAppRepository
import nota.android.crash.xp.app.config.ManagedAppRepository
import nota.android.crash.xp.app.config.PackageVisibilityRepository
import nota.android.crash.xp.app.data.CrashLogRepository
import nota.android.crash.xp.app.data.FileCrashLogRepository

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

    fun prefs(context: Context): SharedPreferences {
        return prefs ?: synchronized(this) {
            prefs ?: context.applicationContext
                .getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
                .also { prefs = it }
        }
    }

    fun legacyAppRepository(context: Context): LegacyAppRepository {
        return legacyAppRepository ?: synchronized(this) {
            legacyAppRepository ?: LegacyAppRepository(context.applicationContext, prefs(context)).also {
                legacyAppRepository = it
            }
        }
    }

    fun managedAppRepository(context: Context): ManagedAppRepository {
        return managedAppRepository ?: synchronized(this) {
            managedAppRepository ?: ManagedAppRepository(context.applicationContext, prefs(context)).also {
                managedAppRepository = it
            }
        }
    }

    fun packageVisibilityRepository(context: Context): PackageVisibilityRepository {
        return packageVisibilityRepository ?: synchronized(this) {
            packageVisibilityRepository ?: PackageVisibilityRepository(context.applicationContext).also {
                packageVisibilityRepository = it
            }
        }
    }

    fun crashLogRepository(context: Context): CrashLogRepository {
        return crashLogRepository ?: synchronized(this) {
            crashLogRepository ?: FileCrashLogRepository(context.applicationContext).also {
                crashLogRepository = it
            }
        }
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
        }
    }
}
