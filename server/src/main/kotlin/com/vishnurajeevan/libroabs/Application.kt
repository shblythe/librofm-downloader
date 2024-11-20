package com.vishnurajeevan.libroabs

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.vishnurajeevan.libroabs.libro.LibraryMetadata
import com.vishnurajeevan.libroabs.libro.LibroApiHandler
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File

fun main(args: Array<String>) {
  NoOpCliktCommand(name = "librofm-abs")
    .subcommands(Run())
    .main(args)
}

class Run : CliktCommand("run") {
  private val port by option("--port")
    .int()
    .default(8080)

  private val verbose by option("--verbose", "-v")
    .flag(default = false)

  private val dataDir by option("--data-dir")
    .default("/data")

  private val mediaDir by option("--media-dir")
    .default("/media")

  private val dryRun by option("--dry-run", "-n")
    .flag(default = false)

  private val libroFmUsername by option("--libro-fm-username").required()
  private val libroFmPassword by option("--libro-fm-password").required()


  override fun run() {
    if (dryRun) {
      println("This is a dry run!")
    }
    runBlocking {
      val libroFmApi = LibroApiHandler(
        dataDir = dataDir,
        client = HttpClient {
          install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
          }
        },
        dryRun = dryRun
      )
      val tokenFile = File("$dataDir/token.txt")
      if (tokenFile.exists()) {
        libroFmApi.fetchLibrary()
      } else {
        libroFmApi.fetchLoginData(libroFmUsername, libroFmPassword)
        libroFmApi.fetchLibrary()
      }

      val localLibrary = Json.decodeFromString<LibraryMetadata>(
        File("$dataDir/library.json").readText()
      )

      localLibrary.audiobooks.forEach { book ->
        val targetDir = File("$mediaDir/${book.authors.first()}/${book.title}")

        if (!targetDir.exists()) {
          targetDir.mkdirs()
          val downloadData = libroFmApi.fetchDownloadMetadata(book.isbn)
          launch {
            libroFmApi.fetchAudioBook(
              isbn = book.isbn,
              data = downloadData.parts,
              targetDirectory = targetDir
            )
          }
        } else {
          println("skipping ${book.title} as it exists on the filesystem!")
          libroFmApi.markIsbnAsDownloaded(book.isbn)
        }
      }

//    embeddedServer(
//      factory = Netty,
//      port = port,
//      host = "0.0.0.0",
//      module = { module(libroFmApi) }
//    )
//      .start(wait = true)
    }
  }
}

fun Application.module(libroApi: LibroApiHandler) {
  routing {
    get("/") {
      call.respondText("Ktor: ${Greeting().greet()}")
    }

    post("/login") {
      val data = call.receive<ApiLoginData>()
      libroApi.fetchLoginData(data.username, data.password)
    }
  }
}