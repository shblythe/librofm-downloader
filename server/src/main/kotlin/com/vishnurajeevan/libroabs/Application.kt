package com.vishnurajeevan.libroabs

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.vishnurajeevan.libroabs.libro.LibraryMetadata
import com.vishnurajeevan.libroabs.libro.LibroApiHandler
import io.github.kevincianfarini.cardiologist.intervalPulse
import io.ktor.client.HttpClient
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.days
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

  private val dataDir by option("--data-dir")
    .default("/data")

  private val mediaDir by option("--media-dir")
    .default("/media")

  private val syncInterval by option("--sync-interval", envvar = "SYNC_INTERVAL")
    .choice("h", "d", "w")
    .default("d")

  private val dryRun by option("--dry-run", "-n", envvar = "DRY_RUN")
    .flag(default = false)

  private val renameChapters by option("--rename-chapters", envvar = "RENAME_CHAPTERS")
    .flag(default = false)

  private val writeTitleTag by option("--write-title-tag", envvar = "WRITE_TITLE_TAG")
    .flag(default = false)

  private val verbose by option("--verbose", "-v", envvar = "VERBOSE")
    .flag(default = false)

  // Limits the number of books pulled down to 1
  private val devMode by option("--dev-mode", "-d", envvar = "DEV_MODE")
    .flag(default = false)

  private val libroFmUsername by option("--libro-fm-username", envvar = "LIBRO_FM_USERNAME")
    .required()

  private val libroFmPassword by option("--libro-fm-password", envvar = "LIBRO_FM_PASSWORD")
    .required()

  private val lfdLogger: (String) -> Unit = {
    if (verbose) {
      println(it)
    }
  }

  private val libroFmApi by lazy {
    LibroApiHandler(
      client = HttpClient { },
      dataDir = dataDir,
      dryRun = dryRun,
      verbose = verbose,
      lfdLogger = lfdLogger
    )
  }

  private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        lfdLogger("Token file not found, logging in")
        libroFmApi.fetchLoginData(libroFmUsername, libroFmPassword)
      }

      libroFmApi.fetchLibrary()
      processLibrary()

      launch {
        lfdLogger("Sync Interval: $syncInterval")
        val syncIntervalTimeUnit = when (syncInterval) {
          "h" -> 1.hours
          "d" -> 1.days
          "w" -> 7.days
          else -> error("Unhandled sync interval")
        }

        Clock.System.intervalPulse(syncIntervalTimeUnit)
          .beat { scheduled, occurred ->
            lfdLogger("Checking library on pulse!")
            libroFmApi.fetchLibrary()
            processLibrary()
          }
      }

      serverScope.launch {
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

    localLibrary.audiobooks
      .let {
        if (devMode) {
          it.take(1)
        }
        else {
          it
        }
      }
      .forEach { book ->
        val targetDir = File("$mediaDir/${book.authors.first()}/${book.title}")

        if (!targetDir.exists()) {
          lfdLogger("downloading ${book.title}")
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
              targetDirectory = targetDir,
              writeTitleTag = writeTitleTag
            )
          }
        }
        else {
          lfdLogger("skipping ${book.title} as it exists on the filesystem!")
        }
      }
  }
}

