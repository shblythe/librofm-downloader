package com.vishnurajeevan.libroabs.libro

import kotlinx.serialization.Serializable

@Serializable
data class DownloadedIsbns(
  val isbns: List<String>
)
