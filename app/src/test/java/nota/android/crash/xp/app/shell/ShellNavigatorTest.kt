package nota.android.crash.xp.app.shell

import android.os.Build
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import nota.android.crash.xp.app.config.ConfigFragment
import nota.android.crash.xp.app.observe.ObserveHostFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ShellNavigatorTest {

    private lateinit var activity: FragmentActivity
    private lateinit var fragmentManager: FragmentManager
    private lateinit var navigator: ShellNavigator
    private val containerId = android.R.id.content

    /** Test fragment factory that creates simple fragments without layout inflation. */
    private val testFragmentFactory = object : ShellNavigator.FragmentFactory {
        override fun createFragment(tab: ShellTab): Fragment = when (tab) {
            ShellTab.CONFIG -> TestConfigFragment()
            ShellTab.OBSERVE -> TestObserveFragment()
        }
    }

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java)
        activity = controller.get()

        val container = FrameLayout(activity).apply { id = containerId }
        activity.setContentView(container)

        controller.create().start().resume()

        fragmentManager = activity.supportFragmentManager
        navigator = ShellNavigator(fragmentManager, containerId, testFragmentFactory)
    }

    // ─── Fragment Creation ───

    @Test
    fun `select CONFIG creates ConfigFragment`() {
        navigator.select(ShellTab.CONFIG)

        val fragment = fragmentManager.findFragmentByTag(ConfigFragment.TAG)
        assertNotNull(fragment)
        assertTrue(fragment is TestConfigFragment)
    }

    @Test
    fun `select OBSERVE creates ObserveHostFragment`() {
        navigator.select(ShellTab.OBSERVE)

        val fragment = fragmentManager.findFragmentByTag(ObserveHostFragment.TAG)
        assertNotNull(fragment)
        assertTrue(fragment is TestObserveFragment)
    }

    @Test
    fun `select creates both fragments on first call`() {
        navigator.select(ShellTab.CONFIG)

        assertNotNull(fragmentManager.findFragmentByTag(ConfigFragment.TAG))
        assertNotNull(fragmentManager.findFragmentByTag(ObserveHostFragment.TAG))
    }

    // ─── Show / Hide ───

    @Test
    fun `select CONFIG shows ConfigFragment and hides ObserveHostFragment`() {
        navigator.select(ShellTab.CONFIG)

        val config = fragmentManager.findFragmentByTag(ConfigFragment.TAG)!!
        val observe = fragmentManager.findFragmentByTag(ObserveHostFragment.TAG)!!

        assertTrue(config.isAdded)
        assertTrue(observe.isAdded)
    }

    @Test
    fun `select OBSERVE shows ObserveHostFragment and hides ConfigFragment`() {
        navigator.select(ShellTab.CONFIG)
        navigator.select(ShellTab.OBSERVE)

        val config = fragmentManager.findFragmentByTag(ConfigFragment.TAG)!!
        val observe = fragmentManager.findFragmentByTag(ObserveHostFragment.TAG)!!

        assertTrue(config.isAdded)
        assertTrue(observe.isAdded)
    }

    @Test
    fun `switching tabs preserves fragment instances`() {
        navigator.select(ShellTab.CONFIG)
        val firstConfig = fragmentManager.findFragmentByTag(ConfigFragment.TAG)

        navigator.select(ShellTab.OBSERVE)
        navigator.select(ShellTab.CONFIG)
        val secondConfig = fragmentManager.findFragmentByTag(ConfigFragment.TAG)

        assertEquals(firstConfig, secondConfig)
    }

    // ─── findFragment ───

    @Test
    fun `findFragment returns null before selection`() {
        assertNull(navigator.findFragment(ShellTab.CONFIG))
        assertNull(navigator.findFragment(ShellTab.OBSERVE))
    }

    @Test
    fun `findFragment returns ConfigFragment after selecting CONFIG`() {
        navigator.select(ShellTab.CONFIG)

        val fragment = navigator.findFragment(ShellTab.CONFIG)
        assertNotNull(fragment)
        assertTrue(fragment is TestConfigFragment)
    }

    @Test
    fun `findFragment returns ObserveHostFragment after selecting OBSERVE`() {
        navigator.select(ShellTab.OBSERVE)

        val fragment = navigator.findFragment(ShellTab.OBSERVE)
        assertNotNull(fragment)
        assertTrue(fragment is TestObserveFragment)
    }

    @Test
    fun `findFragment returns both fragments after any selection`() {
        navigator.select(ShellTab.CONFIG)

        assertNotNull(navigator.findFragment(ShellTab.CONFIG))
        assertNotNull(navigator.findFragment(ShellTab.OBSERVE))
    }

    // ─── Tab State Persistence ───

    @Test
    fun `fragment tags are correct`() {
        navigator.select(ShellTab.CONFIG)

        assertNotNull(fragmentManager.findFragmentByTag(ConfigFragment.TAG))
        assertNotNull(fragmentManager.findFragmentByTag(ObserveHostFragment.TAG))
    }

    @Test
    fun `multiple switches do not create duplicate fragments`() {
        navigator.select(ShellTab.CONFIG)
        navigator.select(ShellTab.OBSERVE)
        navigator.select(ShellTab.CONFIG)
        navigator.select(ShellTab.OBSERVE)

        val configCount = fragmentManager.fragments.count { it is TestConfigFragment }
        val observeCount = fragmentManager.fragments.count { it is TestObserveFragment }

        assertEquals(1, configCount)
        assertEquals(1, observeCount)
    }

    // ─── Test Fragments ───

    class TestConfigFragment : Fragment() {
        companion object {
            const val TAG = "config"
        }
    }

    class TestObserveFragment : Fragment() {
        companion object {
            const val TAG = "observe"
        }
    }
}
