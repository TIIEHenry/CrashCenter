package nota.android.crash.log

import de.robv.android.xposed.XposedBridge

/**
 * Hook-side safe logging utilities.
 *
 * Runs in the target app's process where [android.util.Log] might not be
 * available; delegates to [XposedBridge.log] wrapped in a try-catch so that
 * logging failures never crash the host app.
 */
internal inline fun hookSafeLog(message: String) {
    try { XposedBridge.log(message) } catch (_: Throwable) {}
}

internal inline fun hookSafeLog(tag: String, message: String) {
    hookSafeLog("[$tag] $message")
}

internal inline fun hookSafeLog(tag: String, message: String, e: Throwable) {
    hookSafeLog("[$tag] $message: ${e.message}")
}
