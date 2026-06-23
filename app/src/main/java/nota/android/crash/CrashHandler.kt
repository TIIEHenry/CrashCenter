package nota.android.crash

import android.os.Handler
import android.os.Looper
import android.os.Process
import de.robv.android.xposed.XposedHelpers

/**
 * Hook-side crash capture (ADR-011, ADR-023).
 *
 * [Mode.INTERCEPT]: guarded infinite Looper loop + non-forwarding UEH (swallow).
 * [Mode.OBSERVE]: one-shot Looper catch + forwarding UEH (log then system exit).
 */
object CrashHandler {

    const val SOURCE_LOOPER = "looper"
    const val SOURCE_UNCAUGHT = "uncaught"

    enum class Mode {
        INTERCEPT,
        OBSERVE,
    }

    fun interface ExceptionHandler {
        fun handleException(throwable: Throwable, source: String)
    }

    @Volatile
    private var installed = false

    @Volatile
    private var exceptionHandler: ExceptionHandler? = null

    @Volatile
    private var previousUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    @JvmStatic
    @Synchronized
    fun install(mode: Mode, handler: ExceptionHandler) {
        if (installed) return
        installed = true
        exceptionHandler = handler

        val targetLooper = XposedHelpers.callStaticMethod(Looper::class.java, "getMainLooper") as Looper
        when (mode) {
            Mode.INTERCEPT -> installInterceptLoop(targetLooper)
            Mode.OBSERVE -> installObserveLoop(targetLooper)
        }
        installUncaughtHandler(mode)
    }

    private fun installInterceptLoop(targetLooper: Looper) {
        fun loopOnce() {
            try {
                XposedHelpers.callStaticMethod(Looper::class.java, "loop")
            } catch (e: Throwable) {
                exceptionHandler?.handleException(e, SOURCE_LOOPER)
                Handler(targetLooper).post(::loopOnce)
            }
        }
        Handler(targetLooper).post(::loopOnce)
    }

    private fun installObserveLoop(targetLooper: Looper) {
        fun loopOnce() {
            try {
                XposedHelpers.callStaticMethod(Looper::class.java, "loop")
            } catch (e: Throwable) {
                exceptionHandler?.handleException(e, SOURCE_LOOPER)
                throw e
            }
        }
        Handler(targetLooper).post(::loopOnce)
    }

    private fun installUncaughtHandler(mode: Mode) {
        previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        val delegate = Thread.UncaughtExceptionHandler { thread, throwable ->
            exceptionHandler?.handleException(throwable, SOURCE_UNCAUGHT)
            when (mode) {
                Mode.INTERCEPT -> Unit
                Mode.OBSERVE -> {
                    val previous = previousUncaughtExceptionHandler
                    if (previous != null) {
                        previous.uncaughtException(thread, throwable)
                    } else {
                        Process.killProcess(Process.myPid())
                    }
                }
            }
        }
        XposedHelpers.callStaticMethod(
            Thread::class.java,
            "setDefaultUncaughtExceptionHandler",
            delegate,
        )
    }
}
