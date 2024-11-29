package com.arcane78.githubexplorer.ui.home.states

import com.arcane78.githubexplorer.models.UserResponse

sealed class ListItemState {
    data class UserItemState(val user: UserResponse) : ListItemState()
    data class ErrorItemState(val message: String) : ListItemState()
    data object LoadingItemState : ListItemState()
}
