package com.example.squideck.data

import android.content.Context
import com.example.squideck.domain.settings.SettingsStore

class SharedPrefsSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override fun loadMusicEnabled(): Boolean = prefs.getBoolean(KEY_MUSIC, true)

    override fun saveMusicEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC, enabled).apply()
    }

    private companion object {
        const val KEY_MUSIC = "music"
    }
}
