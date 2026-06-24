package nota.android.crash.root

interface RootAccessClient {
    fun probe(): RootAvailability
    suspend fun fileStat(path: String): RootFileStat?
    suspend fun readText(path: String): String?
    suspend fun listDir(path: String): List<String>
    suspend fun appendBytes(path: String, data: ByteArray, deadlineMs: Long): Boolean
    suspend fun writeText(path: String, content: String): Boolean
    suspend fun delete(path: String): Boolean
}
