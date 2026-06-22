package nota.android.crash.xp.app.observe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.configureCrashDetailBottomSheetAppearance
import nota.android.crash.xp.app.data.CrashDetailLoader
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.crashDetailViewModelFactory
import nota.android.crash.xp.app.databinding.BottomSheetCrashDetailBinding
import nota.android.crash.xp.app.view.CrashLogViewerClient

sealed class CrashDetailArgs {
    abstract fun toBundle(): Bundle

    data class FromId(val crashId: String) : CrashDetailArgs() {
        override fun toBundle(): Bundle = Bundle().apply { putString(ARG_CRASH_ID, crashId) }
    }

    data class FromStackTrace(val stackTrace: String, val title: String? = null) : CrashDetailArgs() {
        override fun toBundle(): Bundle = Bundle().apply {
            putString(ARG_STACK_TRACE, stackTrace)
            title?.let { putString(ARG_TITLE, it) }
        }
    }

    companion object {
        private const val ARG_CRASH_ID = CrashDetailBottomSheet.EXTRA_CRASH_ID
        private const val ARG_STACK_TRACE = "stack_trace"
        private const val ARG_TITLE = "title"

        fun fromBundle(bundle: Bundle): CrashDetailArgs {
            val rawStack = bundle.getString(ARG_STACK_TRACE)
            return if (!rawStack.isNullOrBlank()) {
                FromStackTrace(rawStack, bundle.getString(ARG_TITLE))
            } else {
                FromId(bundle.getString(ARG_CRASH_ID).orEmpty())
            }
        }
    }
}

class CrashDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCrashDetailBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private var viewer: CrashLogViewerClient? = null
    private var currentStackTrace: String = ""

    private val viewModel: CrashDetailViewModel by viewModels {
        ServiceLocator.crashDetailViewModelFactory(this, requireContext())
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
        binding.btnCopy.setOnClickListener { copyStackTraceToClipboard() }
        viewer = CrashLogViewerClient.attach(requireContext(), binding.viewerContainer)
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        configureCrashDetailBottomSheetAppearance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewer = null
        _binding = null
    }

    private fun observeViewModel() {
        val args = CrashDetailArgs.fromBundle(requireArguments())
        when (val a = args) {
            is CrashDetailArgs.FromStackTrace -> {
                binding.tvTitle.text = a.title
                    ?: CrashDetailLoader.titleFromStackTrace(a.stackTrace)
                    ?: getString(R.string.crash_info_title)
                currentStackTrace = a.stackTrace
                viewer?.showStackTrace(a.stackTrace)
            }
            is CrashDetailArgs.FromId -> {
                if (a.crashId.isBlank()) {
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
                                    currentStackTrace = state.stackTrace
                                    viewer?.showStackTrace(state.stackTrace)
                                }
                                is CrashDetailUiState.Error -> {
                                    if (_binding == null) return@collect
                                    binding.tvTitle.text = getString(R.string.crash_info_title)
                                    currentStackTrace = state.message
                                    viewer?.showStackTrace(state.message)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun copyStackTraceToClipboard() {
        if (currentStackTrace.isBlank()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("crash_stack_trace", currentStackTrace))
        Toast.makeText(requireContext(), R.string.crash_detail_copied, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG = "crash_detail_sheet"
        const val EXTRA_CRASH_ID = "crash_id"

        fun newInstance(args: CrashDetailArgs): CrashDetailBottomSheet =
            CrashDetailBottomSheet().apply { arguments = args.toBundle() }

        fun newInstance(crashId: String): CrashDetailBottomSheet =
            newInstance(CrashDetailArgs.FromId(crashId))

        fun newInstanceStackTrace(stackTrace: String, title: String? = null): CrashDetailBottomSheet =
            newInstance(CrashDetailArgs.FromStackTrace(stackTrace, title))
    }
}
