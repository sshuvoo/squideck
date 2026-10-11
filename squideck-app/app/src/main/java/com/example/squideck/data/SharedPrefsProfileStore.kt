package com.example.squideck.data

import android.content.Context
import com.example.squideck.domain.profile.ProfileStore

class SharedPrefsProfileStore(context: Context) : ProfileStore {
    private val prefs = context.applicationContext.getSharedPreferences("profile", Context.MODE_PRIVATE)

    override fun loadName(): String? = prefs.getString(KEY_NAME, null)

    override fun saveName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    private companion object {
        const val KEY_NAME = "name"
    }
}
