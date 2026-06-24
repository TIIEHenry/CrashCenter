package nota.android.crash.log

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootAvailability
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.di.ServiceLocator

object CrashLogMigrationCoordinator {

    private data class PackageUserKey(val userId: Int, val packageName: String)

    suspend fun migrate(context: Context, rootClient: RootAccessClient = ServiceLocator.rootAccessClient(context)) {
        try {
            migrateImpl(context, rootClient)
        } catch (e: Throwable) {
            Log.w("CrashLogMigration", "migrate failed", e)
        }
    }

    fun migrateBlocking(context: Context) {
        runBlocking { migrate(context) }
    }

    private suspend fun migrateImpl(context: Context, rootClient: RootAccessClient) {
        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PrefManager.PREF_DISTRIBUTED_CACHE_MIGRATED, false)) return
        if (rootClient.probe() != RootAvailability.AVAILABLE) return

        val byTarget = mutableMapOf<PackageUserKey, MutableMap<String, CrashEvent>>()
        val canonicalByPackage = mutableMapOf<String, MutableMap<String, CrashEvent>>()

        val legacyCanonical = CrashLogPaths.legacyCanonicalFile(context)
        if (legacyCanonical.isFile) {
            legacyCanonical.readLines().forEach { line ->
                val event = CrashEvent.fromJson(line.trim()) ?: return@forEach
                mergeIntoPackageMap(canonicalByPackage, event)
            }
        }

        collectLegacyRelayEvents(rootClient, byTarget)

        for ((packageName, eventsById) in canonicalByPackage) {
            val userIds = byTarget.keys
                .filter { it.packageName == packageName }
                .map { it.userId }
                .distinct()
            val targets = if (userIds.isEmpty()) listOf(0) else userIds
            for (userId in targets) {
                val key = PackageUserKey(userId, packageName)
                val map = byTarget.getOrPut(key) { mutableMapOf() }
                for (event in eventsById.values) {
                    mergeIntoMap(map, event)
                }
            }
        }

        val hadLegacyCanonical = legacyCanonical.isFile
        val hadRelay = hasLegacyRelayFiles(rootClient)

        if (byTarget.isEmpty()) {
            if (!hadLegacyCanonical && !hadRelay) {
                prefs.edit().putBoolean(PrefManager.PREF_DISTRIBUTED_CACHE_MIGRATED, true).apply()
            }
            return
        }

        var allWritesOk = true
        for ((key, eventsById) in byTarget) {
            val path = CrashLogPaths.eventsPath(key.userId, key.packageName)
            val existing = rootClient.readText(path)?.lineSequence()
                ?.mapNotNull { CrashEvent.fromJson(it.trim()) }
                ?.toList()
                .orEmpty()
            val merged = (existing + eventsById.values)
                .groupBy { it.id }
                .map { (_, group) -> group.maxBy { it.timestampMs } }
            if (!RootCrashLogMutation.writeEvents(rootClient, path, merged)) {
                Log.w("CrashLogMigration", "write failed for $path")
                allWritesOk = false
            }
        }

        if (!allWritesOk) return

        if (hadLegacyCanonical) {
            legacyCanonical.delete()
        }
        deleteLegacyRelayFiles(rootClient)

        prefs.edit().putBoolean(PrefManager.PREF_DISTRIBUTED_CACHE_MIGRATED, true).apply()
    }

    private suspend fun collectLegacyRelayEvents(
        rootClient: RootAccessClient,
        byTarget: MutableMap<PackageUserKey, MutableMap<String, CrashEvent>>,
    ) {
        val userIds = rootClient.listDir(CrashLogPaths.USER_BASE_PATH)
        for (userIdStr in userIds) {
            val userId = userIdStr.toIntOrNull() ?: continue
            if (userId < 0 || userId > CrashLogPaths.MAX_USER_ID) continue
            val userPath = "${CrashLogPaths.USER_BASE_PATH}/$userId"
            val packages = rootClient.listDir(userPath)
            for (packageName in packages) {
                val relayPath = CrashLogPaths.legacyRelayDir(userId, packageName)
                val files = try {
                    rootClient.listDir(relayPath)
                } catch (_: Throwable) {
                    emptyList()
                }
                for (fileName in files) {
                    if (!fileName.endsWith(".json")) continue
                    val content = rootClient.readText("$relayPath/$fileName") ?: continue
                    val event = CrashEvent.fromJson(content.trim()) ?: continue
                    val key = PackageUserKey(userId, event.packageName.ifBlank { packageName })
                    val map = byTarget.getOrPut(key) { mutableMapOf() }
                    mergeIntoMap(map, event)
                }
            }
        }
    }

    private suspend fun hasLegacyRelayFiles(rootClient: RootAccessClient): Boolean {
        val userIds = rootClient.listDir(CrashLogPaths.USER_BASE_PATH)
        for (userIdStr in userIds) {
            val userId = userIdStr.toIntOrNull() ?: continue
            if (userId < 0 || userId > CrashLogPaths.MAX_USER_ID) continue
            val userPath = "${CrashLogPaths.USER_BASE_PATH}/$userId"
            val packages = rootClient.listDir(userPath)
            for (packageName in packages) {
                val relayPath = CrashLogPaths.legacyRelayDir(userId, packageName)
                val files = try {
                    rootClient.listDir(relayPath)
                } catch (_: Throwable) {
                    emptyList()
                }
                if (files.any { it.endsWith(".json") }) return true
            }
        }
        return false
    }

    private suspend fun deleteLegacyRelayFiles(rootClient: RootAccessClient) {
        val userIds = rootClient.listDir(CrashLogPaths.USER_BASE_PATH)
        for (userIdStr in userIds) {
            val userId = userIdStr.toIntOrNull() ?: continue
            if (userId < 0 || userId > CrashLogPaths.MAX_USER_ID) continue
            val userPath = "${CrashLogPaths.USER_BASE_PATH}/$userId"
            val packages = rootClient.listDir(userPath)
            for (packageName in packages) {
                val relayPath = CrashLogPaths.legacyRelayDir(userId, packageName)
                val files = try {
                    rootClient.listDir(relayPath)
                } catch (_: Throwable) {
                    emptyList()
                }
                for (fileName in files) {
                    if (fileName.endsWith(".json")) {
                        rootClient.delete("$relayPath/$fileName")
                    }
                }
            }
        }
    }

    private fun mergeIntoMap(
        map: MutableMap<String, CrashEvent>,
        event: CrashEvent,
    ) {
        val existing = map[event.id]
        if (existing == null || event.timestampMs > existing.timestampMs) {
            map[event.id] = event
        }
    }

    private fun mergeIntoPackageMap(
        byPackage: MutableMap<String, MutableMap<String, CrashEvent>>,
        event: CrashEvent,
    ) {
        val pkg = event.packageName.ifBlank { return }
        val map = byPackage.getOrPut(pkg) { mutableMapOf() }
        mergeIntoMap(map, event)
    }
}
