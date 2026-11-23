package com.qwiksave.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserPreferences {
    private const val PREF_NAME = "qwiksave_user_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_AGE = "user_age"
    private const val KEY_USER_GENDER = "user_gender"
    private const val KEY_AUTH_PROVIDER = "auth_provider"
    private const val KEY_IS_VERIFIED = "is_verified"

    // Battery Overlay & Charging Animation Preferences
    private const val KEY_BATTERY_OVERLAY_ENABLED = "battery_overlay_enabled"
    private const val KEY_CHARGING_ANIM_ENABLED = "charging_anim_enabled"
    private const val KEY_BATTERY_TEMPLATE = "battery_template"
    private const val KEY_BATTERY_POSITION = "battery_position"
    private const val KEY_BATTERY_SCALE = "battery_scale"

    // In-memory reactive state flow to immediately notify navigation of auth verification
    private val _isAuthVerifiedFlow = MutableStateFlow<Boolean?>(null)
    val isAuthVerifiedFlow: StateFlow<Boolean?> = _isAuthVerifiedFlow.asStateFlow()

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
        return if (name.isNotBlank()) name else "User"
    }

    fun setUserName(context: Context, name: String) {
        val trimmed = name.trim()
        val finalName = if (trimmed.isNotBlank()) trimmed else "User"
        getPrefs(context).edit().putString(KEY_USER_NAME, finalName).apply()
    }

    fun getUserAge(context: Context): Int {
        return getPrefs(context).getInt(KEY_USER_AGE, 0)
    }

    fun setUserAge(context: Context, age: Int) {
        getPrefs(context).edit().putInt(KEY_USER_AGE, age.coerceIn(1, 100)).apply()
    }

    fun getUserGender(context: Context): String {
        return getPrefs(context).getString(KEY_USER_GENDER, "") ?: ""
    }

    fun setUserGender(context: Context, gender: String) {
        getPrefs(context).edit().putString(KEY_USER_GENDER, gender.trim()).apply()
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

    fun saveVerifiedAuth(
        context: Context,
        name: String,
        email: String,
        provider: String,
        age: Int = 0,
        gender: String = ""
    ) {
        val trimmedName = name.trim().ifBlank { 
            email.substringBefore("@").replace(".", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
        val editor = getPrefs(context).edit()
            .putString(KEY_USER_NAME, trimmedName)
            .putString(KEY_USER_EMAIL, email.trim())
            .putString(KEY_AUTH_PROVIDER, provider.trim())
            .putBoolean(KEY_IS_VERIFIED, true)
            .putBoolean(KEY_ONBOARDING_DONE, true)
        if (age in 1..100) {
            editor.putInt(KEY_USER_AGE, age)
        }
        if (gender.isNotBlank()) {
            editor.putString(KEY_USER_GENDER, gender.trim())
        }
        editor.apply()
        _isAuthVerifiedFlow.value = true
    }

    fun clearAuth(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_AGE)
            .remove(KEY_USER_GENDER)
            .remove(KEY_AUTH_PROVIDER)
            .putBoolean(KEY_IS_VERIFIED, false)
            .putBoolean(KEY_ONBOARDING_DONE, false)
            .apply()
        _isAuthVerifiedFlow.value = false
    }

    // App User Rating Persistence (once submitted, it is locked)
    private const val KEY_USER_RATING = "user_app_rating_value"
    private const val KEY_RATING_SUBMITTED = "user_app_rating_is_submitted"

    fun hasUserRated(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_RATING_SUBMITTED, false)
    }

    fun getUserRating(context: Context): Float {
        return getPrefs(context).getFloat(KEY_USER_RATING, 0f)
    }

    fun setUserRating(context: Context, rating: Float) {
        getPrefs(context).edit()
            .putFloat(KEY_USER_RATING, rating)
            .putBoolean(KEY_RATING_SUBMITTED, true)
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

    // Battery Overlay Preferences
    fun isBatteryOverlayEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_OVERLAY_ENABLED, false)
    }

    fun setBatteryOverlayEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_OVERLAY_ENABLED, enabled).apply()
    }

    fun isChargingAnimationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CHARGING_ANIM_ENABLED, true)
    }

    fun setChargingAnimationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CHARGING_ANIM_ENABLED, enabled).apply()
    }

    fun getBatteryTemplate(context: Context): String {
        return getPrefs(context).getString(KEY_BATTERY_TEMPLATE, "gear5_luffy") ?: "gear5_luffy"
    }

    fun setBatteryTemplate(context: Context, templateId: String) {
        getPrefs(context).edit().putString(KEY_BATTERY_TEMPLATE, templateId.trim()).apply()
    }

    fun getBatteryPosition(context: Context): String {
        return getPrefs(context).getString(KEY_BATTERY_POSITION, "top_right") ?: "top_right"
    }

    fun setBatteryPosition(context: Context, position: String) {
        getPrefs(context).edit().putString(KEY_BATTERY_POSITION, position.trim()).apply()
    }

    fun getBatteryScale(context: Context): Float {
        return getPrefs(context).getFloat(KEY_BATTERY_SCALE, 1.0f)
    }

    fun setBatteryScale(context: Context, scale: Float) {
        getPrefs(context).edit().putFloat(KEY_BATTERY_SCALE, scale.coerceIn(0.7f, 1.5f)).apply()
    }
}
