package com.arcane78.githubexplorer.ui.profile.states

import com.arcane78.githubexplorer.models.UserDetails

sealed class UserDetailsState {
    data object Loading : UserDetailsState()
    data class Success(val userDetails: UserDetails) : UserDetailsState()
    data class Error(val message: String) : UserDetailsState()
}