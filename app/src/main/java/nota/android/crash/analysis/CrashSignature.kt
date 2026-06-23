package nota.android.crash.analysis

import java.security.MessageDigest

/**
 * Normalizes stack traces for duplicate-crash clustering (4G-V2).
 * Does not persist to [nota.android.crash.common.data.CrashEvent]; computed on read in stats.
 */
object CrashSignature {

    const val MAX_FRAMES = 5
    const val CLUSTER_ID_LENGTH = 12
    const val SIGNATURE_HEX_LENGTH = 16

    private val LINE_NUMBER = Regex(""":\d+\)""")
    private val LAMBDA_SUFFIX = Regex("""\$\d+""")

    fun normalizeFrames(stackTrace: String, maxFrames: Int = MAX_FRAMES): List<String> {
        return stackTrace.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("at ") }
            .take(maxFrames)
            .map(::normalizeFrame)
            .toList()
    }

    fun signatureHash(exceptionClass: String, stackTrace: String): String {
        val payload = buildString {
            append(exceptionClass)
            append('\n')
            normalizeFrames(stackTrace).forEach { frame ->
                append(frame)
                append('\n')
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
            .take(SIGNATURE_HEX_LENGTH)
    }

    fun clusterId(signatureHash: String): String = signatureHash.take(CLUSTER_ID_LENGTH)

    fun clusterId(exceptionClass: String, stackTrace: String): String =
        clusterId(signatureHash(exceptionClass, stackTrace))

    fun firstFrameLabel(stackTrace: String): String? {
        val frame = normalizeFrames(stackTrace, maxFrames = 1).firstOrNull() ?: return null
        return frame.removePrefix("at ").substringBefore('(').trim()
    }

    private fun normalizeFrame(line: String): String {
        var normalized = LINE_NUMBER.replace(line, ")")
        normalized = LAMBDA_SUFFIX.replace(normalized, "")
        return normalized
    }
}
