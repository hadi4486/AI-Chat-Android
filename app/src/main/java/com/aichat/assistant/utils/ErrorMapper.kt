package com.aichat.assistant.utils

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aichat.assistant.BuildConfig
import com.aichat.assistant.R
import com.aichat.assistant.domain.model.AppError

/**
 * The single place that turns an [AppError] into text a user can read (master spec §10: never
 * show a raw exception). [AppError.technicalDetail] is deliberately never used here — only
 * [logIfDebug] touches it, and only behind [BuildConfig.DEBUG].
 */
@StringRes
fun AppError.toStringRes(): Int = when (this) {
    is AppError.InvalidApiKey -> R.string.error_invalid_api_key
    is AppError.Unauthorized -> R.string.error_unauthorized
    is AppError.Forbidden -> R.string.error_forbidden
    is AppError.BadRequest -> R.string.error_bad_request
    is AppError.RateLimited -> R.string.error_rate_limit
    is AppError.Timeout -> R.string.error_timeout
    is AppError.NoInternet -> R.string.error_no_internet
    is AppError.ServerError -> R.string.error_server
    is AppError.InvalidBaseUrl -> R.string.error_invalid_base_url
    is AppError.EmptyResponse -> R.string.error_empty_response
    is AppError.UnsupportedModel -> R.string.error_unsupported_model
    is AppError.NotConfigured -> R.string.error_not_configured
    is AppError.Unknown -> R.string.error_unknown
}

@Composable
fun AppError.localizedMessage(): String = stringResource(toStringRes())

/**
 * Debug-only diagnostic log. Only ever writes [AppError.technicalDetail], which is populated from
 * HTTP status/response bodies or exception messages — never from request headers — so the API key
 * cannot end up here even in a debug build. A no-op in release builds.
 */
fun AppError.logIfDebug(tag: String) {
    if (BuildConfig.DEBUG) {
        Log.d(tag, "${this::class.simpleName}: ${technicalDetail.orEmpty()}")
    }
}
