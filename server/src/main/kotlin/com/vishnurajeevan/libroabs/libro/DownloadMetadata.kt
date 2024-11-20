package com.vishnurajeevan.libroabs.libro

import kotlinx.serialization.Serializable

@Serializable
data class DownloadMetadata(
    val parts: List<DownloadPart>
)

@Serializable
data class DownloadPart(
    val url: String,
    val size_bytes: Long
)