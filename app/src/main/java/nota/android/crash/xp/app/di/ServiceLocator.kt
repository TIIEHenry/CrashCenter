package nota.android.crash.xp.app.di

import android.content.Context
import nota.android.crash.xp.app.config.AppRepository
import nota.android.crash.xp.app.config.AppRepositoryInterface
import nota.android.crash.xp.app.data.CrashLogRepository
import nota.android.crash.xp.app.data.FileCrashLogRepository

/**
 * Manual service locator for singleton dependencies.
 * Used until a proper DI framework (e.g., Hilt) is introduced.
 */
object ServiceLocator {

    @Volatile
    private var appRepository: AppRepositoryInterface? = null

    @Volatile
    private var crashLogRepository: CrashLogRepository? = null

    fun appRepository(context: Context): AppRepositoryInterface {
        return appRepository ?: synchronized(this) {
            appRepository ?: AppRepository(context.applicationContext).also {
                appRepository = it
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
            appRepository = null
            crashLogRepository = null
        }
    }
}
