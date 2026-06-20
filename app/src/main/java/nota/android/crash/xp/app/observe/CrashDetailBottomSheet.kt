package nota.android.crash.xp.app.observe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.configureBottomSheetAppearance
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.ViewModelFactory
import nota.android.crash.xp.app.databinding.BottomSheetCrashDetailBinding
import nota.android.crash.xp.app.view.CrashLogViewerClient

class CrashDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCrashDetailBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private var viewer: CrashLogViewerClient? = null

    private val viewModel: CrashDetailViewModel by viewModels {
        val crashId = arguments?.getString(ARG_CRASH_ID).orEmpty()
        ViewModelFactory {
            CrashDetailViewModel(
                crashId = crashId,
                repository = ServiceLocator.crashLogRepository(requireContext()),
                contextProvider = { requireContext() },
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetCrashDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnClickListener { dismiss() }
        viewer = CrashLogViewerClient.attach(requireContext(), binding.viewerContainer)
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        configureBottomSheetAppearance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewer = null
        _binding = null
    }

    private fun observeViewModel() {
        val args = requireArguments()
        val rawStack = args.getString(ARG_STACK_TRACE)
        if (!rawStack.isNullOrBlank()) {
            binding.tvTitle.text = args.getString(ARG_TITLE)
                ?: titleFromStackTrace(rawStack)
                ?: getString(R.string.crash_info_title)
            viewer?.showStackTrace(rawStack)
            return
        }

        val crashId = args.getString(ARG_CRASH_ID).orEmpty()
        if (crashId.isBlank()) {
            viewer?.showStackTrace("")
            binding.tvTitle.text = getString(R.string.crash_info_title)
            return
        }

        binding.tvTitle.text = getString(R.string.crash_history_loading)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CrashDetailUiState.Loading -> {
                            binding.tvTitle.text = getString(R.string.crash_history_loading)
                        }
                        is CrashDetailUiState.Success -> {
                            if (_binding == null) return@collect
                            binding.tvTitle.text = state.title
                            viewer?.showStackTrace(state.stackTrace)
                        }
                    }
                }
            }
        }
    }

    private fun titleFromStackTrace(stackTrace: String): String? {
        val firstLine = stackTrace.lineSequence().firstOrNull()?.trim().orEmpty()
        if (firstLine.isEmpty()) return null
        val exceptionToken = firstLine.substringBefore(':').trim()
        return exceptionToken.substringAfterLast('.').ifBlank { exceptionToken }
    }

    companion object {
        const val TAG = "crash_detail_sheet"
        const val ARG_CRASH_ID = CrashHistoryFragment.EXTRA_CRASH_ID
        const val ARG_STACK_TRACE = "stack_trace"
        const val ARG_TITLE = "title"

        fun newInstance(crashId: String): CrashDetailBottomSheet =
            CrashDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CRASH_ID, crashId)
                }
            }

        fun newInstanceStackTrace(stackTrace: String, title: String? = null): CrashDetailBottomSheet =
            CrashDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_STACK_TRACE, stackTrace)
                    title?.let { putString(ARG_TITLE, it) }
                }
            }
    }
}
