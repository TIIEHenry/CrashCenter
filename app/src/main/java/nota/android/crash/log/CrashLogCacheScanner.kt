package nota.android.crash.log

import android.util.Log
import nota.android.crash.root.RootAccessClient

object CrashLogCacheScanner {

    data class CrashLogFileRef(
        val userId: Int,
        val packageName: String,
        val path: String,
    )

    suspend fun scanEventFiles(rootClient: RootAccessClient): List<CrashLogFileRef> {
        val result = mutableListOf<CrashLogFileRef>()
        val userIds = try {
            rootClient.listDir(CrashLogPaths.USER_BASE_PATH)
        } catch (e: Throwable) {
            Log.d("CrashLogCacheScanner", "Failed to list user dirs", e)
            return emptyList()
        }

        for (userIdStr in userIds) {
            val userId = userIdStr.toIntOrNull() ?: continue
            if (userId < 0 || userId > CrashLogPaths.MAX_USER_ID) continue

            val userPath = "${CrashLogPaths.USER_BASE_PATH}/$userId"
            val packages = try {
                rootClient.listDir(userPath)
            } catch (e: Throwable) {
                Log.d("CrashLogCacheScanner", "Failed to list packages for user $userId", e)
                continue
            }

            for (packageName in packages) {
                val logDirPath = "$userPath/$packageName/cache/${CrashLogPaths.LOG_DIR}"
                val files = try {
                    rootClient.listDir(logDirPath)
                } catch (e: Throwable) {
                    continue
                }
                if (CrashLogPaths.EVENTS_FILE !in files) continue
                result.add(
                    CrashLogFileRef(
                        userId = userId,
                        packageName = packageName,
                        path = CrashLogPaths.eventsPath(userId, packageName),
                    ),
                )
                if (result.size >= CrashLogPaths.MAX_SCAN_FILES) {
                    Log.w("CrashLogCacheScanner", "Hit scan file limit (${CrashLogPaths.MAX_SCAN_FILES})")
                    return result
                }
            }
        }
        return result
    }
}
