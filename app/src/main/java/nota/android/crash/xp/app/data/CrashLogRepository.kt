package nota.android.crash.xp.app.data

import nota.android.crash.common.data.CrashEvent

interface CrashLogRepository {
    fun getAll(filter: CrashFilter = CrashFilter(), limit: Int, offset: Int): List<CrashEvent>
    fun getById(id: String): CrashEvent?
    fun getCount(filter: CrashFilter = CrashFilter()): Int
    fun getPackageCounts(filter: CrashFilter = CrashFilter()): List<Pair<String, Int>>
    fun deleteById(id: String): Boolean
    fun deleteByPackage(packageName: String): Int
    fun clear()
    fun applyRetention()
}
