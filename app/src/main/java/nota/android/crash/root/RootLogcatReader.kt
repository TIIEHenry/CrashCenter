package nota.android.crash.root

import android.util.Log
import com.topjohnwu.superuser.Shell
import nota.android.crash.xp.app.observe.LogcatBuffer

/**
 * Reads Android logcat buffers via root shell.
 */
object RootLogcatReader {

    private const val TAG = "RootLogcatReader"

    /**
     * Read [buffer] via root shell.
     * Returns raw logcat output, or null if root is unavailable or command fails.
     */
    suspend fun readBuffer(buffer: LogcatBuffer, maxLines: Int = 5000): String? {
        if (!isAvailable()) return null
        return try {
            val result = Shell.cmd(
                "logcat -b ${buffer.id} -d -v threadtime -t $maxLines",
            ).exec()
            if (result.isSuccess) result.out.joinToString("\n") else null
        } catch (e: Exception) {
            Log.w(TAG, "readBuffer failed", e)
            null
        }
    }

    /** Check if root shell is ready for logcat commands. */
    fun isAvailable(): Boolean {
        return try {
            Shell.getShell().isRoot
        } catch (_: Throwable) {
            false
        }
    }
}
