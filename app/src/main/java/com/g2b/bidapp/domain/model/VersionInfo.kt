package com.g2b.bidapp.domain.model

data class VersionInfo(
    val latestVersion: String,
    val minRequiredVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)