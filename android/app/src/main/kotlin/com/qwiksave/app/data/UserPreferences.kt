package com.qwiksave.app.data

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private const val PREF_NAME = "qwiksave_user_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_AUTH_PROVIDER = "auth_provider"
    private const val KEY_IS_VERIFIED = "is_verified"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun hasCompletedOnboarding(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun setOnboardingCompleted(context: Context, done: Boolean = true) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }

    fun getUserName(context: Context): String {
        val name = getPrefs(context).getString(KEY_USER_NAME, "")?.trim() ?: ""
        return if (name.isNotBlank()) name else "Guy's"
    }

    fun setUserName(context: Context, name: String) {
        val trimmed = name.trim()
        val finalName = if (trimmed.isNotBlank()) trimmed else "Guy's"
        getPrefs(context).edit().putString(KEY_USER_NAME, finalName).apply()
    }

    fun getUserEmail(context: Context): String {
        return getPrefs(context).getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun getAuthProvider(context: Context): String {
        return getPrefs(context).getString(KEY_AUTH_PROVIDER, "") ?: ""
    }

    fun isUserVerified(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_VERIFIED, false)
    }

    fun saveVerifiedAuth(context: Context, name: String, email: String, provider: String) {
        val trimmedName = name.trim().ifBlank { email.substringBefore("@").replace(".", " ").capitalize() }
        getPrefs(context).edit()
            .putString(KEY_USER_NAME, trimmedName)
            .putString(KEY_USER_EMAIL, email.trim())
            .putString(KEY_AUTH_PROVIDER, provider.trim())
            .putBoolean(KEY_IS_VERIFIED, true)
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()
    }

    private const val KEY_LIKED_SONG_IDS = "liked_song_ids_set"
    private const val KEY_DOWNLOADED_SONGS = "downloaded_songs_map"

    fun getLikedSongIds(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_LIKED_SONG_IDS, emptySet()) ?: emptySet()
    }

    fun toggleLikedSong(context: Context, songId: String): Boolean {
        val current = getLikedSongIds(context).toMutableSet()
        val isNowLiked = if (current.contains(songId)) {
            current.remove(songId)
            false
        } else {
            current.add(songId)
            true
        }
        getPrefs(context).edit().putStringSet(KEY_LIKED_SONG_IDS, current).apply()
        return isNowLiked
    }

    fun isSongLiked(context: Context, songId: String): Boolean {
        return getLikedSongIds(context).contains(songId)
    }

    fun getDownloadedSongPaths(context: Context): Map<String, String> {
        val str = getPrefs(context).getString(KEY_DOWNLOADED_SONGS, "{}") ?: "{}"
        return try {
            val json = org.json.JSONObject(str)
            val map = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = json.getString(k)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveDownloadedSong(context: Context, songId: String, localPath: String) {
        try {
            val str = getPrefs(context).getString(KEY_DOWNLOADED_SONGS, "{}") ?: "{}"
            val json = org.json.JSONObject(str)
            json.put(songId, localPath)
            getPrefs(context).edit().putString(KEY_DOWNLOADED_SONGS, json.toString()).apply()
        } catch (_: Exception) {}
    }

    fun saveDownloadedSongPath(context: Context, songId: String, localPath: String) {
        saveDownloadedSong(context, songId, localPath)
    }
}
