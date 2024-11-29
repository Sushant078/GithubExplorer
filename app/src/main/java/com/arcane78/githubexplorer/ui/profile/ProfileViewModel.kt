package com.arcane78.githubexplorer.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcane78.githubexplorer.api.ApiResponse
import com.arcane78.githubexplorer.models.UserRepoDetails
import com.arcane78.githubexplorer.repository.GitHubRepository
import com.arcane78.githubexplorer.ui.profile.states.UserDetailsState
import com.arcane78.githubexplorer.ui.profile.states.UserRepoDetailsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: GitHubRepository
) : ViewModel() {

    private val _userDetailsFlow = MutableStateFlow<UserDetailsState>(UserDetailsState.Loading)
    val usersDetailsFlow = _userDetailsFlow.asStateFlow()

    private val _userRepoDetailsFlow = MutableStateFlow<UserRepoDetailsState>(UserRepoDetailsState.Loading)
    val usersRepoDetailsFlow = _userRepoDetailsFlow.asStateFlow()

    fun getUserDetails(username:String) {
        viewModelScope.launch {
            repository.getUserDetails(username).collect { response ->
                when(response) {
                    is ApiResponse.Loading -> _userDetailsFlow.value = UserDetailsState.Loading
                    is ApiResponse.Success -> {
                        response.data?.let { user ->
                            _userDetailsFlow.value = UserDetailsState.Success(user)
                        }
                    }
                    is ApiResponse.Error -> _userDetailsFlow.value = UserDetailsState.Error(response.message)
                }
            }
        }
    }

    fun getUserRepoDetails(username:String) {
        viewModelScope.launch {
            repository.getUserRepoDetails(username).collect { response ->
                when(response) {
                    is ApiResponse.Loading -> _userRepoDetailsFlow.value = UserRepoDetailsState.Loading
                    is ApiResponse.Success -> {
                        response.data?.let { user ->
                            _userRepoDetailsFlow.value = UserRepoDetailsState.Success(user)
                        }
                    }
                    is ApiResponse.Error -> _userRepoDetailsFlow.value = UserRepoDetailsState.Error(response.message)
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            githubRepository: GitHubRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(githubRepository) as T
            }
        }
    }
}