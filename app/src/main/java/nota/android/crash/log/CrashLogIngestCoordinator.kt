package nota.android.crash.log

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import nota.android.crash.log.backend.RelayMergeBackend
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootAvailability
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.di.ServiceLocator

/**
 * Module-side coordinator that schedules relay harvest into canonical JSONL.
 *
 * Delegates merge/dedupe to [RelayMergeBackend] (tier 2, module process).
 * Hook-side [nota.android.crash.log.backend.TargetRelayBackend] writes relay files;
 * this coordinator only triggers module-side ingest — no duplicate hook writes.
 *
 * Runs on startup via [ingest]. All errors are silently ignored.
 */
object CrashLogIngestCoordinator {

    /** Test-only: replace root access client; null uses real [ServiceLocator]. */
    @JvmStatic
    @VisibleForTesting
    internal var testClient: RootAccessClient? = null

    /**
     * Harvest relay files into canonical JSONL when root is available and
     * [RelayMergeBackend] is enabled in prefs.
     */
    suspend fun ingest(context: Context) {
        ingest(context, testClient ?: ServiceLocator.rootAccessClient(context))
    }

    @VisibleForTesting
    internal suspend fun ingest(context: Context, rootClient: RootAccessClient) {
        try {
            ingestImpl(context, rootClient)
        } catch (e: Throwable) {
            Log.w("CrashLogIngestCoordinator", "ingest failed", e)
        }
    }

    private suspend fun ingestImpl(context: Context, rootClient: RootAccessClient) {
        if (rootClient.probe() != RootAvailability.AVAILABLE) return

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PrefManager.PREF_CRASH_LOG_ENABLED, true)) return

        val enabled = CrashLogBackendRegistry.enabledModuleBackends(prefs)
            .any { it.id == BackendId.RELAY_MERGE }
        if (!enabled) return

        RelayMergeBackend.harvest(context, rootClient)
    }
}
