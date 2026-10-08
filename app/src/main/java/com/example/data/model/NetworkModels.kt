package com.example.data.model

data class NetworkResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isSuccess: Boolean,
    val statusCode: Int?,
    val statusMessage: String,
    val responseBody: String?,
    val requestUrl: String,
    val requestMethod: String,
    val latencyMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val is503Error: Boolean = (statusCode == 503)
)

data class PresetEndpoint(
    val label: String,
    val path: String,
    val method: String,
    val sampleBody: String
)
