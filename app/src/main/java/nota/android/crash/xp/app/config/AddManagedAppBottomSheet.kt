package nota.android.crash.xp.app.config

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.DenseSearchField
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.databinding.BottomSheetAddManagedAppBinding
import java.util.Locale

class AddManagedAppBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddManagedAppBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ManagedAppRepository
    private lateinit var adapter: PickableAppAdapter
    private var allApps: List<PickableApp> = emptyList()
    private var query: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ManagedAppRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetAddManagedAppBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupToolbar()
        setupSearch()
        loadPickableApps()
    }

    override fun onStart() {
        super.onStart()
        configureBottomSheetAppearance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
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

    private fun setupToolbar() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnDone.setOnClickListener { commitSelection() }
    }

    private fun setupSearch() {
        DenseSearchField.setOnQueryChangeListener(binding.searchField.root) { newQuery ->
            query = newQuery
            applyFilter()
        }
    }

    private fun setupList() {
        adapter = PickableAppAdapter(mutableListOf())
        adapter.onItemClick { _, data, _ ->
            adapter.toggleSelection(data)
        }
        binding.recyclerPickable.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPickable.adapter = adapter
    }

    private fun loadPickableApps() {
        binding.loadingPanel.root.visibility = View.VISIBLE
        LoadingState.bind(binding.loadingPanel.root)
        binding.recyclerPickable.visibility = View.GONE
        binding.emptyState.root.visibility = View.GONE

        Thread {
            val loaded = try {
                repository.loadPickableApps()
            } catch (_: Exception) {
                emptyList()
            }
            Handler(Looper.getMainLooper()).post {
                if (_binding == null) return@post
                allApps = loaded
                binding.loadingPanel.root.visibility = View.GONE
                applyFilter()
            }
        }.start()
    }

    private fun applyFilter() {
        val normalized = query.lowercase(Locale.getDefault())
        val visible = allApps.filter { app ->
            if (normalized.isEmpty()) return@filter true
            app.label.lowercase(Locale.getDefault()).contains(normalized) ||
                app.packageName.lowercase(Locale.getDefault()).contains(normalized)
        }
        adapter.dataList = visible.toMutableList()

        val empty = visible.isEmpty() && binding.loadingPanel.root.visibility != View.VISIBLE
        if (empty) {
            EmptyState.bind(
                binding.emptyState.root,
                getString(R.string.add_managed_picker_empty),
                R.drawable.ic_add,
            )
        }
        binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerPickable.visibility = if (!empty) View.VISIBLE else View.GONE
    }

    private fun commitSelection() {
        val selected = adapter.selectedPackages()
        if (selected.isEmpty()) {
            dismiss()
            return
        }
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putStringArrayList(ARG_PACKAGES, ArrayList(selected))
            },
        )
        Toast.makeText(
            requireContext(),
            getString(R.string.add_managed_success, selected.size),
            Toast.LENGTH_SHORT,
        ).show()
        dismiss()
    }

    companion object {
        const val TAG = "add_managed_app"
        const val REQUEST_KEY = "add_managed_app_result"
        const val ARG_PACKAGES = "packages"
        private const val SHEET_HEIGHT_HALF_RATIO = 0.5f

        fun newInstance(): AddManagedAppBottomSheet = AddManagedAppBottomSheet()
    }
}
