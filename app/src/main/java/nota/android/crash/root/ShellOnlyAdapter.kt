package nota.android.crash.root

import android.util.Base64
import com.topjohnwu.superuser.Shell
import java.io.InputStream

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
        } catch (_: Exception) {
            RootAvailability.UNAVAILABLE
        }
    }

    override suspend fun readText(path: String): String? {
        return try {
            val result = Shell.cmd("timeout $TIMEOUT_SEC su -c cat \"$path\"").exec()
            if (result.isSuccess) result.out.joinToString("\n") else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun listDir(path: String): List<String> {
        return try {
            val result = Shell.cmd("timeout $TIMEOUT_SEC su -c ls \"$path\"").exec()
            if (result.isSuccess) result.out else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun openRead(path: String): InputStream? {
        // Not supported via shell — callers should use RootServiceRemoteAdapter
        return null
    }

    override suspend fun appendBytes(path: String, data: ByteArray, deadlineMs: Long): Boolean {
        return try {
            val encoded = Base64.encodeToString(data, Base64.NO_WRAP)
            val remainingSec = ((deadlineMs - System.currentTimeMillis()) / 1000)
                .coerceIn(1, 5)
            val result = Shell.cmd(
                "timeout $remainingSec sh -c 'printf %s \"$encoded\" | base64 -d >> \"$path\"'"
            ).exec()
            result.isSuccess
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun delete(path: String): Boolean {
        return try {
            val result = Shell.cmd("timeout $TIMEOUT_SEC su -c rm \"$path\"").exec()
            result.isSuccess
        } catch (_: Exception) {
            false
        }
    }
}
