package nota.android.crash.xp.app.shell

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import nota.android.crash.xp.app.config.ConfigFragment
import nota.android.crash.xp.app.observe.ObserveHostFragment

/**
 * Manages the two primary shell tabs (Config / Observe) using a show/hide
 * strategy so that each fragment is created once and its state is preserved
 * across tab switches.
 */
class ShellNavigator(
    private val fragmentManager: FragmentManager,
    @IdRes private val containerId: Int,
    private val fragmentFactory: FragmentFactory = DefaultFragmentFactory(),
) {

    /** Switch to [tab], creating the fragment lazily if necessary. */
    fun select(tab: ShellTab) {
        val config = findOrCreate(ShellTab.CONFIG)
        val observe = findOrCreate(ShellTab.OBSERVE)

        fragmentManager.beginTransaction().apply {
            setReorderingAllowed(true)
            when (tab) {
                ShellTab.CONFIG -> {
                    show(config)
                    setMaxLifecycle(config, Lifecycle.State.RESUMED)
                    hide(observe)
                    setMaxLifecycle(observe, Lifecycle.State.STARTED)
                }
                ShellTab.OBSERVE -> {
                    hide(config)
                    setMaxLifecycle(config, Lifecycle.State.STARTED)
                    show(observe)
                    setMaxLifecycle(observe, Lifecycle.State.RESUMED)
                }
            }
        }.commitNow()
    }

    /** Find an existing fragment or create a new one. */
    private fun findOrCreate(tab: ShellTab): Fragment {
        val tag = tab.tag()
        return fragmentManager.findFragmentByTag(tag)
            ?: fragmentFactory.createFragment(tab).also { fragment ->
                fragmentManager.beginTransaction()
                    .add(containerId, fragment, tag)
                    .commitNow()
            }
    }

    /** Retrieve a specific tab's fragment, if it has been created. */
    fun findFragment(tab: ShellTab): Fragment? {
        return fragmentManager.findFragmentByTag(tab.tag())
    }

    private fun ShellTab.tag(): String = when (this) {
        ShellTab.CONFIG -> ConfigFragment.TAG
        ShellTab.OBSERVE -> ObserveHostFragment.TAG
    }

    /** Factory for creating fragments by tab. */
    interface FragmentFactory {
        fun createFragment(tab: ShellTab): Fragment
    }

    /** Default production factory. */
    class DefaultFragmentFactory : FragmentFactory {
        override fun createFragment(tab: ShellTab): Fragment = when (tab) {
            ShellTab.CONFIG -> ConfigFragment.newInstance()
            ShellTab.OBSERVE -> ObserveHostFragment.newInstance()
        }
    }
}
