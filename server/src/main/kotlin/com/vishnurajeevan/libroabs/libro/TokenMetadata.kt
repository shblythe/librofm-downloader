package com.vishnurajeevan.libroabs.libro

import kotlinx.serialization.Serializable

@Serializable
data class TokenMetadata(
  val access_token: String? = null,
)