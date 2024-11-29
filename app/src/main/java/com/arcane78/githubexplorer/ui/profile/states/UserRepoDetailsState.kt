package com.arcane78.githubexplorer.ui.profile.states

import com.arcane78.githubexplorer.models.UserDetails
import com.arcane78.githubexplorer.models.UserRepoDetails

sealed class UserRepoDetailsState {
    data object Loading : UserRepoDetailsState()
    data class Success(val userRepos: List<UserRepoDetails>) : UserRepoDetailsState()
    data class Error(val message: String) : UserRepoDetailsState()
}