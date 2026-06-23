package nota.android.crash.xp.app.observe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import nota.android.crash.analysis.RuleEngine
import nota.android.crash.common.data.CrashAnalysis
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.configureCrashDetailBottomSheetAppearance
import nota.android.crash.xp.app.data.CrashDetailLoader
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.crashDetailViewModelFactory
import nota.android.crash.xp.app.databinding.BottomSheetCrashDetailBinding

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

    private var currentStackTrace: String = ""
    private var ruleEngine: RuleEngine? = null

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
        binding.analysisDevSuggestionHeader.setOnClickListener { toggleDevSuggestion() }
        initRuleEngine()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        configureCrashDetailBottomSheetAppearance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
                binding.textStackTrace.text = a.stackTrace
                runAnalysis(a.stackTrace)
            }
            is CrashDetailArgs.FromId -> {
                if (a.crashId.isBlank()) {
                    binding.textStackTrace.text = ""
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
                                    binding.textStackTrace.text = state.stackTrace
                                    runAnalysis(state.stackTrace)
                                }
                                is CrashDetailUiState.Error -> {
                                    if (_binding == null) return@collect
                                    binding.tvTitle.text = getString(R.string.crash_info_title)
                                    currentStackTrace = state.message
                                    binding.textStackTrace.text = state.message
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

    private fun initRuleEngine() {
        ruleEngine = try {
            RuleEngine.fromAssets(requireContext())
        } catch (_: Exception) {
            null
        }
    }

    private fun runAnalysis(stackTrace: String) {
        val engine = ruleEngine ?: return
        if (_binding == null) return
        val firstLine = stackTrace.lineSequence().firstOrNull()?.trim().orEmpty()
        val exceptionClass = firstLine
            .removePrefix("Caused by:")
            .substringBefore(':')
            .substringBefore('@')
            .trim()
        val analysis = engine.match(exceptionClass, stackTrace)
        if (analysis != null) {
            showAnalysisCard(analysis)
        } else {
            binding.analysisCard.isVisible = false
        }
    }

    private fun showAnalysisCard(analysis: CrashAnalysis) {
        binding.analysisCategoryChip.text = analysis.category
        binding.analysisRootCauseGroup.removeAllViews()
        for (tag in analysis.rootCauseTags) {
            val chip = Chip(requireContext()).apply {
                text = tag
                isClickable = false
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
            }
            binding.analysisRootCauseGroup.addView(chip)
        }
        binding.analysisSuggestion.text = analysis.suggestion
        binding.analysisDevSuggestion.text = analysis.devSuggestion
        binding.analysisDevSuggestion.isVisible = false
        binding.analysisDevSuggestionHeader.text = getString(R.string.analysis_dev_suggestion_header)
        binding.analysisCard.isVisible = true
    }

    private fun toggleDevSuggestion() {
        val devText = binding.analysisDevSuggestion
        devText.isVisible = !devText.isVisible
        val header = binding.analysisDevSuggestionHeader
        val arrow = if (devText.isVisible) "▼ " else "▶ "
        header.text = arrow + getString(R.string.analysis_dev_suggestion_header)
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
