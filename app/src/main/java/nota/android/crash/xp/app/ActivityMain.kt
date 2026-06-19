package nota.android.crash.xp.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import nota.android.crash.xp.app.shell.MainShellActivity

/**
 * Migration wrapper — forwards to [MainShellActivity].
 * Kept for internal compatibility; launcher entry is MainShellActivity.
 */
class ActivityMain : AppCompatActivity() {

    fun isModuleActived(): Boolean = ModuleActivation.isModuleActived()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainShellActivity::class.java).apply {
                action = intent.action
                data = intent.data
                putExtras(intent)
            },
        )
        finish()
    }
}
