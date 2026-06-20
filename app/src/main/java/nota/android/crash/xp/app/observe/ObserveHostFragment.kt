package nota.android.crash.xp.app.observe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.FragmentObserveHostBinding

class ObserveHostFragment : Fragment() {

    private var _binding: FragmentObserveHostBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentObserveHostBinding.inflate(inflater, container, false)
        return checkNotNull(_binding) { "Binding accessed after onDestroyView" }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.commit {
                replace(R.id.observeContent, CrashHistoryFragment.newInstance(), CrashHistoryFragment.TAG)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "observe"

        fun newInstance(): ObserveHostFragment = ObserveHostFragment()
    }
}
