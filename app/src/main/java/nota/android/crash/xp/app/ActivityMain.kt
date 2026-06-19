package nota.android.crash.xp.app

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import nota.android.crash.xp.app.recyclerhelper.RecyclerAdapter
import nota.android.crash.xp.app.recyclerhelper.ViewHolder
import nota.android.crash.xp.app.ArrayUtil.filter
import nota.android.crash.xp.app.ArrayUtil.map
import nota.android.crash.xp.PrefManager.ITSELF
import nota.android.crash.xp.PrefManager.PREF_HANDLE_SYSTEM
import nota.android.crash.xp.PrefManager.PREF_NAME
import nota.android.crash.xp.PrefManager.PREF_PACKAGE_LIST
import nota.android.crash.xp.PrefManager.PREF_SCOPE_MODE
import nota.android.crash.xp.PrefManager.PREF_SHOW_SYSTEM_UI
import nota.android.crash.xp.PrefMigrator
import nota.android.crash.xp.XposedManagerLauncher
import nota.android.crash.xp.app.databinding.ActivityMainBinding
import java.util.Collections
import java.util.Locale

class ActivityMain : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var showSystem = false
    private var searchQuery = ""
    private var hookFilter: HookFilter = HookFilter.ALL
    private val mApps: MutableList<App> = ArrayList()
    private val mAppAdapter = AppAdapter(mApps)
    private val mSharedPreferences: SharedPreferences by lazy {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    private var mInitApps: List<App> = ArrayList()
    private val mFilteredApps = ArrayList<App>()
    private var packageVisibilityStatus: PackageVisibilityHelper.Status? = null
    private var returningFromPermissionSettings = false

    private enum class HookFilter { ALL, ON, OFF }

    override fun onCreate(savedInstanceState: Bundle?) {
        PrefMigrator.migrateIfNeeded(applicationContext)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBars.setup(this)
        SystemBars.applyToolbarHeaderInsets(binding.toolbarHeader)

        setSupportActionBar(binding.toolbar)

        showSystem = mSharedPreferences.getBoolean(PREF_SHOW_SYSTEM_UI, false)

        mAppAdapter.onItemClick { rootView, data, _ ->
            data.get_it = !data.get_it
            rootView.findViewById<SwitchMaterial>(R.id.sw).isChecked = data.get_it
            updatePref()
            refreshAppCount()
        }

        binding.recyclerv.apply {
            adapter = mAppAdapter
            itemAnimator = DefaultItemAnimator().apply { addDuration = 100 }
            layoutManager = LinearLayoutManager(this@ActivityMain, RecyclerView.VERTICAL, false)
        }

        setupSettingsChips()
        setupSearch()
        setupFilterChips()
        setupStatusBanner()
        setupPermissionBanner()
        refreshPackageVisibilityAndApps()
        updateXposedStatusBanner()
    }

    override fun onResume() {
        super.onResume()
        if (returningFromPermissionSettings) {
            returningFromPermissionSettings = false
            refreshPackageVisibilityAndApps()
        }
    }

    private fun setupPermissionBanner() {
        val openSettingsFlow = View.OnClickListener { showPermissionRationaleDialog() }
        binding.permissionBanner.setOnClickListener(openSettingsFlow)
        binding.permissionGrantButton.setOnClickListener(openSettingsFlow)
    }

    private fun refreshPackageVisibilityAndApps() {
        packageVisibilityStatus = PackageVisibilityHelper.check(this)
        updatePermissionBanner(packageVisibilityStatus!!)
        initApps(forceReload = true)
    }

    private fun updatePermissionBanner(status: PackageVisibilityHelper.Status) {
        val show = status.needsUserAction
        binding.permissionBanner.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.permissionBannerTitle.text = if (status.visiblePackageCount > 0) {
                getString(R.string.permission_list_partial_hint, status.visiblePackageCount)
            } else {
                getString(R.string.permission_banner_title)
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_rationale_title)
            .setMessage(R.string.permission_rationale_message)
            .setPositiveButton(R.string.permission_open_settings) { _, _ ->
                returningFromPermissionSettings = true
                if (!PackageVisibilityHelper.openAppSettings(this)) {
                    returningFromPermissionSettings = false
                    Toast.makeText(this, R.string.permission_settings_failed, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupStatusBanner() {
        binding.statusBanner.setOnClickListener {
            if (!XposedManagerLauncher.open(this)) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.xposed_not_active)
                    .setMessage(R.string.xposed_hint)
                    .show()
            }
        }
    }

    private fun setupSettingsChips() {
        binding.chipScopeMode.apply {
            isChecked = mSharedPreferences.getBoolean(PREF_SCOPE_MODE, false)
            setOnCheckedChangeListener { _, isChecked ->
                mSharedPreferences.edit().putBoolean(PREF_SCOPE_MODE, isChecked).apply()
            }
            setOnLongClickListener {
                AlertDialog.Builder(this@ActivityMain)
                    .setTitle(R.string.item_scope_mode)
                    .setMessage(R.string.scope_mode_description)
                    .show()
                true
            }
        }

        binding.chipHandleSystem.apply {
            isChecked = mSharedPreferences.getBoolean(PREF_HANDLE_SYSTEM, false)
            setOnCheckedChangeListener { _, isChecked ->
                mSharedPreferences.edit().putBoolean(PREF_HANDLE_SYSTEM, isChecked).apply()
            }
        }

        binding.chipShowSystem.apply {
            isChecked = showSystem
            setOnCheckedChangeListener { _, isChecked ->
                showSystem = isChecked
                mSharedPreferences.edit().putBoolean(PREF_SHOW_SYSTEM_UI, showSystem).apply()
                applyFiltersAndSort(preserveSort = true)
            }
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString().orEmpty()
                applyFiltersAndSort(preserveSort = true)
            }
        })
    }

    private fun setupFilterChips() {
        binding.filterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            hookFilter = when (checkedIds.firstOrNull()) {
                R.id.chipOn -> HookFilter.ON
                R.id.chipOff -> HookFilter.OFF
                else -> HookFilter.ALL
            }
            applyFiltersAndSort(preserveSort = true)
        }
    }

    private var appsLoadGeneration = 0

    private fun initApps(forceReload: Boolean = false) {
        if (!forceReload && mInitApps.isNotEmpty()) {
            return
        }

        val prefWhiteList = mSharedPreferences.getStringSet(PREF_PACKAGE_LIST, null)
        showLoading(true)
        val loadGeneration = ++appsLoadGeneration

        Thread {
            val loadedApps = try {
                val packageManager = applicationContext.packageManager
                val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    packageManager.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
                } else {
                    packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
                }
                filter(map(installedPackages) { packageInfo ->
                    val appInfo = packageInfo.applicationInfo ?: return@map null
                    App(
                        appInfo.loadLabel(packageManager).toString(),
                        appInfo.loadIcon(packageManager),
                        prefWhiteList == null || !prefWhiteList.contains(packageInfo.packageName),
                        packageInfo.packageName,
                        appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        packageInfo.lastUpdateTime,
                        packageInfo.firstInstallTime
                    )
                }.filterNotNull()) { app -> app.packageName != ITSELF }
            } catch (_: Exception) {
                emptyList()
            }

            Handler(Looper.getMainLooper()).post {
                if (isFinishing || loadGeneration != appsLoadGeneration) return@post
                mInitApps = loadedApps
                val afterLoadStatus = PackageVisibilityHelper.checkAfterLoad(
                    this@ActivityMain,
                    loadedApps.size,
                )
                packageVisibilityStatus = afterLoadStatus
                updatePermissionBanner(afterLoadStatus)
                showLoading(false)
                initFilterApps()
            }
        }.start()
    }

    private fun showLoading(loading: Boolean) {
        binding.loadingPanel.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.recyclerv.visibility = View.GONE
            binding.emptyState.visibility = View.GONE
        } else {
            updateListVisibility()
        }
    }

    private fun updateListVisibility() {
        val empty = mFilteredApps.isEmpty()
        binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerv.visibility = if (empty) View.GONE else View.VISIBLE
    }

    fun isModuleActived(): Boolean {
        return false
    }

    private fun updateXposedStatusBanner() {
        val active = isModuleActived()

        if (active) {
            binding.statusBanner.setBackgroundResource(R.drawable.bg_status_active)
            binding.statusIcon.setImageResource(R.drawable.ic_shield_check)
            binding.statusTitle.setText(R.string.xposed_active_inline)
            binding.statusTitle.setTextColor(ContextCompat.getColor(this, R.color.statusActiveText))
        } else {
            binding.statusBanner.setBackgroundResource(R.drawable.bg_status_inactive)
            binding.statusIcon.setImageResource(R.drawable.ic_shield_off)
            binding.statusTitle.setText(R.string.xposed_inactive_inline)
            binding.statusTitle.setTextColor(ContextCompat.getColor(this, R.color.statusInactiveText))
        }
    }

    private fun checkXposedStatus() {
        if (!isModuleActived()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.xposed_not_active)
                .setMessage(R.string.xposed_hint)
                .show()
        }
    }

    private fun initFilterApps() {
        applyFiltersAndSort(preserveSort = false)
        checkXposedStatus()
    }

    internal fun updatePref() {
        val stringSet = HashSet<String>()
        for (app in mInitApps) {
            if (!app.get_it) {
                stringSet.add(app.packageName)
            }
        }
        mSharedPreferences.edit()
            .putStringSet(PREF_PACKAGE_LIST, stringSet)
            .apply()
    }

    private fun applyFiltersAndSort(preserveSort: Boolean) {
        mFilteredApps.clear()
        mFilteredApps.addAll(filter(mInitApps) { app ->
            val systemMatch = showSystem && app.is_system_app || !showSystem && !app.is_system_app
            if (!systemMatch) return@filter false

            val hookMatch = when (hookFilter) {
                HookFilter.ON -> app.get_it
                HookFilter.OFF -> !app.get_it
                HookFilter.ALL -> true
            }
            if (!hookMatch) return@filter false

            if (searchQuery.isEmpty()) return@filter true
            val query = searchQuery.lowercase(Locale.getDefault())
            app.name.lowercase(Locale.getDefault()).contains(query)
                    || app.packageName.lowercase(Locale.getDefault()).contains(query)
        })

        if (!preserveSort) {
            sort(Collections.reverseOrder { a, b -> java.lang.Long.compare(a.updateTime, b.updateTime) })
        } else {
            mAppAdapter.dataList = mFilteredApps
            refreshAppCount()
        }
    }

    internal fun sort(comparator: Comparator<App>) {
        Collections.sort(mFilteredApps, comparator)
        mAppAdapter.dataList = mFilteredApps
        refreshAppCount()
    }

    private fun refreshAppCount() {
        binding.appCount.text = getString(R.string.app_count_format, mFilteredApps.size)
        updateListVisibility()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_cancel_all -> {
                for (app in mInitApps) {
                    app.get_it = false
                }
                applyFiltersAndSort(preserveSort = true)
                updatePref()
            }
            R.id.item_select_all -> {
                for (app in mInitApps) {
                    app.get_it = true
                }
                applyFiltersAndSort(preserveSort = true)
                updatePref()
            }
            R.id.item_help -> AlertDialog.Builder(this)
                .setTitle(getString(R.string.using_warning_title))
                .setMessage(getString(R.string.using_warning))
                .show()
            R.id.item_test -> {
                Toast.makeText(this, R.string.test_hint, Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({ throw RuntimeException("just for test") }, 2000)
            }
            R.id.item_sort_by_name -> {
                item.isChecked = true
                sort(Comparator { a, b -> a.name.compareTo(b.name) })
            }
            R.id.item_sort_by_name_reverse -> {
                item.isChecked = true
                sort(Collections.reverseOrder { a, b -> a.name.compareTo(b.name) })
            }
            R.id.item_sort_by_install_time -> {
                item.isChecked = true
                sort(Comparator { a, b -> java.lang.Long.compare(a.installTime, b.installTime) })
            }
            R.id.item_sort_by_install_time_reverse -> {
                item.isChecked = true
                sort(Collections.reverseOrder { a, b -> java.lang.Long.compare(a.installTime, b.installTime) })
            }
            R.id.item_sort_by_update_time -> {
                item.isChecked = true
                sort(Comparator { a, b -> java.lang.Long.compare(a.updateTime, b.updateTime) })
            }
            R.id.item_sort_by_update_time_reverse -> {
                item.isChecked = true
                sort(Collections.reverseOrder { a, b -> java.lang.Long.compare(a.updateTime, b.updateTime) })
            }
        }
        return true
    }

    internal inner class AppAdapter(dataList: MutableList<App>) : RecyclerAdapter<App>(dataList) {

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): ViewHolder<App> {
            val view = LayoutInflater.from(this@ActivityMain)
                .inflate(R.layout.activity_main_appitem, parent, false)
            return ViewHolder(view)
        }

        override fun bindData(holder: ViewHolder<App>, data: App, pos: Int) {
            with(holder) {
                (getView(R.id.ivIcon) as ImageView).setImageDrawable(data.icon)
                (getView(R.id.tvName) as TextView).text = data.name
                (getView(R.id.sw) as SwitchMaterial).isChecked = data.get_it
                (getView(R.id.tvPackageName) as TextView).text = data.packageName
                getView(R.id.tvSystemBadge)?.visibility =
                    if (data.is_system_app) View.VISIBLE else View.GONE
            }
        }
    }

    internal inner class App(
        var name: String,
        var icon: Drawable,
        var get_it: Boolean,
        var packageName: String,
        var is_system_app: Boolean,
        var updateTime: Long,
        var installTime: Long
    ) {
        override fun toString(): String = "name:$name get_it:$get_it"
    }

}
