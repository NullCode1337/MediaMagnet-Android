package dev.nullcode.mediamagnet.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream

class FileSaver(private val context: Context) {
    private val resolver get() = context.contentResolver

    companion object {
        const val DEFAULT_FOLDER = "Downloads/MediaMagnet"

        fun guessMime(name: String): String {
            val ext = name.substringAfterLast('.', "").lowercase()
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                ?: "application/octet-stream"
        }
    }

    suspend fun save(
        relativePath: String,
        mime: String,
        treeUri: String?,
        onCreated: suspend (Uri) -> Unit = {},
        write: suspend (OutputStream) -> Unit,
    ): Uri = withContext(Dispatchers.IO) {
        val segments = relativePath.split('/')
            .map(::sanitize)
            .filter { it.isNotEmpty() && it != "." && it != ".." }
        val fileName = segments.lastOrNull() ?: "download"
        val dirs = segments.dropLast(1)

        if (treeUri != null) {
            saveToTree(Uri.parse(treeUri), dirs, fileName, mime, onCreated, write)
        } else {
            saveToDownloads(dirs, fileName, mime, onCreated, write)
        }
    }

    private suspend fun saveToDownloads(
        dirs: List<String>,
        name: String,
        mime: String,
        onCreated: suspend (Uri) -> Unit,
        write: suspend (OutputStream) -> Unit,
    ): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = (
                listOf(Environment.DIRECTORY_DOWNLOADS, "MediaMagnet") + dirs
                ).joinToString("/")
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values)
                ?: throw IOException("Could not create a file in Downloads")

            try {
                onCreated(uri)
                resolver.openOutputStream(uri)?.use { output -> write(output) }
                    ?: throw IOException("Could not open the new file in Downloads")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return uri
            } catch (error: Throwable) {
                resolver.delete(uri, null, null)
                throw error
            }
        }

        if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw IOException("Storage permission is required on this Android version")
        }

        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(root, (listOf("MediaMagnet") + dirs).joinToString("/")).apply { mkdirs() }
        val file = uniqueFile(dir, name)
        onCreated(Uri.fromFile(file))
        file.outputStream().use { output -> write(output) }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null)
        return Uri.fromFile(file)
    }

    private suspend fun saveToTree(
        tree: Uri,
        dirs: List<String>,
        name: String,
        mime: String,
        onCreated: suspend (Uri) -> Unit,
        write: suspend (OutputStream) -> Unit,
    ): Uri {
        var dir = DocumentFile.fromTreeUri(context, tree)
            ?: throw IOException("The selected download folder is no longer available")

        for (segment in dirs) {
            dir = dir.findFile(segment)?.takeIf { it.isDirectory }
                ?: dir.createDirectory(segment)
                ?: throw IOException("Could not create folder: $segment")
        }

        val targetName = uniqueDocumentName(dir, name)
        val file = dir.createFile(mime, targetName)
            ?: throw IOException("Could not create a file in the selected folder")

        try {
            onCreated(file.uri)
            resolver.openOutputStream(file.uri, "w")?.use { output -> write(output) }
                ?: throw IOException("Could not open the new file in the selected folder")
            return file.uri
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        val candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (true) {
            val file = File(dir, "$base ($index)$ext")
            if (!file.exists()) return file
            index++
        }
    }

    private fun uniqueDocumentName(dir: DocumentFile, name: String): String {
        if (dir.findFile(name) == null) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (true) {
            val candidate = "$base ($index)$ext"
            if (dir.findFile(candidate) == null) return candidate
            index++
        }
    }

    private fun sanitize(segment: String): String =
        segment.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim()
}
