package nota.android.crash.xp.app.observe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.configureBottomSheetAppearance
import nota.android.crash.xp.app.data.CrashDetailLoader
import nota.android.crash.xp.app.data.FileCrashLogRepository
import nota.android.crash.xp.app.databinding.BottomSheetCrashDetailBinding
import nota.android.crash.xp.app.view.CrashLogViewerClient

class CrashDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCrashDetailBinding? = null
    private val binding get() = _binding!!

    private var viewer: CrashLogViewerClient? = null

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
        loadContent()
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

    private fun loadContent() {
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
        lifecycleScope.launch {
            val event = withContext(Dispatchers.IO) {
                FileCrashLogRepository(requireContext()).getById(crashId)
            }
            val stackTrace = withContext(Dispatchers.IO) {
                CrashDetailLoader.loadStackTraceById(requireContext(), crashId)
                    ?: getString(R.string.crash_detail_not_found, crashId)
            }
            val title = event?.shortExceptionClass
                ?: titleFromStackTrace(stackTrace)
                ?: getString(R.string.crash_info_title)
            if (_binding == null) return@launch
            binding.tvTitle.text = title
            viewer?.showStackTrace(stackTrace)
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
