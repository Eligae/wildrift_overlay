package com.eligae.wildrift.overlay.api

data class GitHubRelease(
    val tag_name: String,
    val name: String?,
    val html_url: String,
)
