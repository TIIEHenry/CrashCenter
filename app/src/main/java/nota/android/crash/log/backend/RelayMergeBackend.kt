package nota.android.crash.log.backend

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CanonicalJsonlWriter
import nota.android.crash.log.CrashLogBackend
import nota.android.crash.log.ProcessSlot
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootAvailability
import nota.android.crash.xp.app.data.FileCrashLogRepository
import java.io.File

/**
 * Module-side backend that harvests hook-written relay files into canonical JSONL.
 *
 * Scans `/data/user/{userId}/{packageName}/files/crashcenter_relay/` via root,
 * dedupes by [CrashEvent.id] against existing canonical lines (ADR-017), stamps
 * [CrashEvent.ingestedFrom], appends new events, and deletes ingested relay files.
 *
 * Hook-side [TargetRelayBackend] writes relay copies; this backend only reads them.
 */
object RelayMergeBackend : CrashLogBackend {

    private const val USER_BASE_PATH = "/data/user"
    private const val MAX_USER_ID = 150
    private const val MAX_RELAY_FILES = 1000

    override val id = BackendId.RELAY_MERGE
    override val tier = 2
    override val runsOn = ProcessSlot.MODULE

    override fun probe(context: Context): BackendAvailability =
        BackendAvailability.READY

    override fun append(context: Context, event: CrashEvent, deadlineMs: Long): AppendResult =
        AppendResult.Failure("relay merge uses harvest, not append")

    /**
     * Harvests all relay files into canonical JSONL. Silent on any failure.
     */
    suspend fun harvest(context: Context, rootClient: RootAccessClient) {
        try {
            harvestImpl(context, rootClient)
        } catch (e: Throwable) {
            Log.w("RelayMergeBackend", "Relay harvest failed", e)
        }
    }

    private suspend fun harvestImpl(context: Context, rootClient: RootAccessClient) {
        val eventsFile = FileCrashLogRepository.eventsFile(context)
        val canonicalIds = readCanonicalIds(eventsFile).toMutableSet()
        val relayFiles = scanRelayFiles(rootClient)
        for ((userId, packageName, fileName) in relayFiles) {
            try {
                mergeRelayFile(rootClient, eventsFile, canonicalIds, userId, packageName, fileName)
            } catch (e: Throwable) {
                Log.d("RelayMergeBackend", "Failed to merge relay file: $userId/$packageName/$fileName", e)
            }
        }
    }

    /**
     * Reads a relay file, dedupes by id, appends when new, deletes on successful ingest.
     *
     * @return true when the relay file was deleted (ingested or duplicate skipped)
     */
    @VisibleForTesting
    internal suspend fun mergeRelayFile(
        rootClient: RootAccessClient,
        eventsFile: File,
        canonicalIds: MutableSet<String>,
        userId: Int,
        packageName: String,
        fileName: String,
    ): Boolean {
        val path = relayFilePath(userId, packageName, fileName)
        val content = rootClient.readText(path) ?: return false
        val event = CrashEvent.fromJson(content) ?: return false

        return when (val decision = mergeDecision(event, canonicalIds)) {
            is MergeDecision.Append -> {
                CanonicalJsonlWriter.append(eventsFile, decision.stamped)
                canonicalIds.add(event.id)
                rootClient.delete(path)
            }
            is MergeDecision.SkipDuplicate -> rootClient.delete(path)
        }
    }

    /**
     * Scans all user/package combinations for relay `.json` files.
     */
    @VisibleForTesting
    internal suspend fun scanRelayFiles(rootClient: RootAccessClient): List<RelayFileRef> {
        val result = mutableListOf<RelayFileRef>()

        val userIds = try {
            rootClient.listDir(USER_BASE_PATH)
        } catch (e: Throwable) {
            Log.d("RelayMergeBackend", "Failed to list user dirs", e)
            return emptyList()
        }

        for (userIdStr in userIds) {
            val userId = userIdStr.toIntOrNull() ?: continue
            if (userId < 0 || userId > MAX_USER_ID) continue

            val userPath = "$USER_BASE_PATH/$userId"
            val packages = try {
                rootClient.listDir(userPath)
            } catch (e: Throwable) {
                Log.d("RelayMergeBackend", "Failed to list packages for user $userId", e)
                continue
            }

            for (packageName in packages) {
                val relayPath = "$userPath/$packageName/files/${TargetRelayBackend.RELAY_DIR}"
                val files = try {
                    rootClient.listDir(relayPath)
                } catch (e: Throwable) {
                    Log.d("RelayMergeBackend", "Failed to list relay dir: $relayPath", e)
                    continue
                }

                for (fileName in files) {
                    if (fileName.endsWith(".json")) {
                        result.add(RelayFileRef(userId, packageName, fileName))
                        if (result.size >= MAX_RELAY_FILES) {
                            Log.w("RelayMergeBackend", "Hit relay file limit ($MAX_RELAY_FILES), stopping scan early")
                            return result
                        }
                    }
                }
            }
        }

        return result
    }

    @VisibleForTesting
    internal fun readCanonicalIds(eventsFile: File): Set<String> {
        if (!eventsFile.isFile) return emptySet()
        return eventsFile.readLines()
            .asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { CrashEvent.fromJson(it)?.id }
            .toSet()
    }

    @VisibleForTesting
    internal fun mergeDecision(event: CrashEvent, canonicalIds: Set<String>): MergeDecision {
        if (event.id in canonicalIds) {
            return MergeDecision.SkipDuplicate
        }
        val stamped = event.copy(
            ingestedFrom = BackendId.TARGET_RELAY.wireName,
            backendWritten = (event.backendWritten + BackendId.RELAY_MERGE.wireName).distinct(),
        )
        return MergeDecision.Append(stamped)
    }

    @VisibleForTesting
    internal fun relayFilePath(userId: Int, packageName: String, fileName: String): String =
        "$USER_BASE_PATH/$userId/$packageName/files/${TargetRelayBackend.RELAY_DIR}/$fileName"

    data class RelayFileRef(
        val userId: Int,
        val packageName: String,
        val fileName: String,
    )

    sealed class MergeDecision {
        data class Append(val stamped: CrashEvent) : MergeDecision()
        data object SkipDuplicate : MergeDecision()
    }
}
