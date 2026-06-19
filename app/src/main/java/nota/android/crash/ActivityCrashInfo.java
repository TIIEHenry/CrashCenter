package nota.android.crash;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import nota.android.crash.xp.app.SystemBars;
import nota.android.crash.xp.app.databinding.ActivityCrashinfoBinding;

public class ActivityCrashInfo extends AppCompatActivity {
    private ActivityCrashinfoBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCrashinfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SystemBars.setup(this);
        SystemBars.applyToolbarHeaderInsets(binding.toolbarHeader);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        String stackTrace = intent.getStringExtra("Exception");
        binding.textv.setText(stackTrace != null ? stackTrace : "");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
