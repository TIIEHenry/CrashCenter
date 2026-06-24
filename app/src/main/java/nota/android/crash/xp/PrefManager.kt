package nota.android.crash.xp

object PrefManager {
    const val PACKAGE_NAME = "nota.android.crash.xp.app"
    const val PREF_NAME = "crash"
    const val PREF_HANDLE_SYSTEM = "handle_system"
    const val PREF_SHOW_SYSTEM_UI = "show_system_ui"
    const val PREF_MANAGED_PACKAGES = "managed_packages"
    const val PREF_XPOSED_DIALOG_DISMISSED = "xposed_dialog_dismissed"
    const val PREF_ROOT_DIALOG_DISMISSED = "root_dialog_dismissed"
    const val PREF_CRASH_LOG_ENABLED = "crash_log_enabled"
    const val PREF_CRASH_LOG_BACKEND_PROVIDER = "crash_log_backend_provider"
    const val PREF_CRASH_LOG_BACKEND_DIRECT_FS = "crash_log_backend_direct_fs"
    const val PREF_CRASH_LOG_BACKEND_RELAY = "crash_log_backend_relay"
    const val PREF_CRASH_LOG_BACKEND_ROOT_SU = "crash_log_backend_root_su"
    const val PREF_CRASH_LOG_BACKEND_ROOT_FS = "crash_log_backend_root_fs"
    const val PREF_CRASH_LOG_BACKEND_RELAY_MERGE = "crash_log_backend_relay_merge"
    const val PREF_CRASH_LOG_MAX_ENTRIES = "crash_log_max_entries"
    const val PREF_CRASH_LOG_MAX_BYTES = "crash_log_max_bytes"
    const val ITSELF = PACKAGE_NAME
}
