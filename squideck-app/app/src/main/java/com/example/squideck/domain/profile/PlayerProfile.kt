package com.example.squideck.domain.profile

/** Where the player's chosen name is remembered between launches. */
interface ProfileStore {
    fun loadName(): String?
    fun saveName(name: String)
}

/** Rules for the display name typed on the first screen. */
object PlayerName {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 14

    /** Trims and collapses repeated spaces, then caps the length. */
    fun clean(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(MAX_LENGTH)

    fun isValid(raw: String): Boolean = clean(raw).length >= MIN_LENGTH
}
