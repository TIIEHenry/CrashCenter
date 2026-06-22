package nota.android.crash.log

import android.content.Context
import androidx.annotation.VisibleForTesting
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootAvailability
import nota.android.crash.xp.app.data.FileCrashLogRepository
import nota.android.crash.xp.app.di.ServiceLocator
import java.io.File

/**
 * Module-side coordinator that ingests relay files into the canonical JSONL store.
 *
 * TargetRelayBackend (hook-side, tier 3) writes crash events to each target app's
 * private relay directory. This coordinator uses root to scan those directories,
 * parse the events, merge them into the module's canonical JSONL, and delete the
 * relay files after successful merge.
 *
 * Runs on startup ([Context.getApplicationContext] `onCreate`) via [Dispatchers.IO].
 * All errors are silently ignored — relay files may not exist and permissions may fail.
 *
 * ## Scan path
 * ```
 * /data/user/{userId}/{packageName}/files/crashcenter_relay/{eventId}.json
 * ```
 */
object CrashLogIngestCoordinator {

    private const val USER_BASE_PATH = "/data/user"
    private const val MAX_USER_ID = 150
    private const val RELAY_DIR_NAME = "crashcenter_relay"

    /** Test-only: replace root access client; null uses real [ServiceLocator]. */
    @JvmStatic
    @VisibleForTesting
    internal var testClient: RootAccessClient? = null

    /**
     * Scan all relay directories via root, merge events into canonical JSONL,
     * and delete successfully ingested relay files.
     *
     * Safe to call without root — returns immediately if root is unavailable.
     */
    suspend fun ingest(context: Context) {
        ingest(context, testClient ?: ServiceLocator.rootAccessClient(context))
    }

    @VisibleForTesting
    internal suspend fun ingest(context: Context, rootClient: RootAccessClient) {
        try {
            ingestImpl(context, rootClient)
        } catch (_: Throwable) {
            // Silent failure — relay scanning is best-effort
        }
    }

    private suspend fun ingestImpl(context: Context, rootClient: RootAccessClient) {
        if (rootClient.probe() != RootAvailability.AVAILABLE) return

        val eventsFile = FileCrashLogRepository.eventsFile(context)
        val relayFiles = scanRelayFiles(rootClient)
        for ((userId, packageName, fileName) in relayFiles) {
            try {
                mergeRelayFile(rootClient, eventsFile, userId, packageName, fileName)
            } catch (_: Throwable) {
                // Per-file failure is non-fatal — continue with the next file
            }
        }
    }

    /**
     * Scans all user/package combinations for relay files.
     * Returns a list of (userId, packageName, fileName) triples.
     */
    @VisibleForTesting
    internal suspend fun scanRelayFiles(
        rootClient: RootAccessClient,
    ): List<RelayFileRef> {
        val result = mutableListOf<RelayFileRef>()

        val userIds = try {
            rootClient.listDir(USER_BASE_PATH)
        } catch (_: Throwable) {
            return emptyList()
        }

        for (userIdStr in userIds) {
            val userId = userIdStr.toIntOrNull() ?: continue
            if (userId < 0 || userId > MAX_USER_ID) continue

            val userPath = "$USER_BASE_PATH/$userId"
            val packages = try {
                rootClient.listDir(userPath)
            } catch (_: Throwable) {
                continue
            }

            for (packageName in packages) {
                val relayPath = "$userPath/$packageName/files/$RELAY_DIR_NAME"
                val files = try {
                    rootClient.listDir(relayPath)
                } catch (_: Throwable) {
                    continue
                }

                for (fileName in files) {
                    if (fileName.endsWith(".json")) {
                        result.add(RelayFileRef(userId, packageName, fileName))
                    }
                }
            }
        }

        return result
    }

    /**
     * Reads a single relay file, parses it as a [CrashEvent], appends to the
     * canonical JSONL, and deletes the relay file on success.
     */
    private suspend fun mergeRelayFile(
        rootClient: RootAccessClient,
        eventsFile: File,
        userId: Int,
        packageName: String,
        fileName: String,
    ) {
        val path = "$USER_BASE_PATH/$userId/$packageName/files/$RELAY_DIR_NAME/$fileName"
        val content = rootClient.readText(path) ?: return
        val event = CrashEvent.fromJson(content) ?: return

        CanonicalJsonlWriter.append(eventsFile, event)
        rootClient.delete(path)
    }

    /**
     * Identifies a single relay file by its location.
     */
    data class RelayFileRef(
        val userId: Int,
        val packageName: String,
        val fileName: String,
    )
}
