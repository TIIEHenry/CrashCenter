package nota.android.crash.xp.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import nota.android.crash.common.data.CrashEvent

class FakeCrashLogRepository : CrashLogRepository {

    private val _events = MutableStateFlow<List<CrashEvent>>(emptyList())
    var throwOnGetAll: Boolean = false
    var throwOnGetById: Boolean = false
    var throwOnGetCount: Boolean = false

    var events: List<CrashEvent>
        get() = _events.value
        set(value) {
            _events.value = value.toList()
        }

    override fun getAll(filter: CrashFilter, limit: Int, offset: Int): List<CrashEvent> {
        if (throwOnGetAll) throw RuntimeException("Simulated error")
        return events.drop(offset).take(limit)
    }

    override fun getById(id: String): CrashEvent? {
        if (throwOnGetById) throw RuntimeException("Simulated getById error")
        return events.firstOrNull { it.id == id }
    }

    override fun getCount(filter: CrashFilter): Int {
        if (throwOnGetCount) throw RuntimeException("Simulated getCount error")
        return events.size
    }

    fun addEvent(event: CrashEvent) {
        _events.update { it + event }
    }

    override fun deleteById(id: String): Boolean {
        val original = events.size
        _events.update { list -> list.filter { it.id != id } }
        return events.size < original
    }

    override fun clear() {
        _events.value = emptyList()
        throwOnGetAll = false
        throwOnGetById = false
        throwOnGetCount = false
    }

    override fun applyRetention() {
        // No-op for fake; tests can trim events manually if needed
    }
}
