package com.vishnurajeevan.libroabs.libro

import de.jensklingenberg.ktorfit.http.*

interface LibroAPI {
  @POST("oauth/token")
  suspend fun fetchLoginData(
    @Body loginRequest: LoginRequest
  ): TokenMetadata

  @GET("api/v10/library")
  suspend fun fetchLibrary(
    @Header("Authorization") authToken: String,
    @Query("page") page: Int = 1
  ): LibraryMetadata

  @GET("api/v10/download-manifest")
  suspend fun fetchDownloadMetadata(
    @Header("Authorization") authToken: String,
    @Query("isbn") isbn: String
  ): DownloadMetadata
}