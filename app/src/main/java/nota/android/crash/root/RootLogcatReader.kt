package nota.android.crash.root

import android.content.Context
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
    suspend fun readBuffer(context: Context, buffer: LogcatBuffer, maxLines: Int = 5000): String? {
        return try {
            val result = Shell.cmd("su -c logcat -b ${buffer.id} -d -v threadtime -t $maxLines").exec()
            if (result.isSuccess) result.out.joinToString("\n") else null
        } catch (e: Exception) {
            Log.w(TAG, "readBuffer failed", e)
            null
        }
    }

    /**
     * Check if root logcat access is available.
     */
    fun isAvailable(context: Context): Boolean {
        return try {
            val shell = Shell.getShell()
            shell.isRoot
        } catch (_: Throwable) {
            false
        }
    }
}
