package nota.android.crash.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShellOnlyAdapterTest {

    private lateinit var shellStatic: MockedStatic<Shell>
    private lateinit var adapter: ShellOnlyAdapter

    @Before
    fun setUp() {
        shellStatic = Mockito.mockStatic(Shell::class.java)
        adapter = ShellOnlyAdapter()
    }

    @After
    fun tearDown() {
        shellStatic.close()
    }

    // --- probe ---

    @Test
    fun `probe returns AVAILABLE when cached shell is root`() {
        val shell = mock<Shell>()
        whenever(shell.isRoot).thenReturn(true)
        shellStatic.`when`<Shell?> { Shell.getCachedShell() }.thenReturn(shell)

        assertEquals(RootAvailability.AVAILABLE, adapter.probe())
    }

    @Test
    fun `probe returns DENIED when cached shell is not root`() {
        val shell = mock<Shell>()
        whenever(shell.isRoot).thenReturn(false)
        shellStatic.`when`<Shell?> { Shell.getCachedShell() }.thenReturn(shell)

        assertEquals(RootAvailability.DENIED, adapter.probe())
    }

    @Test
    fun `probe returns AVAILABLE when getShell is root`() {
        shellStatic.`when`<Shell?> { Shell.getCachedShell() }.thenReturn(null)
        val shell = mock<Shell>()
        whenever(shell.isRoot).thenReturn(true)
        shellStatic.`when`<Shell> { Shell.getShell() }.thenReturn(shell)

        assertEquals(RootAvailability.AVAILABLE, adapter.probe())
    }

    @Test
    fun `probe returns UNAVAILABLE when getShell throws`() {
        shellStatic.`when`<Shell?> { Shell.getCachedShell() }.thenReturn(null)
        shellStatic.`when`<Shell> { Shell.getShell() }.thenThrow(RuntimeException("no root"))

        assertEquals(RootAvailability.UNAVAILABLE, adapter.probe())
    }

    // --- readText uses su -c ---

    @Test
    fun `readText uses su -c command`() {
        val result = mock<Shell.Result>()
        whenever(result.isSuccess).thenReturn(true)
        whenever(result.out).thenReturn(listOf("file content"))

        val job = mock<Shell.Job>()
        whenever(job.exec()).thenReturn(result)

        shellStatic.`when`<Shell.Job> { Shell.cmd(any<String>()) }.thenReturn(job)

        val text = runBlocking { adapter.readText("/data/test.txt") }

        assertEquals("file content", text)
        shellStatic.verify {
            Shell.cmd(argThat<String> { contains("su -c cat") })
        }
    }

    @Test
    fun `readText returns null on failure`() {
        val result = mock<Shell.Result>()
        whenever(result.isSuccess).thenReturn(false)

        val job = mock<Shell.Job>()
        whenever(job.exec()).thenReturn(result)

        shellStatic.`when`<Shell.Job> { Shell.cmd(any<String>()) }.thenReturn(job)

        val text = runBlocking { adapter.readText("/data/test.txt") }
        assertNull(text)
    }

    // --- listDir uses su -c ---

    @Test
    fun `listDir uses su -c command`() {
        val result = mock<Shell.Result>()
        whenever(result.isSuccess).thenReturn(true)
        whenever(result.out).thenReturn(listOf("file1", "file2"))

        val job = mock<Shell.Job>()
        whenever(job.exec()).thenReturn(result)

        shellStatic.`when`<Shell.Job> { Shell.cmd(any<String>()) }.thenReturn(job)

        val files = runBlocking { adapter.listDir("/data/") }

        assertEquals(listOf("file1", "file2"), files)
        shellStatic.verify {
            Shell.cmd(argThat<String> { contains("su -c ls") })
        }
    }

    @Test
    fun `listDir returns empty list on failure`() {
        val result = mock<Shell.Result>()
        whenever(result.isSuccess).thenReturn(false)

        val job = mock<Shell.Job>()
        whenever(job.exec()).thenReturn(result)

        shellStatic.`when`<Shell.Job> { Shell.cmd(any<String>()) }.thenReturn(job)

        val files = runBlocking { adapter.listDir("/data/") }
        assertTrue(files.isEmpty())
    }

    // --- appendBytes uses su -c (P0 fix verification) ---

    @Test
    fun `appendBytes uses su -c not sh -c`() {
        val result = mock<Shell.Result>()
        whenever(result.isSuccess).thenReturn(true)

        val job = mock<Shell.Job>()
        whenever(job.exec()).thenReturn(result)

        shellStatic.`when`<Shell.Job> { Shell.cmd(any<String>()) }.thenReturn(job)

        val success = runBlocking {
            adapter.appendBytes("/data/log.jsonl", "test".toByteArray(), System.currentTimeMillis() + 5000)
        }

        assertTrue(success)
        shellStatic.verify {
            Shell.cmd(argThat<String> {
                contains("su -c") && !contains("sh -c")
            })
        }
    }

    @Test
    fun `appendBytes returns false on failure`() {
        val result = mock<Shell.Result>()
        whenever(result.isSuccess).thenReturn(false)

        val job = mock<Shell.Job>()
        whenever(job.exec()).thenReturn(result)

        shellStatic.`when`<Shell.Job> { Shell.cmd(any<String>()) }.thenReturn(job)

        val success = runBlocking {
            adapter.appendBytes("/data/log.jsonl", "test".toByteArray(), System.currentTimeMillis() + 5000)
        }

        assertFalse(success)
    }

    @Test
    fun `appendBytes returns false on exception`() {
        shellStatic.`when`<Shell.Job> { Shell.cmd(any<String>()) }
            .thenThrow(RuntimeException("shell error"))

        val success = runBlocking {
            adapter.appendBytes("/data/log.jsonl", "test".toByteArray(), System.currentTimeMillis() + 5000)
        }

        assertFalse(success)
    }

    // --- delete uses su -c ---

    @Test
    fun `delete uses su -c command`() {
        val result = mock<Shell.Result>()
        whenever(result.isSuccess).thenReturn(true)

        val job = mock<Shell.Job>()
        whenever(job.exec()).thenReturn(result)

        shellStatic.`when`<Shell.Job> { Shell.cmd(any<String>()) }.thenReturn(job)

        val success = runBlocking { adapter.delete("/data/test.txt") }

        assertTrue(success)
        shellStatic.verify {
            Shell.cmd(argThat<String> { contains("su -c rm") })
        }
    }

    @Test
    fun `delete returns false on failure`() {
        val result = mock<Shell.Result>()
        whenever(result.isSuccess).thenReturn(false)

        val job = mock<Shell.Job>()
        whenever(job.exec()).thenReturn(result)

        shellStatic.`when`<Shell.Job> { Shell.cmd(any<String>()) }.thenReturn(job)

        val success = runBlocking { adapter.delete("/data/test.txt") }
        assertFalse(success)
    }

    // --- openRead returns null ---

    @Test
    fun `openRead returns null`() {
        val result = runBlocking { adapter.openRead("/data/test.txt") }
        assertNull(result)
    }
}
