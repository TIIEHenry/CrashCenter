package nota.android.crash.xp.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeCrashLogRepository : CrashLogRepository {

    var events: List<CrashEvent> = emptyList()
    var throwOnGetAll: Boolean = false
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

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
        _changes.tryEmit(Unit)
    }

    override fun deleteById(id: String): Boolean {
        val original = events.size
        events = events.filter { it.id != id }
        val changed = events.size < original
        if (changed) _changes.tryEmit(Unit)
        return changed
    }

    override fun clear() {
        val changed = events.isNotEmpty()
        events = emptyList()
        throwOnGetAll = false
        if (changed) _changes.tryEmit(Unit)
    }

    override fun observeChanges(): Flow<Unit> = _changes.asSharedFlow()

    override fun applyRetention() {
        // No-op for fake; tests can trim events manually if needed
    }
}
