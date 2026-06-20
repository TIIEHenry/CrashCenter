package nota.android.crash.xp.app.data

class FakeCrashLogRepository : CrashLogRepository {

    var events: List<CrashEvent> = emptyList()
    var throwOnGetAll: Boolean = false

    override fun getAll(filter: CrashFilter, limit: Int, offset: Int): List<CrashEvent> {
        if (throwOnGetAll) throw RuntimeException("Simulated error")
        return events.drop(offset).take(limit)
    }

    override fun getById(id: String): CrashEvent? =
        events.firstOrNull { it.id == id }

    override fun getCount(filter: CrashFilter): Int =
        events.size

    fun addEvent(event: CrashEvent) {
        events = events + event
    }

    override fun deleteById(id: String): Boolean {
        val original = events.size
        events = events.filter { it.id != id }
        return events.size < original
    }

    override fun clear() {
        events = emptyList()
        throwOnGetAll = false
    }
}
