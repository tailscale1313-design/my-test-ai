package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NetworkResult
import com.example.data.model.PresetEndpoint
import com.example.data.network.HttpManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val hostUrl: String = "http://192.168.1.1:8000",
    val endpointPath: String = "/api/status",
    val httpMethod: String = "POST",
    val requestBody: String = "{\n  \"query\": \"status_check\",\n  \"client\": \"Android\"\n}",
    val isLoading: Boolean = false,
    val lastResult: NetworkResult? = null,
    val history: List<NetworkResult> = emptyList()
)

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : UiEvent
}

class NetworkViewModel(
    private val httpManager: HttpManager = HttpManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val presets = listOf(
        PresetEndpoint(
            label = "Status",
            path = "/api/status",
            method = "GET",
            sampleBody = ""
        ),
        PresetEndpoint(
            label = "Query",
            path = "/api/query",
            method = "POST",
            sampleBody = "{\n  \"query\": \"ping_node\",\n  \"timestamp\": ${System.currentTimeMillis()}\n}"
        ),
        PresetEndpoint(
            label = "Health Check",
            path = "/health",
            method = "GET",
            sampleBody = ""
        ),
        PresetEndpoint(
            label = "Gateway Root",
            path = "/",
            method = "GET",
            sampleBody = ""
        )
    )

    fun onHostUrlChanged(newHost: String) {
        _uiState.update { it.copy(hostUrl = newHost) }
    }

    fun onEndpointPathChanged(newPath: String) {
        _uiState.update { it.copy(endpointPath = newPath) }
    }

    fun onHttpMethodChanged(newMethod: String) {
        _uiState.update { it.copy(httpMethod = newMethod) }
    }

    fun onRequestBodyChanged(newBody: String) {
        _uiState.update { it.copy(requestBody = newBody) }
    }

    fun applyPreset(preset: PresetEndpoint) {
        _uiState.update {
            it.copy(
                endpointPath = preset.path,
                httpMethod = preset.method,
                requestBody = if (preset.sampleBody.isNotEmpty()) preset.sampleBody else it.requestBody
            )
        }
    }

    fun sendRequest() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = httpManager.executeRequest(
                baseUrl = currentState.hostUrl,
                path = currentState.endpointPath,
                method = currentState.httpMethod,
                bodyContent = if (currentState.httpMethod == "POST") currentState.requestBody else null
            )

            handleResult(result)
        }
    }

    fun testSimulate503() {
        val currentState = _uiState.value
        val result = httpManager.generateSimulated503(
            baseUrl = currentState.hostUrl,
            path = currentState.endpointPath,
            method = currentState.httpMethod
        )
        handleResult(result)
    }

    fun testSimulateSuccess() {
        val currentState = _uiState.value
        val result = httpManager.generateSimulatedSuccess(
            baseUrl = currentState.hostUrl,
            path = currentState.endpointPath,
            method = currentState.httpMethod
        )
        handleResult(result)
    }

    private fun handleResult(result: NetworkResult) {
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                lastResult = result,
                history = listOf(result) + current.history.take(19)
            )
        }

        viewModelScope.launch {
            when {
                result.is503Error -> {
                    _events.emit(UiEvent.ShowToast("⚠️ HTTP 503: Service Unavailable on ${result.requestUrl}"))
                    _events.emit(UiEvent.ShowSnackbar("Server returned 503 Service Unavailable", "Retry"))
                }
                !result.isSuccess -> {
                    val statusText = result.statusCode?.let { "HTTP $it error" } ?: result.statusMessage
                    _events.emit(UiEvent.ShowToast("❌ Request Failed: $statusText"))
                    _events.emit(UiEvent.ShowSnackbar("HTTP Request Failed: $statusText", "Details"))
                }
                else -> {
                    _events.emit(UiEvent.ShowToast("✅ Success: HTTP 200 OK (${result.latencyMs}ms)"))
                    _events.emit(UiEvent.ShowSnackbar("Response received successfully (${result.latencyMs}ms)"))
                }
            }
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }
}
