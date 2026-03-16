package com.mobilitytracker.data.video

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var cachedVideos: List<Pair<String, Long>>? = null

    fun findVideoUri(fileName: String): Uri? {
        val videos = getOrLoadAllVideos()

        // 1. Exact match
        videos.find { it.first == fileName }
            ?.let { return buildUri(it.second) }

        // 2. Case-insensitive match
        videos.find { it.first.equals(fileName, ignoreCase = true) }
            ?.let { return buildUri(it.second) }

        // 3. Normalized match — usuwa różnice w polskich znakach
        val normalizedTarget = normalize(fileName)
        videos.find { normalize(it.first) == normalizedTarget }
            ?.let { return buildUri(it.second) }

        // 4. Normalized + case-insensitive
        videos.find { normalize(it.first).equals(normalizedTarget, ignoreCase = true) }
            ?.let { return buildUri(it.second) }

        return null
    }

    fun refreshCache() {
        cachedVideos = null
    }

    @Synchronized
    private fun getOrLoadAllVideos(): List<Pair<String, Long>> {
        cachedVideos?.let { return it }

        val result = mutableListOf<Pair<String, Long>>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME
        )

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: continue
                val id = cursor.getLong(idCol)
                result.add(name to id)
            }
        }

        return result.also { cachedVideos = it }
    }

    private fun buildUri(id: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")
            .replace("  ", " ")
            .trim()
}
