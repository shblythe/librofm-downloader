package com.vishnurajeevan.libroabs

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  val events = Channel<AppEvents>()
  val presenter = AppPresenter()
  ComposeViewport(document.body!!) {
    App(
      model = presenter.present(
        AppModel(),
        events.receiveAsFlow()
      ),
      dispatch = events::trySend
    )
  }
}