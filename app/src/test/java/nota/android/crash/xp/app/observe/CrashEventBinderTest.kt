package nota.android.crash.xp.app.observe

import android.app.Application
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.ViewCrashEventRowBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class CrashEventBinderTest {

    private lateinit var app: Application
    private lateinit var binding: ViewCrashEventRowBinding

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        val parent = FrameLayout(app)
        binding = ViewCrashEventRowBinding.inflate(
            android.view.LayoutInflater.from(app),
            parent,
            false,
        )
        binding.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun event(
        packageName: String = "com.example.app",
        appLabel: String? = "Example App",
        message: String? = "Something went wrong",
        exceptionClass: String = "java.lang.RuntimeException",
        timestampMs: Long = System.currentTimeMillis() - 60_000,
        source: String? = null,
        intercepted: Boolean = false,
    ) = CrashEvent(
        id = "test-id",
        packageName = packageName,
        appLabel = appLabel,
        message = message,
        exceptionClass = exceptionClass,
        timestampMs = timestampMs,
        source = source,
        intercepted = intercepted,
    )

    // ─── Basic field binding ───

    @Test
    fun `bind sets appName to appLabel when present`() {
        val e = event(appLabel = "My App")

        CrashEventBinder.bindGlobal(binding, e)

        assertEquals("My App", binding.tvAppName.text.toString())
    }

    @Test
    fun `bind sets subtitle with exception class and message`() {
        val e = event(
            exceptionClass = "java.lang.IllegalStateException",
            message = "bad state",
        )

        CrashEventBinder.bindGlobal(binding, e)

        val subtitle = binding.tvSubtitle.text.toString()
        assertTrue(subtitle.contains("IllegalStateException"))
        assertTrue(subtitle.contains("bad state"))
    }

    @Test
    fun `bind sets contentDescription with label subtitle and outcome`() {
        val e = event(appLabel = "TestApp", intercepted = false)

        CrashEventBinder.bindGlobal(binding, e)

        val cd = binding.root.contentDescription?.toString().orEmpty()
        assertTrue(cd.contains("TestApp"))
        assertTrue(cd.contains("Monitor only"))
    }

    @Test
    fun `bind sets icon drawable without crashing`() {
        CrashEventBinder.bindGlobal(binding, event())

        assertNotNull(binding.ivIcon.drawable)
    }

    // ─── appLabel fallback ───

    @Test
    fun `bind falls back to packageName when appLabel is null`() {
        val e = event(packageName = "com.example.pkg", appLabel = null)

        CrashEventBinder.bindGlobal(binding, e)

        assertEquals("com.example.pkg", binding.tvAppName.text.toString())
    }

    @Test
    fun `bind falls back to packageName when appLabel is empty`() {
        val e = event(packageName = "com.example.pkg", appLabel = "")

        CrashEventBinder.bindGlobal(binding, e)

        assertEquals("com.example.pkg", binding.tvAppName.text.toString())
    }

    // ─── message fallback ───

    @Test
    fun `bind falls back to packageName when message is null`() {
        val e = event(
            packageName = "com.example.pkg",
            exceptionClass = "java.lang.RuntimeException",
            message = null,
        )

        CrashEventBinder.bindGlobal(binding, e)

        val subtitle = binding.tvSubtitle.text.toString()
        assertTrue(subtitle.contains("com.example.pkg"))
    }

    @Test
    fun `bind falls back to packageName when message is blank`() {
        val e = event(
            packageName = "com.example.pkg",
            exceptionClass = "java.lang.RuntimeException",
            message = "   ",
        )

        CrashEventBinder.bindGlobal(binding, e)

        val subtitle = binding.tvSubtitle.text.toString()
        assertTrue(subtitle.contains("com.example.pkg"))
    }

    // ─── source badge ───

    @Test
    fun `bind shows source badge for uncaught source`() {
        val e = event(source = "uncaught")

        CrashEventBinder.bindGlobal(binding, e)

        assertEquals(View.VISIBLE, binding.tvSourceBadge.visibility)
        assertEquals("UEH", binding.tvSourceBadge.text.toString())
    }

    @Test
    fun `bind shows source badge for looper source`() {
        val e = event(source = "looper")

        CrashEventBinder.bindGlobal(binding, e)

        assertEquals(View.VISIBLE, binding.tvSourceBadge.visibility)
        assertEquals("Looper", binding.tvSourceBadge.text.toString())
    }

    @Test
    fun `bind hides source badge when source is null`() {
        val e = event(source = null)

        CrashEventBinder.bindGlobal(binding, e)

        assertEquals(View.GONE, binding.tvSourceBadge.visibility)
    }

    @Test
    fun `bind hides source badge when source is empty`() {
        val e = event(source = "")

        CrashEventBinder.bindGlobal(binding, e)

        assertEquals(View.GONE, binding.tvSourceBadge.visibility)
    }

    @Test
    fun `bind shows raw source label for unknown source`() {
        val e = event(source = "customSource")

        CrashEventBinder.bindGlobal(binding, e)

        assertEquals(View.VISIBLE, binding.tvSourceBadge.visibility)
        assertEquals("customSource", binding.tvSourceBadge.text.toString())
    }

    @Test
    fun `bind shows intercepted outcome badge`() {
        CrashEventBinder.bindGlobal(binding, event(intercepted = true))

        assertEquals(View.VISIBLE, binding.tvOutcomeBadge.visibility)
        assertEquals("Intercepted", binding.tvOutcomeBadge.text.toString())
    }

    @Test
    fun `bind shows monitor-only outcome badge`() {
        CrashEventBinder.bindGlobal(binding, event(intercepted = false))

        assertEquals(View.VISIBLE, binding.tvOutcomeBadge.visibility)
        assertEquals("Monitor only", binding.tvOutcomeBadge.text.toString())
    }

    // ─── Icon loading with nonexistent package ───

    @Test
    fun `bind uses fallback icon for nonexistent package`() {
        val e = event(packageName = "com.nonexistent.fake.package")

        CrashEventBinder.bindGlobal(binding, e)

        assertNotNull(binding.ivIcon.drawable)
    }
}
