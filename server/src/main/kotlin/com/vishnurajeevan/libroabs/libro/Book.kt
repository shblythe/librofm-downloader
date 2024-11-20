package com.vishnurajeevan.libroabs.libro

import kotlinx.serialization.Serializable

@Serializable
data class Book(
  val title: String,
  val authors: List<String>,
  val isbn: String
)