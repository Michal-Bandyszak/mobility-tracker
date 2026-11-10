package com.mobilitytracker.data.video

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

data class VideoFile(
    val fileName: String,
    val uri: Uri
)

@Singleton
class VideoResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var cachedVideos: List<IndexedVideo>? = null

    fun findVideoUri(fileName: String): Uri? {
        val videos = getOrLoadAllVideos()

        // 1. Exact match
        videos.find { it.fileName == fileName }
            ?.let { return buildUri(it.id) }

        // 2. Case-insensitive match
        videos.find { it.fileName.equals(fileName, ignoreCase = true) }
            ?.let { return buildUri(it.id) }

        // 3. Normalized match — usuwa różnice w polskich znakach
        val normalizedTarget = normalize(fileName)
        videos.find { normalize(it.fileName) == normalizedTarget }
            ?.let { return buildUri(it.id) }

        // 4. Normalized + case-insensitive
        videos.find { normalize(it.fileName).equals(normalizedTarget, ignoreCase = true) }
            ?.let { return buildUri(it.id) }

        return null
    }

    fun findVideosInFolder(folder: String): List<VideoFile> {
        val normalizedFolder = folder.replace('\\', '/').trim('/').trimEnd('/')
        return getOrLoadAllVideos()
            .asSequence()
            .filter { isInFolder(it.folderPath, normalizedFolder) }
            .map { VideoFile(it.fileName, buildUri(it.id)) }
            .toList()
    }

    fun refreshCache() {
        cachedVideos = null
    }

    @Synchronized
    private fun getOrLoadAllVideos(): List<IndexedVideo> {
        cachedVideos?.let { return it }

        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            MediaStore.MediaColumns.DATA
        }
        val result = mutableListOf<IndexedVideo>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            pathColumn
        )

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(pathColumn)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: continue
                val id = cursor.getLong(idCol)
                result.add(IndexedVideo(name, id, cursor.getString(pathCol)))
            }
        }

        return result.also { cachedVideos = it }
    }

    private fun isInFolder(path: String?, normalizedFolder: String): Boolean {
        val normalizedPath = path?.replace('\\', '/')?.trim('/') ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return normalizedPath.equals(normalizedFolder, ignoreCase = true)
        }

        return normalizedPath.substringBeforeLast('/').endsWith(normalizedFolder, ignoreCase = true)
    }

    private data class IndexedVideo(
        val fileName: String,
        val id: Long,
        val folderPath: String?
    )

    private fun buildUri(id: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")
            .replace("  ", " ")
            .trim()
}
