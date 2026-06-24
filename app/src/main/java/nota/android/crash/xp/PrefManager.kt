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
    const val PREF_CRASH_LOG_BACKEND_LOCAL_CACHE = "crash_log_backend_local_cache"
    const val PREF_DISTRIBUTED_CACHE_MIGRATED = "distributed_cache_migrated"
    const val PREF_CRASH_LOG_MAX_ENTRIES = "crash_log_max_entries"
    const val PREF_CRASH_LOG_MAX_BYTES = "crash_log_max_bytes"
    const val PREF_LOGCAT_CRASH_FILTER_DEFAULT = "logcat_crash_filter_default"
    const val ITSELF = PACKAGE_NAME
}
