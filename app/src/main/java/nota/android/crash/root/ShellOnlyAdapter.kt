package nota.android.crash.root

import android.util.Base64
import android.util.Log
import com.topjohnwu.superuser.Shell

/**
 * Hook-process implementation of [RootAccessClient].
 * Uses shell commands with explicit `su -c` for each operation.
 * Does NOT hold a RootBroker reference; never calls RootService.bind().
 *
 * All operations have a 1-second shell-level timeout via the `timeout` utility.
 */
class ShellOnlyAdapter : RootAccessClient {

    companion object {
        /** Timeout in seconds for each shell command. */
        private const val TIMEOUT_SEC = 1
    }

    override fun probe(): RootAvailability {
        return try {
            val cached = Shell.getCachedShell()
            if (cached != null) {
                return when {
                    cached.isRoot -> RootAvailability.AVAILABLE
                    else -> RootAvailability.DENIED
                }
            }
            val shell = Shell.getShell()
            when {
                shell.isRoot -> RootAvailability.AVAILABLE
                else -> RootAvailability.DENIED
            }
        } catch (e: Exception) {
            Log.w("ShellOnlyAdapter", "probe failed", e)
            RootAvailability.UNAVAILABLE
        }
    }

    override suspend fun fileStat(path: String): RootFileStat? {
        return try {
            val result = Shell.cmd("timeout $TIMEOUT_SEC su -c stat -c '%Y %s' \"$path\"").exec()
            if (!result.isSuccess || result.out.isEmpty()) return null
            val parts = result.out.first().trim().split(Regex("\\s+"))
            if (parts.size < 2) return null
            val mtimeSec = parts[0].toLongOrNull() ?: return null
            val length = parts[1].toLongOrNull() ?: return null
            RootFileStat(mtimeMs = mtimeSec * 1000L, length = length)
        } catch (e: Exception) {
            Log.w("ShellOnlyAdapter", "fileStat failed", e)
            null
        }
    }

    override suspend fun readText(path: String): String? {
        return try {
            val result = Shell.cmd("timeout $TIMEOUT_SEC su -c cat \"$path\"").exec()
            if (result.isSuccess) result.out.joinToString("\n") else null
        } catch (e: Exception) {
            Log.w("ShellOnlyAdapter", "readText failed", e)
            null
        }
    }

    override suspend fun listDir(path: String): List<String> {
        return try {
            val result = Shell.cmd("timeout $TIMEOUT_SEC su -c ls \"$path\"").exec()
            if (result.isSuccess) result.out else emptyList()
        } catch (e: Exception) {
            Log.w("ShellOnlyAdapter", "listDir failed", e)
            emptyList()
        }
    }

    override suspend fun appendBytes(path: String, data: ByteArray, deadlineMs: Long): Boolean {
        return try {
            val encoded = Base64.encodeToString(data, Base64.NO_WRAP)
            val remainingSec = ((deadlineMs - System.currentTimeMillis()) / 1000)
                .coerceIn(1, 5)
            val result = Shell.cmd(
                "timeout $remainingSec su -c 'printf %s \"$encoded\" | base64 -d >> \"$path\"'"
            ).exec()
            result.isSuccess
        } catch (e: Exception) {
            Log.w("ShellOnlyAdapter", "appendBytes failed", e)
            false
        }
    }

    override suspend fun writeText(path: String, content: String): Boolean {
        return try {
            val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val parent = path.substringBeforeLast('/')
            val result = Shell.cmd(
                "timeout $TIMEOUT_SEC su -c 'mkdir -p \"$parent\" && printf %s \"$encoded\" | base64 -d > \"$path\"'"
            ).exec()
            result.isSuccess
        } catch (e: Exception) {
            Log.w("ShellOnlyAdapter", "writeText failed", e)
            false
        }
    }

    override suspend fun delete(path: String): Boolean {
        return try {
            val result = Shell.cmd("timeout $TIMEOUT_SEC su -c rm -f \"$path\"").exec()
            result.isSuccess
        } catch (e: Exception) {
            Log.w("ShellOnlyAdapter", "delete failed", e)
            false
        }
    }
}
