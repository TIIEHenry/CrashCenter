package nota.android.crash.root

import java.io.InputStream

interface RootAccessClient {
    fun probe(): RootAvailability
    suspend fun readText(path: String): String?
    suspend fun listDir(path: String): List<String>
    suspend fun openRead(path: String): InputStream?
    suspend fun appendBytes(path: String, data: ByteArray, deadlineMs: Long): Boolean
    suspend fun delete(path: String): Boolean
}
