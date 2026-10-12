package com.example.squideck.data

import android.content.Context
import com.example.squideck.domain.profile.ProfileStore

class SharedPrefsProfileStore(context: Context) : ProfileStore {
    private val prefs = context.applicationContext.getSharedPreferences("profile", Context.MODE_PRIVATE)

    override fun loadName(): String? = prefs.getString(KEY_NAME, null)

    override fun saveName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    override fun loadAvatarId(): Int? = if (prefs.contains(KEY_AVATAR)) prefs.getInt(KEY_AVATAR, 0) else null

    override fun saveAvatarId(avatarId: Int) {
        prefs.edit().putInt(KEY_AVATAR, avatarId).apply()
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_AVATAR = "avatar"
    }
}
