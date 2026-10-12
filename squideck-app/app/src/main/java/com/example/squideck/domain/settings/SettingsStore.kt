package com.example.squideck.domain.settings

/** Player preferences that survive app restarts. */
interface SettingsStore {
    fun loadMusicEnabled(): Boolean
    fun saveMusicEnabled(enabled: Boolean)
}
