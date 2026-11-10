package com.mobilitytracker.data.repository

import com.mobilitytracker.data.local.SettingsDao
import com.mobilitytracker.data.model.MORNING_PLAYLIST
import com.mobilitytracker.data.model.MORNING_WARMUP_FOLDER
import com.mobilitytracker.data.model.MORNING_WARMUP_HISTORY_LIMIT
import com.mobilitytracker.data.model.SettingsKeys
import com.mobilitytracker.data.model.UserSettings
import com.mobilitytracker.data.video.VideoResolver
import com.mobilitytracker.domain.model.MorningWarmup
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MorningWarmupRepository @Inject constructor(
    private val settingsDao: SettingsDao,
    private val videoResolver: VideoResolver
) {
    suspend fun getToday(): MorningWarmup? = pick(forceNew = false)

    /** Losuje inny film niż dzisiejszy (poprzedni trafia do historii, więc nie wróci od razu). */
    suspend fun reroll(): MorningWarmup? = pick(forceNew = true)

    private suspend fun pick(forceNew: Boolean): MorningWarmup? {
        // Lokalne pliki mają pierwszeństwo; bez nich losujemy z playlisty YouTube.
        val candidates = videoResolver.findVideosInFolder(MORNING_WARMUP_FOLDER)
            .distinctBy { it.fileName }
            .map { MorningWarmup(it.fileName, it.fileName, it.uri.toString(), isLocal = true) }
            .ifEmpty {
                MORNING_PLAYLIST.map {
                    MorningWarmup(it.id, it.title, "https://www.youtube.com/watch?v=${it.id}", isLocal = false)
                }
            }
        if (candidates.isEmpty()) return null

        val today = LocalDate.now().toString()
        if (!forceNew && settingsDao.getValue(SettingsKeys.MORNING_WARMUP_DATE) == today) {
            val savedId = settingsDao.getValue(SettingsKeys.MORNING_WARMUP_FILE)
            candidates.firstOrNull { it.id == savedId }?.let { return it }
        }

        val recent = settingsDao.getValue(SettingsKeys.MORNING_WARMUP_HISTORY)
            ?.lineSequence()
            ?.filter { it.isNotBlank() }
            ?.toList()
            .orEmpty()
        val selectedId = chooseMorningWarmupFile(candidates.map { it.id }, recent) ?: return null
        val selected = candidates.first { it.id == selectedId }

        settingsDao.upsert(UserSettings(SettingsKeys.MORNING_WARMUP_DATE, today))
        settingsDao.upsert(UserSettings(SettingsKeys.MORNING_WARMUP_FILE, selected.id))
        settingsDao.upsert(
            UserSettings(
                SettingsKeys.MORNING_WARMUP_HISTORY,
                (listOf(selected.id) + recent.filterNot { it == selected.id })
                    .take(MORNING_WARMUP_HISTORY_LIMIT)
                    .joinToString("\n")
            )
        )
        return selected
    }
}

/** Losuje spośród filmów spoza ostatnio użytych; gdy wszystkie były ostatnio, losuje z całej puli. */
internal fun chooseMorningWarmupFile(
    fileNames: List<String>,
    recentFiles: List<String>,
    random: Random = Random.Default
): String? {
    val available = fileNames.distinct()
    if (available.isEmpty()) return null

    val recentAvailable = recentFiles
        .filter { it in available }
        .take((available.size - 1).coerceAtLeast(0))
        .toSet()

    return available.filter { it !in recentAvailable }.random(random)
}
