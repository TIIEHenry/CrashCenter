package nota.android.crash.xp.app.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface LoadableState {
    val isLoading: Boolean
    val errorMessage: String?
    fun copyWithLoading(isLoading: Boolean): LoadableState
    fun copyWithError(errorMessage: String?): LoadableState
}

interface HasErrorMessage {
    val errorMessage: String?
    fun withNoErrorMessage(): HasErrorMessage
}

abstract class BaseFlowViewModel<S>(initialState: S) : ViewModel() {

    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState

    protected fun emitState(transform: S.() -> S) {
        _uiState.value = _uiState.value.transform()
    }

    @Suppress("UNCHECKED_CAST")
    protected fun loadWithState(
        scope: CoroutineScope,
        block: suspend () -> Unit,
    ) {
        val state = _uiState.value as LoadableState
        _uiState.value = state.copyWithLoading(true) as S
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                safeLog("BaseFlowViewModel", "load failed", e)
                val current = _uiState.value as LoadableState
                _uiState.value = current.copyWithError(e.message) as S
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun clearError() {
        val current = _uiState.value
        if (current is HasErrorMessage) {
            _uiState.value = current.withNoErrorMessage() as S
        }
    }

    protected fun launchWithErrorHandling(
        scope: CoroutineScope,
        onError: (Throwable) -> Unit,
        block: suspend () -> Unit,
    ) {
        scope.launch {
            try {
                block()
            } catch (e: Throwable) {
                safeLog("BaseFlowViewModel", "Operation failed", e)
                onError(e)
            }
        }
    }
}
