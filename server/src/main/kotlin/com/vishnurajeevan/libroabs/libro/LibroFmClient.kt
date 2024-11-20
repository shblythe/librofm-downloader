package com.vishnurajeevan.libroabs.libro

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.outputStream

class LibroApiHandler(
  private val client: HttpClient,
  private val dataDir: String,
  private val dryRun: Boolean
) {
  private val ktorfit = Ktorfit.Builder()
    .baseUrl("https://libro.fm/")
    .httpClient(client.config {
      defaultRequest {
        contentType(ContentType.Application.Json)
      }
      install(ContentNegotiation) {
        json(Json { isLenient = true; ignoreUnknownKeys = true })
      }
    })
    .build()

  private val libroAPI = ktorfit.createLibroAPI()
  private val authToken by lazy {
    File("$dataDir/token.txt").useLines { it.first() }
  }
  val downloadedIsbnsFile by lazy { File("$dataDir/downloaded.json") }
  val downloadedIsbns get() =
    if (downloadedIsbnsFile.exists()) {
      Json.decodeFromString<DownloadedIsbns>(downloadedIsbnsFile.readText())
    } else {
      DownloadedIsbns(emptyList())
    }

  suspend fun fetchLoginData(username: String, password: String) {
    val tokenData = libroAPI.fetchLoginData(
      LoginRequest(username = username, password = password)
    )
    if (tokenData.access_token != null) {
      File("$dataDir/token.txt").printWriter().use {
        it.write(tokenData.access_token!!)
      }
    }
    println(tokenData)
  }

  suspend fun fetchLibrary(page: Int = 1) {
    val library = libroAPI.fetchLibrary("Bearer $authToken", page)
    if (library.audiobooks.isNotEmpty()) {
      val text = Json.encodeToString(library)
      println(text)
      File("$dataDir/library.json").writeText(text)
    }
  }

  suspend fun fetchDownloadMetadata(isbn: String): DownloadMetadata {
    return libroAPI.fetchDownloadMetadata("Bearer $authToken", isbn)
  }

  suspend fun fetchAudioBook(isbn: String, data: List<DownloadPart>, targetDirectory: File) {
    if (downloadedIsbns.isbns.contains(isbn)) {
      return
    }
    client.use { httpClient ->
      data.forEachIndexed { index, part ->
        if (!dryRun) {
          val url = part.url
          val destinationFile = File(targetDirectory, "part-$index.zip")
          val response = httpClient.get(url)

          val input = response.body<ByteReadChannel>()
          FileOutputStream(destinationFile).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

            while (true) {
              val bytesRead = input.readAvailable(buffer)
              if (bytesRead == -1) break

              output.write(buffer, 0, bytesRead)
            }
            output.flush()
          }

          ZipInputStream(destinationFile.inputStream()).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
              val entryPath = targetDirectory.toPath() / entry.name

              if (entry.isDirectory) {
                // Create directory
                entryPath.createDirectories()
              } else {
                // Ensure parent directory exists
                entryPath.parent?.createDirectories()

                // Extract file
                entryPath.outputStream().use { output ->
                  zipIn.copyTo(output)
                }
              }

              // Move to next entry
              entry = zipIn.nextEntry
            }
          }
          destinationFile.delete()
        }
        markIsbnAsDownloaded(isbn)
      }
    }
  }

  fun markIsbnAsDownloaded(isbn: String) {
    println("marking $isbn as downloaded")
    val newIsbns = downloadedIsbns.isbns.toMutableList().apply {
      add(isbn)
    }
    val newDownloadedIsbns = downloadedIsbns.copy(
      isbns = newIsbns
    )
    downloadedIsbnsFile.writeText(Json.encodeToString(newDownloadedIsbns))
  }
}