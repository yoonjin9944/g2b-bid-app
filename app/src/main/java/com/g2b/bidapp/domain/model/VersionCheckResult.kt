package com.g2b.bidapp.domain.model

sealed interface VersionCheckResult {
    data class ForceUpdate(val downloadUrl: String, val releaseNotes: String) : VersionCheckResult
    data class RecommendUpdate(val downloadUrl: String, val releaseNotes: String) : VersionCheckResult
    data object UpToDate : VersionCheckResult
    data class Error(val message: String) : VersionCheckResult
}