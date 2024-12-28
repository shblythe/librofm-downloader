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
import io.github.kevincianfarini.cardiologist.intervalPulse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.hours

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

  private val renameChapters by option("--rename-chapters")
    .flag(default = false)

  private val libroFmUsername by option("--libro-fm-username").required()
  private val libroFmPassword by option("--libro-fm-password").required()

  private val libroFmApi by lazy {
    LibroApiHandler(
      client = HttpClient {
        install(Logging) {
          logger = Logger.DEFAULT
          level = if (verbose) LogLevel.BODY else LogLevel.INFO
        }
      },
      dataDir = dataDir,
      dryRun = dryRun,
      logger = logger
    )
  }

  private val logger: (String) -> Unit = {
    if (verbose) {
      println(it)
    }
  }

  override fun run() {
    if (dryRun) {
      println("This is a dry run!")
    }
    runBlocking {
      val dataDir = File(dataDir).apply {
        if (!exists()) {
          mkdirs()
        }
      }
      val tokenFile = File("$dataDir/token.txt")
      if (!tokenFile.exists()) {
        logger("Token file not found, logging in")
        libroFmApi.fetchLoginData(libroFmUsername, libroFmPassword)
      }

      libroFmApi.fetchLibrary()
      processLibrary()

      launch {
        Clock.System.intervalPulse(1.hours).beat { scheduled, occurred ->
          logger("Checking library on pulse!")
          libroFmApi.fetchLibrary()
          processLibrary()
        }
      }
      launch {
        embeddedServer(
          factory = Netty,
          port = port,
          host = "0.0.0.0",
          module = {
            routing {
              post("/update") {
                call.respondText("Updating!")
                libroFmApi.fetchLibrary()
                processLibrary()
              }
            }
          }
        ).start(wait = true)
      }
    }
  }

  private suspend fun processLibrary() {
    val localLibrary = Json.decodeFromString<LibraryMetadata>(
      File("$dataDir/library.json").readText()
    )

    localLibrary.audiobooks.forEach { book ->
      val targetDir = File("$mediaDir/${book.authors.first()}/${book.title}")

      if (!targetDir.exists()) {
        logger("downloading ${book.title}")
        targetDir.mkdirs()
        val downloadData = libroFmApi.fetchDownloadMetadata(book.isbn)
        libroFmApi.fetchAudioBook(
          data = downloadData.parts,
          targetDirectory = targetDir
        )

        if (renameChapters) {
          libroFmApi.renameChapters(
            title = book.title,
            tracks = downloadData.tracks,
            targetDirectory = targetDir
          )
        }
      }
      else {
        logger("skipping ${book.title} as it exists on the filesystem!")
      }
    }
  }
}

