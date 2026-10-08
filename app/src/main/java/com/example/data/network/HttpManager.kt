package com.example.data.network

import com.example.data.model.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class HttpManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    suspend fun executeRequest(
        baseUrl: String,
        path: String,
        method: String,
        bodyContent: String?
    ): NetworkResult = withContext(Dispatchers.IO) {
        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        val fullUrl = "$cleanBaseUrl$cleanPath"

        val startTime = System.currentTimeMillis()

        try {
            val requestBuilder = Request.Builder().url(fullUrl)

            if (method.equals("POST", ignoreCase = true)) {
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = (bodyContent ?: "{}").toRequestBody(mediaType)
                requestBuilder.post(requestBody)
            } else {
                requestBuilder.get()
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                val code = response.code
                val message = response.message
                val bodyString = response.body?.string() ?: ""

                val isSuccess = response.isSuccessful

                NetworkResult(
                    isSuccess = isSuccess,
                    statusCode = code,
                    statusMessage = if (code == 503) "503 Service Unavailable" else "$code $message",
                    responseBody = bodyString,
                    requestUrl = fullUrl,
                    requestMethod = method,
                    latencyMs = elapsed,
                    is503Error = (code == 503)
                )
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            val (statusCode, friendlyMsg) = when (e) {
                is SocketTimeoutException -> Pair(null, "Connection timed out connecting to $cleanBaseUrl")
                is ConnectException -> Pair(null, "Connection refused or unreachable: $cleanBaseUrl")
                is UnknownHostException -> Pair(null, "Host name could not be resolved: $cleanBaseUrl")
                is IOException -> Pair(null, "Network I/O Error: ${e.localizedMessage ?: "Unknown error"}")
                else -> Pair(null, "Request failed: ${e.localizedMessage ?: "Unknown error"}")
            }

            NetworkResult(
                isSuccess = false,
                statusCode = statusCode,
                statusMessage = friendlyMsg,
                responseBody = "Exception: ${e.javaClass.simpleName}\nDetail: ${e.message}",
                requestUrl = fullUrl,
                requestMethod = method,
                latencyMs = elapsed,
                is503Error = false
            )
        }
    }

    fun generateSimulated503(baseUrl: String, path: String, method: String): NetworkResult {
        val fullUrl = "${baseUrl.trim().removeSuffix("/")}/${path.trimStart('/')}"
        return NetworkResult(
            isSuccess = false,
            statusCode = 503,
            statusMessage = "503 Service Unavailable",
            responseBody = """{"error": "Service Unavailable", "code": 503, "detail": "The server at 192.168.1.1 is temporarily overloaded or down for maintenance."}""",
            requestUrl = fullUrl,
            requestMethod = method,
            latencyMs = 42L,
            is503Error = true
        )
    }

    fun generateSimulatedSuccess(baseUrl: String, path: String, method: String): NetworkResult {
        val fullUrl = "${baseUrl.trim().removeSuffix("/")}/${path.trimStart('/')}"
        return NetworkResult(
            isSuccess = true,
            statusCode = 200,
            statusMessage = "200 OK",
            responseBody = """{"status": "online", "host": "192.168.1.1", "uptime": "4d 18h 32m", "services": ["http", "dns", "api"]}""",
            requestUrl = fullUrl,
            requestMethod = method,
            latencyMs = 18L,
            is503Error = false
        )
    }
}
