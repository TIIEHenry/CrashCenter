package nota.android.crash;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import nota.android.crash.xp.app.SystemBars;
import nota.android.crash.xp.app.common.ui.ToolbarHeaderInsets;
import nota.android.crash.xp.app.data.CrashDetailLoader;
import nota.android.crash.xp.app.databinding.ActivityCrashinfoBinding;
import nota.android.crash.xp.app.observe.CrashHistoryFragment;

public class ActivityCrashInfo extends AppCompatActivity {
    private ActivityCrashinfoBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCrashinfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SystemBars.setup(this);
        ToolbarHeaderInsets.apply(binding.toolbarHeader);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        String stackTrace = resolveStackTrace(intent);
        binding.textv.setText(stackTrace != null ? stackTrace : "");
    }

    private String resolveStackTrace(Intent intent) {
        String crashId = intent.getStringExtra(CrashHistoryFragment.EXTRA_CRASH_ID);
        if (crashId != null && !crashId.isEmpty()) {
            String fromLog = CrashDetailLoader.loadStackTraceById(this, crashId);
            if (fromLog != null) {
                return fromLog;
            }
            return getString(nota.android.crash.xp.app.R.string.crash_detail_not_found, crashId);
        }
        return intent.getStringExtra("Exception");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
