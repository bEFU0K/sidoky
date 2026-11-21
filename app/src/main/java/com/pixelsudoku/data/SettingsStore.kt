package com.pixelsudoku.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore("settings")

data class Settings(
    val theme: Int = 0, // 0 системная, 1 светлая, 2 тёмная
    val sound: Boolean = true,
    val volume: Float = 0.6f,
    val vibration: Boolean = true,
    val autoCheck: Boolean = true,
    val hlSame: Boolean = true,
    val hlRegion: Boolean = true,
    val showTimer: Boolean = true,
    val losses: Int = 0,
    val streak: Int = 0,
    val maxStreak: Int = 0
)

object Keys {
    val THEME = intPreferencesKey("theme")
    val SOUND = booleanPreferencesKey("sound")
    val VOLUME = floatPreferencesKey("volume")
    val VIBRATION = booleanPreferencesKey("vibration")
    val AUTO_CHECK = booleanPreferencesKey("auto_check")
    val HL_SAME = booleanPreferencesKey("hl_same")
    val HL_REGION = booleanPreferencesKey("hl_region")
    val SHOW_TIMER = booleanPreferencesKey("show_timer")
    val LOSSES = intPreferencesKey("losses")
    val STREAK = intPreferencesKey("streak")
    val MAX_STREAK = intPreferencesKey("max_streak")
}

class SettingsStore(private val ctx: Context) {
    val settings: Flow<Settings> = ctx.ds.data.map { p ->
        Settings(
            p[Keys.THEME] ?: 0, p[Keys.SOUND] ?: true, p[Keys.VOLUME] ?: 0.6f,
            p[Keys.VIBRATION] ?: true, p[Keys.AUTO_CHECK] ?: true, p[Keys.HL_SAME] ?: true,
            p[Keys.HL_REGION] ?: true, p[Keys.SHOW_TIMER] ?: true,
            p[Keys.LOSSES] ?: 0, p[Keys.STREAK] ?: 0, p[Keys.MAX_STREAK] ?: 0
        )
    }

    suspend fun <T> set(key: Preferences.Key<T>, v: T) { ctx.ds.edit { it[key] = v } }

    suspend fun recordWin() = ctx.ds.edit {
        val s = (it[Keys.STREAK] ?: 0) + 1
        it[Keys.STREAK] = s
        it[Keys.MAX_STREAK] = maxOf(it[Keys.MAX_STREAK] ?: 0, s)
    }

    suspend fun recordLoss() = ctx.ds.edit {
        it[Keys.LOSSES] = (it[Keys.LOSSES] ?: 0) + 1
        it[Keys.STREAK] = 0
    }

    suspend fun resetStats() = ctx.ds.edit {
        it.remove(Keys.LOSSES); it.remove(Keys.STREAK); it.remove(Keys.MAX_STREAK)
    }

    suspend fun resetAll() = ctx.ds.edit { it.clear() }
}
