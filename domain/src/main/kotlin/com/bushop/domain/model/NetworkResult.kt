package com.bushop.domain.model

/** Standardized network result type for the data layer. */
sealed class NetworkResult<out T> {
    data class Success<T>(
        val data: T,
    ) : NetworkResult<T>()

    data class Error(
        val message: String,
        val exception: Throwable? = null,
    ) : NetworkResult<Nothing>()
}

/** Convert a [Result] to [NetworkResult], preserving error messages. */
fun <T> Result<T>.toNetworkResult(label: String = ""): NetworkResult<T> =
    fold(
        onSuccess = { NetworkResult.Success(it) },
        onFailure = { e ->
            val prefix = if (label.isNotBlank()) "$label: " else ""
            NetworkResult.Error("${prefix}${e.message ?: "Unknown error"}", e)
        },
    )
