package nota.android.crash.xp.app.common

import android.util.Log

inline fun safeLog(tag: String, message: String, e: Throwable) {
    try { Log.w(tag, message, e) } catch (_: Throwable) {}
}
