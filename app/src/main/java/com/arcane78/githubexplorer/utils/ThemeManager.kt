package com.arcane78.githubexplorer.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val THEME_PREFS = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    fun setThemeMode(context: Context, isDarkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_THEME_MODE, isDarkMode)
            .apply()
    }

    fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_THEME_MODE, false)
    }
}