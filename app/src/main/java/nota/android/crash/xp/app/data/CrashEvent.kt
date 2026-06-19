package nota.android.crash.xp.app.data

import org.json.JSONObject

data class CrashEvent(
    val id: String,
    val timestampMs: Long,
    val packageName: String,
    val appLabel: String?,
    val processName: String?,
    val exceptionClass: String,
    val message: String?,
    val stackTrace: String,
    val source: String?,
    val backendWritten: List<String> = emptyList(),
) {
    fun withBackendWritten(backends: List<String>): CrashEvent =
        copy(backendWritten = backends.distinct())
    val shortExceptionClass: String
        get() = exceptionClass.substringAfterLast('.')

    fun toJsonLine(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("timestampMs", timestampMs)
        json.put("packageName", packageName)
        appLabel?.let { json.put("appLabel", it) }
        processName?.let { json.put("processName", it) }
        json.put("exceptionClass", exceptionClass)
        message?.let { json.put("message", it) }
        json.put("stackTrace", stackTrace)
        source?.let { json.put("source", it) }
        if (backendWritten.isNotEmpty()) {
            json.put("backendWritten", org.json.JSONArray(backendWritten))
        }
        return json.toString()
    }

    companion object {
        fun fromJson(line: String): CrashEvent? {
            return try {
                val json = JSONObject(line)
                val id = json.optString("id", "")
                if (id.isEmpty()) return null
                CrashEvent(
                    id = id,
                    timestampMs = json.optLong("timestampMs", 0L),
                    packageName = json.optString("packageName", ""),
                    appLabel = json.optString("appLabel").takeIf { it.isNotEmpty() },
                    processName = json.optString("processName").takeIf { it.isNotEmpty() },
                    exceptionClass = json.optString("exceptionClass", "Unknown"),
                    message = json.optString("message").takeIf { it.isNotEmpty() },
                    stackTrace = json.optString("stackTrace", ""),
                    source = json.optString("source").takeIf { it.isNotEmpty() },
                    backendWritten = parseBackendWritten(json),
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun parseBackendWritten(json: JSONObject): List<String> {
            val array = json.optJSONArray("backendWritten") ?: return emptyList()
            return buildList {
                for (i in 0 until array.length()) {
                    array.optString(i).takeIf { it.isNotEmpty() }?.let(::add)
                }
            }
        }
    }
}
