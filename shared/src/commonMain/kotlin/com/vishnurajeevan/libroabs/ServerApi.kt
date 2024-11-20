package com.vishnurajeevan.libroabs

import de.jensklingenberg.ktorfit.Call
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST

interface ServerApi {
  @POST("/login")
  suspend fun login(@Body loginData: ApiLoginData): Call<Unit>

}