package nota.android.crash.xp.app.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseFlowViewModel<S>(initialState: S) : ViewModel() {

    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState

    protected fun emitState(transform: S.() -> S) {
        _uiState.value = _uiState.value.transform()
    }
}
