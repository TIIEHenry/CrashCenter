package nota.android.crash.xp.app.config

import android.content.Context
import android.content.SharedPreferences
import nota.android.crash.xp.PrefManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ManagedAppRepositoryTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var repository: ManagedAppRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository = ManagedAppRepository(context, prefs)
    }

    @Test
    fun `readInterceptEnabledPackages returns empty when unset`() {
        assertTrue(repository.readInterceptEnabledPackages().isEmpty())
    }

    @Test
    fun `setInterceptEnabled adds and removes packages`() {
        repository.setInterceptEnabled("com.example.a", enabled = true)
        assertEquals(setOf("com.example.a"), repository.readInterceptEnabledPackages())

        repository.setInterceptEnabled("com.example.a", enabled = false)
        assertTrue(repository.readInterceptEnabledPackages().isEmpty())
    }

    @Test
    fun `setInterceptEnabled ignores self package`() {
        repository.setInterceptEnabled(PrefManager.ITSELF, enabled = true)
        assertTrue(repository.readInterceptEnabledPackages().isEmpty())
    }

    @Test
    fun `handleSystem and showSystemUi round trip`() {
        assertFalse(repository.readHandleSystem())
        assertFalse(repository.readShowSystemUi())

        repository.setHandleSystem(true)
        repository.setShowSystemUi(true)

        assertTrue(repository.readHandleSystem())
        assertTrue(repository.readShowSystemUi())
    }
}
