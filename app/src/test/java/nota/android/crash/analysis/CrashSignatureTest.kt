package nota.android.crash.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashSignatureTest {

    private val sampleTrace = """
        java.lang.NullPointerException: null
            at com.example.app.ui.HomeFragment.onViewCreated(HomeFragment.kt:68)
            at androidx.fragment.app.Fragment.performViewCreated(Fragment.java:2987)
            at com.example.app.util.Helper${'$'}1.invoke(Helper.kt:42)
            at com.example.app.util.Helper${'$'}2.lambda${'$'}process(Helper.kt:100)
            at com.example.app.util.Helper.run(Helper.kt:110)
            at java.lang.Thread.run(Thread.java:920)
    """.trimIndent()

    // ─── normalizeFrames ───

    @Test
    fun `normalizeFrames returns empty list for empty input`() {
        val result = CrashSignature.normalizeFrames("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `normalizeFrames filters out non-at lines`() {
        val trace = """
            java.lang.RuntimeException: boom
            Caused by: java.io.IOException
                at com.example.A.foo(A.java:10)
        """.trimIndent()
        val result = CrashSignature.normalizeFrames(trace)
        assertEquals(1, result.size)
        assertTrue(result[0].startsWith("at "))
    }

    @Test
    fun `normalizeFrames strips line numbers`() {
        val trace = "\tat com.example.A.foo(A.java:42)"
        val result = CrashSignature.normalizeFrames(trace)
        assertEquals(1, result.size)
        assertEquals("at com.example.A.foo(A.java)", result[0])
    }

    @Test
    fun `normalizeFrames strips lambda suffixes`() {
        val trace = "\tat com.example.Helper\$1.invoke(Helper.kt:10)"
        val result = CrashSignature.normalizeFrames(trace)
        assertEquals(1, result.size)
        assertEquals("at com.example.Helper.invoke(Helper.kt)", result[0])
    }

    @Test
    fun `normalizeFrames respects maxFrames limit`() {
        val result = CrashSignature.normalizeFrames(sampleTrace, maxFrames = 2)
        assertEquals(2, result.size)
    }

    @Test
    fun `normalizeFrames default maxFrames is MAX_FRAMES`() {
        val result = CrashSignature.normalizeFrames(sampleTrace)
        assertEquals(CrashSignature.MAX_FRAMES, result.size)
    }

    // ─── signatureHash ───

    @Test
    fun `signatureHash is deterministic`() {
        val h1 = CrashSignature.signatureHash("java.lang.NPE", sampleTrace)
        val h2 = CrashSignature.signatureHash("java.lang.NPE", sampleTrace)
        assertEquals(h1, h2)
    }

    @Test
    fun `signatureHash produces different hashes for different inputs`() {
        val h1 = CrashSignature.signatureHash("java.lang.NPE", sampleTrace)
        val h2 = CrashSignature.signatureHash("java.lang.IOE", sampleTrace)
        assertTrue(h1 != h2)
    }

    @Test
    fun `signatureHash is truncated to SIGNATURE_HEX_LENGTH`() {
        val hash = CrashSignature.signatureHash("java.lang.NPE", sampleTrace)
        assertEquals(CrashSignature.SIGNATURE_HEX_LENGTH, hash.length)
    }

    @Test
    fun `signatureHash with empty stack trace still produces valid hash`() {
        val hash = CrashSignature.signatureHash("java.lang.NPE", "")
        assertEquals(CrashSignature.SIGNATURE_HEX_LENGTH, hash.length)
        assertTrue(hash.isNotEmpty())
    }

    // ─── clusterId ───

    @Test
    fun `clusterId is truncated to CLUSTER_ID_LENGTH`() {
        val id = CrashSignature.clusterId("java.lang.NPE", sampleTrace)
        assertEquals(CrashSignature.CLUSTER_ID_LENGTH, id.length)
    }

    @Test
    fun `clusterId from hash takes first CLUSTER_ID_LENGTH characters`() {
        val fullHash = CrashSignature.signatureHash("java.lang.NPE", sampleTrace)
        val expected = fullHash.take(CrashSignature.CLUSTER_ID_LENGTH)
        assertEquals(expected, CrashSignature.clusterId(fullHash))
    }

    @Test
    fun `clusterId is deterministic`() {
        val id1 = CrashSignature.clusterId("java.lang.NPE", sampleTrace)
        val id2 = CrashSignature.clusterId("java.lang.NPE", sampleTrace)
        assertEquals(id1, id2)
    }

    // ─── firstFrameLabel ───

    @Test
    fun `firstFrameLabel extracts class-method from first at-line`() {
        val label = CrashSignature.firstFrameLabel(sampleTrace)
        assertEquals("com.example.app.ui.HomeFragment.onViewCreated", label)
    }

    @Test
    fun `firstFrameLabel returns null for empty stack`() {
        assertNull(CrashSignature.firstFrameLabel(""))
    }

    @Test
    fun `firstFrameLabel returns null when no at lines present`() {
        val trace = """
            java.lang.RuntimeException: boom
            Caused by: java.io.IOError
        """.trimIndent()
        assertNull(CrashSignature.firstFrameLabel(trace))
    }

    @Test
    fun `firstFrameLabel strips line numbers from label`() {
        val trace = "\tat com.example.A.bar(A.java:99)"
        assertEquals("com.example.A.bar", CrashSignature.firstFrameLabel(trace))
    }
}
