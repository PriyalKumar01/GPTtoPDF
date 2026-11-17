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
}
