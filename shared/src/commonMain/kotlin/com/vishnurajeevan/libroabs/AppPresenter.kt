package com.vishnurajeevan.libroabs

import androidx.compose.runtime.*
import de.jensklingenberg.ktorfit.Callback
import de.jensklingenberg.ktorfit.ktorfitBuilder
import kotlinx.coroutines.flow.Flow

class AppPresenter {
  private val client = ktorfitBuilder {
    baseUrl("localhost")
  }
    .build()
    .createServerApi()

  @Composable
  fun present(lastModel: AppModel, events: Flow<AppEvents>): AppModel {
    var model by remember { mutableStateOf(lastModel) }

    LaunchedEffect(Unit) {
      events.collect { event ->
        when (event) {
          is AppEvents.Login -> {
            client.login(ApiLoginData(event.username, event.password))
              .onExecute(object : Callback<Unit> {
                override fun onError(exception: Throwable) {
                  model = model.copy(
                    isLoggedIn = false
                  )
                }

                override fun onResponse(call: Unit, response: io.ktor.client.statement.HttpResponse) {
                  model = model.copy(
                    isLoggedIn = true
                  )
                }
              })
          }
        }
      }
    }
    return model
  }
}

data class AppModel(
  val isLoggedIn: Boolean = false
)

sealed interface AppEvents {
  data class Login(val username: String, val password: String) : AppEvents
}