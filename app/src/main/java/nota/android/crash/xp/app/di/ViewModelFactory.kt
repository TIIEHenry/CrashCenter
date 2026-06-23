package nota.android.crash.xp.app.di

import android.content.Context
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import nota.android.crash.analysis.RuleEngine
import nota.android.crash.xp.app.config.AddManagedAppViewModel
import nota.android.crash.xp.app.config.AppInterventionEditViewModel
import nota.android.crash.xp.app.config.ConfigViewModel
import nota.android.crash.xp.app.observe.CrashDetailViewModel
import nota.android.crash.xp.app.observe.CrashHistoryViewModel
import nota.android.crash.xp.app.observe.CrashStatsViewModel
import nota.android.crash.xp.app.observe.PerAppCrashViewModel
import nota.android.crash.xp.app.shell.ShellViewModel

/**
 * Generic ViewModel factory that delegates creation to a lambda.
 * Replaces duplicated one-off ViewModelProvider.Factory subclasses.
 */
class ViewModelFactory<VM : ViewModel>(
    private val creator: () -> VM,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}

/**
 * Pre-configured ViewModel factories exposed from ServiceLocator.
 * Eliminates duplicated lambda boilerplate in Fragments/Activities.
 */
fun ServiceLocator.shellViewModelFactory(
    owner: SavedStateRegistryOwner,
): ViewModelProvider.Factory =
    object : AbstractSavedStateViewModelFactory(owner, null) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle,
        ): T {
            return ShellViewModel(handle) as T
        }
    }

fun ServiceLocator.configViewModelFactory(context: Context): ViewModelProvider.Factory =
    ViewModelFactory {
        ConfigViewModel(
            legacyAppRepository(context),
            managedAppRepository(context),
            packageVisibilityRepository(context),
        )
    }

fun ServiceLocator.crashHistoryViewModelFactory(context: Context): ViewModelProvider.Factory =
    ViewModelFactory {
        CrashHistoryViewModel(crashLogRepository(context))
    }

fun ServiceLocator.crashStatsViewModelFactory(context: Context): ViewModelProvider.Factory =
    ViewModelFactory {
        CrashStatsViewModel(
            crashLogRepository(context),
            RuleEngine.fromAssets(context),
        )
    }

fun ServiceLocator.perAppCrashViewModelFactory(
    context: Context,
    packageName: String?,
    exceptionClass: String? = null,
): ViewModelProvider.Factory =
    ViewModelFactory {
        PerAppCrashViewModel(packageName, exceptionClass, crashLogRepository(context))
    }

fun ServiceLocator.addManagedAppViewModelFactory(context: Context): ViewModelProvider.Factory =
    ViewModelFactory {
        AddManagedAppViewModel(managedAppRepository(context))
    }

fun ServiceLocator.appInterventionEditViewModelFactory(
    context: Context,
    packageName: String,
): ViewModelProvider.Factory =
    ViewModelFactory {
        AppInterventionEditViewModel(
            packageName,
            managedAppRepository(context),
        )
    }

/**
 * CrashDetailViewModel factory that uses SavedStateHandle to read crashId.
 * The fragment passes crashId via arguments; the ViewModel reads it from SavedStateHandle.
 */
fun ServiceLocator.crashDetailViewModelFactory(
    owner: SavedStateRegistryOwner,
    context: Context,
): ViewModelProvider.Factory =
    object : AbstractSavedStateViewModelFactory(owner, null) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle,
        ): T {
            return CrashDetailViewModel(
                crashLogRepository(context),
                savedStateHandle = handle,
            ) as T
        }
    }
