package nota.android.crash

import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XposedHelpers

/**
 * Hook-side crash interceptor (ADR-011).
 *
 * Replaces the main Looper.loop() with a guarded infinite loop so that
 * RuntimeExceptions thrown on the main thread do not kill the process.
 * Also intercepts uncaught exceptions on all threads.
 */
object CrashHandler {

    const val SOURCE_LOOPER = "looper"
    const val SOURCE_UNCAUGHT = "uncaught"

    fun interface ExceptionHandler {
        fun handleException(throwable: Throwable, source: String)
    }

    @Volatile
    private var installed = false

    @Volatile
    private var exceptionHandler: ExceptionHandler? = null

    private var uncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    @JvmStatic
    @Synchronized
    fun insert(handler: ExceptionHandler) {
        if (installed) return
        installed = true
        exceptionHandler = handler

        val targetLooper = XposedHelpers.callStaticMethod(Looper::class.java, "getMainLooper") as Looper
        fun loopOnce() {
            try {
                XposedHelpers.callStaticMethod(Looper::class.java, "loop")
            } catch (e: Throwable) {
                exceptionHandler?.handleException(e, SOURCE_LOOPER)
                Handler(targetLooper).post(::loopOnce)
            }
        }
        Handler(targetLooper).post(::loopOnce)

        uncaughtExceptionHandler = XposedHelpers.callStaticMethod(
            Thread::class.java,
            "getDefaultUncaughtExceptionHandler"
        ) as Thread.UncaughtExceptionHandler?

        XposedHelpers.callStaticMethod(
            Thread::class.java,
            "setDefaultUncaughtExceptionHandler",
            Thread.UncaughtExceptionHandler { _, e ->
                exceptionHandler?.handleException(e, SOURCE_UNCAUGHT)
            }
        )
    }
}
