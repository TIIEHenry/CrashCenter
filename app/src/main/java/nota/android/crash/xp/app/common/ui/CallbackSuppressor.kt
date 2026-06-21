package nota.android.crash.xp.app.common.ui

class CallbackSuppressor {
    var suppressed = false

    inline fun run(block: () -> Unit) {
        suppressed = true
        try {
            block()
        } finally {
            suppressed = false
        }
    }
}
