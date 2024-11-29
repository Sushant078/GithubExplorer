package com.arcane78.githubexplorer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.arcane78.githubexplorer.utils.ThemeManager

class GithubExplorerApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(
            if (ThemeManager.isDarkMode(this))
                AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}