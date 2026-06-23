package nota.android.crash.xp.app.observe

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nota.android.crash.analysis.RuleEngine
import nota.android.crash.xp.app.common.BaseFlowViewModel
import nota.android.crash.xp.app.data.CrashLogRepository
import nota.android.crash.xp.app.data.StatsAggregator

class CrashStatsViewModel(
    repository: CrashLogRepository,
    ruleEngine: RuleEngine? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseFlowViewModel<CrashStatsUiState>(CrashStatsUiState()) {

    private val aggregator = StatsAggregator(repository, ruleEngine)

    fun loadStats() {
        loadWithState(viewModelScope) {
            val stats = withContext(ioDispatcher) { aggregator.computeStats() }
            emitState { copy(isLoading = false, stats = stats) }
        }
    }
}
