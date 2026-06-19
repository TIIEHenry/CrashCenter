package nota.android.crash.xp.app.observe

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import nota.android.crash.xp.app.R
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

    private fun configureBottomSheetAppearance() {
        val sheetDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = sheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet,
        ) ?: return

        val radius = resources.getDimension(R.dimen.radius_mobile_sheet)
        val shapeAppearance = ShapeAppearanceModel.builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, radius)
            .setTopRightCorner(CornerFamily.ROUNDED, radius)
            .build()
        val sheetBackground = MaterialShapeDrawable(shapeAppearance).apply {
            fillColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.surface),
            )
            setStroke(
                1.0f,
                ContextCompat.getColor(requireContext(), R.color.outlineVariant),
            )
        }
        bottomSheet.background = sheetBackground
        bottomSheet.clipToOutline = true
        bottomSheet.elevation = resources.getDimension(R.dimen.sheet_elevation)

        val behavior = BottomSheetBehavior.from(bottomSheet)
        val displayHeight = resources.displayMetrics.heightPixels
        behavior.peekHeight = (displayHeight * SHEET_HEIGHT_HALF_RATIO).toInt()
        behavior.isFitToContents = false
        behavior.skipCollapsed = false
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
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
        Thread {
            val event = FileCrashLogRepository(requireContext()).getById(crashId)
            val stackTrace = CrashDetailLoader.loadStackTraceById(requireContext(), crashId)
                ?: getString(R.string.crash_detail_not_found, crashId)
            val title = event?.shortExceptionClass
                ?: titleFromStackTrace(stackTrace)
                ?: getString(R.string.crash_info_title)
            Handler(Looper.getMainLooper()).post {
                if (_binding == null) return@post
                binding.tvTitle.text = title
                viewer?.showStackTrace(stackTrace)
            }
        }.start()
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
        private const val SHEET_HEIGHT_HALF_RATIO = 0.5f

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
