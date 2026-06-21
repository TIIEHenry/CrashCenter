package nota.android.crash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import nota.android.crash.xp.app.SystemBars
import nota.android.crash.xp.app.common.ui.ToolbarHeaderInsets
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.databinding.ActivityCrashinfoBinding
import nota.android.crash.xp.app.observe.CrashDetailBottomSheet
import nota.android.crash.xp.app.view.CrashLogViewerClient

class ActivityCrashInfo : AppCompatActivity() {
    private lateinit var binding: ActivityCrashinfoBinding
    private lateinit var viewer: CrashLogViewerClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrashinfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBars.setup(this)
        ToolbarHeaderInsets.apply(binding.toolbarHeader)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewer = CrashLogViewerClient.attach(this, binding.viewerContainer)

        val intent = intent
        val stackTrace = resolveStackTrace(intent)
        viewer.showStackTrace(stackTrace ?: "")
    }

    private fun resolveStackTrace(intent: Intent): String? {
        val crashId = intent.getStringExtra(CrashDetailBottomSheet.EXTRA_CRASH_ID)
        if (crashId != null && crashId.isNotEmpty()) {
            val repo = ServiceLocator.crashLogRepository(this)
            val event = repo.getById(crashId)
            if (event != null) {
                return nota.android.crash.xp.app.data.CrashDetailLoader.stackTraceFrom(event)
            }
            return getString(nota.android.crash.xp.app.R.string.crash_detail_not_found, crashId)
        }
        return intent.getStringExtra("Exception")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
