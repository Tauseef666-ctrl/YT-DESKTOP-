/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.errors

/**
 * Error taxonomy (plan.md 1.8). Every failure that reaches the UI is tagged
 * with the user action that triggered it and a stable, user-safe message.
 */
enum class UserAction {
    SEARCH,
    SUGGESTIONS,
    TRENDING,
    CHANNEL,
    CHANNEL_TAB,
    STREAM_RESOLUTION,
    VIDEO_PAGE,
    PLAYBACK,
    UNKNOWN,
}

/** Immutable, serialisable description of a failure, safe for the UI layer. */
data class ErrorInfo(
    val userAction: UserAction,
    val message: String,
    val causeClass: String? = null,
)

/** Exception thrown by the core service layer; [ErrorInfo] is the UI payload. */
class YtException(
    val userAction: UserAction,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    val errorInfo: ErrorInfo
        get() = ErrorInfo(userAction, message ?: "Unexpected error", cause?.javaClass?.name)
}