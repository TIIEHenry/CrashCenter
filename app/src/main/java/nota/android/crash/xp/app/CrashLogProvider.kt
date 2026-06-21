package nota.android.crash.xp.app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import nota.android.crash.log.BackendId
import nota.android.crash.log.CanonicalJsonlWriter
import nota.android.crash.log.CrashLogContract
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.FileCrashLogRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Exported ContentProvider for hook-side crash log IPC (ADR-007 / IS-4).
 * No signature permission — hook runs under target app UID.
 */
class CrashLogProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != MATCH_EVENTS) return null
        val payload = values?.getAsString(CrashLogContract.COLUMN_PAYLOAD) ?: return null
        val claimedPackage = values.getAsString(CrashLogContract.COLUMN_PACKAGE_NAME) ?: return null
        if (!validateCaller(claimedPackage)) return null
        if (!checkRateLimit()) return null
        if (payload.length > MAX_PAYLOAD_CHARS) return null

        val event = CrashEvent.fromJson(payload) ?: return null
        if (event.packageName != claimedPackage) return null

        return try {
            val ctx = context ?: return null
            val eventsFile = FileCrashLogRepository.eventsFile(ctx)
            val stamped = event.withBackendWritten(
                (event.backendWritten + BackendId.PROVIDER_INSERT.wireName),
            )
            CanonicalJsonlWriter.append(eventsFile, stamped)
            Uri.withAppendedPath(uri, event.id)
        } catch (_: Throwable) {
            null
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    internal fun validateCaller(claimedPackage: String): Boolean {
        val callingUid = Binder.getCallingUid()
        if (callingUid == android.os.Process.myUid()) return true
        val packages = context?.packageManager?.getPackagesForUid(callingUid) ?: return false
        return packages.contains(claimedPackage)
    }

    private val rateLock = Any()

    internal fun checkRateLimit(): Boolean {
        val callingUid = Binder.getCallingUid()
        val now = System.currentTimeMillis()
        val windowStart = synchronized(rateLock) {
            val existing = rateWindowStart[callingUid]
            if (existing == null || now - existing > RATE_WINDOW_MS) {
                rateWindowStart[callingUid] = now
                rateCounts[callingUid] = 0
                now
            } else {
                existing
            }
        }
        if (now - windowStart > RATE_WINDOW_MS) {
            synchronized(rateLock) {
                rateWindowStart[callingUid] = now
                rateCounts[callingUid] = 0
            }
        }
        val count = synchronized(rateLock) {
            val newCount = (rateCounts[callingUid] ?: 0) + 1
            rateCounts[callingUid] = newCount
            newCount
        }
        return count <= MAX_INSERTS_PER_WINDOW
    }

    companion object {
        private const val MATCH_EVENTS = 1
        private const val MAX_PAYLOAD_CHARS = 64 * 1024
        private const val RATE_WINDOW_MS = 60_000L
        private const val MAX_INSERTS_PER_WINDOW = 30

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(CrashLogContract.AUTHORITY, CrashLogContract.PATH_EVENTS, MATCH_EVENTS)
        }

        private val rateWindowStart = ConcurrentHashMap<Int, Long>()
        private val rateCounts = ConcurrentHashMap<Int, Int>()
    }
}
