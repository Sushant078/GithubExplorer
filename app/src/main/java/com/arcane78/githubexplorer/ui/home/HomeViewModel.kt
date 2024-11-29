package com.arcane78.githubexplorer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcane78.githubexplorer.api.ApiResponse
import com.arcane78.githubexplorer.models.UserResponse
import com.arcane78.githubexplorer.models.response.SearchUsersResponse
import com.arcane78.githubexplorer.repository.GitHubRepository
import com.arcane78.githubexplorer.ui.home.states.ListItemState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: GitHubRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<List<ListItemState>>(emptyList())
    val uiState = _uiState.asStateFlow()

    // State flags
    private var isLoading = false
    private var isSearchActive = false

    // Search state
    private var searchJob: Job? = null
    private var currentSearchQuery = ""
    private var currentSearchPage = 1
    private var hasMoreSearchResults = true
    private var totalSearchResults = 0

    // Data holders
    private val usersList = mutableListOf<UserResponse>()
    private val searchResults = mutableListOf<UserResponse>()
    private var lastUserId = 0

    init {
        loadInitialUsers()
    }

    private fun loadInitialUsers() {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            setLoadingState()
            repository.getAllUsers(since = 0).collect { response ->
                handleInitialUsersResponse(response)
            }
        }
    }

    fun loadMoreUsers() {
        if (isLoading || isSearchActive) return
        isLoading = true

        viewModelScope.launch {
            appendLoadingItem()
            repository.getAllUsers(since = lastUserId).collect { response ->
                handleLoadMoreUsersResponse(response)
            }
        }
    }

    fun searchUsers(query: String, isNewSearch: Boolean = true) {
        if (query.isBlank()) {
            clearSearch()
            return
        }

        if (shouldSkipSearch(query, isNewSearch)) return

        setupSearchState(query, isNewSearch)
        executeSearch()
    }

    fun loadMoreSearchResults() {
        if (isSearchActive && !isLoading && hasMoreSearchResults) {
            searchUsers(currentSearchQuery, isNewSearch = false)
        }
    }

    fun clearSearch() {
        resetSearchState()
        _uiState.value = usersList.map { ListItemState.UserItemState(it) }
    }

    fun retry() {
        if (isSearchActive) {
            searchUsers(currentSearchQuery, isNewSearch = true)
        } else {
            loadInitialUsers()
        }
    }

    private fun setLoadingState() {
        _uiState.value = listOf(ListItemState.LoadingItemState)
    }

    private fun appendLoadingItem() {
        val currentItems = _uiState.value.toMutableList()
        currentItems.add(ListItemState.LoadingItemState)
        _uiState.value = currentItems
    }

    private fun handleInitialUsersResponse(response: ApiResponse<List<UserResponse>?>) {
        when (response) {
            is ApiResponse.Success -> {
                response.data?.let { users ->
                    usersList.clear()
                    usersList.addAll(users)
                    lastUserId = usersList.lastOrNull()?.id ?: 0
                    _uiState.value = usersList.map { ListItemState.UserItemState(it) }
                }
                isLoading = false
            }
            is ApiResponse.Error -> {
                _uiState.value = listOf(ListItemState.ErrorItemState(response.message))
                isLoading = false
            }
            is ApiResponse.Loading -> Unit
        }
    }

    private fun handleLoadMoreUsersResponse(response: ApiResponse<List<UserResponse>?>) {
        when (response) {
            is ApiResponse.Success -> {
                response.data?.let { newUsers ->
                    usersList.addAll(newUsers)
                    lastUserId = usersList.lastOrNull()?.id ?: 0
                    _uiState.value = usersList.map { ListItemState.UserItemState(it) }
                }
                isLoading = false
            }
            is ApiResponse.Error -> {
                _uiState.value = buildErrorState(usersList, response.message)
                isLoading = false
            }
            is ApiResponse.Loading -> Unit
        }
    }

    private fun shouldSkipSearch(query: String, isNewSearch: Boolean): Boolean {
        return (!isNewSearch && (!hasMoreSearchResults || isLoading))
    }

    private fun setupSearchState(query: String, isNewSearch: Boolean) {
        if (isNewSearch) {
            searchJob?.cancel()
            resetSearchForNewQuery(query)
            setLoadingState()
        } else if (!isLoading) {
            appendLoadingItem()
        }

        isSearchActive = true
        isLoading = true
    }

    private fun resetSearchForNewQuery(query: String) {
        currentSearchQuery = query
        currentSearchPage = 1
        searchResults.clear()
        hasMoreSearchResults = true
        totalSearchResults = 0
    }

    private fun resetSearchState() {
        isSearchActive = false
        currentSearchQuery = ""
        currentSearchPage = 1
        searchResults.clear()
        hasMoreSearchResults = true
        totalSearchResults = 0
    }

    private fun executeSearch() {
        searchJob = viewModelScope.launch {
            repository.searchUsers(query = currentSearchQuery, page = currentSearchPage)
                .collect { response ->
                    handleSearchResponse(response)
                }
        }
    }

    private fun handleSearchResponse(response: ApiResponse<SearchUsersResponse?>) {
        when (response) {
            is ApiResponse.Success -> handleSuccessfulSearch(response)
            is ApiResponse.Error -> handleSearchError(response)
            is ApiResponse.Loading -> Unit
        }
    }

    private fun handleSuccessfulSearch(response: ApiResponse.Success<SearchUsersResponse?>) {
        response.data?.let { searchResponse ->
            updateSearchState(searchResponse)
            updateSearchResults(searchResponse.users)
        }
        isLoading = false
    }

    private fun updateSearchState(searchResponse: SearchUsersResponse) {
        if (currentSearchPage == 1) {
            totalSearchResults = searchResponse.totalCount
        }
    }

    private fun updateSearchResults(newUsers: List<UserResponse>) {
        when {
            currentSearchPage == 1 && (newUsers.isEmpty() || totalSearchResults == 0) -> {
                handleEmptySearchResults()
            }
            newUsers.isEmpty() -> {
                handleNoMoreSearchResults()
            }
            else -> {
                handleValidSearchResults(newUsers)
            }
        }
    }

    private fun handleEmptySearchResults() {
        _uiState.value = listOf(ListItemState.ErrorItemState("No users found matching '$currentSearchQuery'"))
        hasMoreSearchResults = false
    }

    private fun handleNoMoreSearchResults() {
        hasMoreSearchResults = false
        _uiState.value = searchResults.map { ListItemState.UserItemState(it) }
    }

    private fun handleValidSearchResults(newUsers: List<UserResponse>) {
        searchResults.addAll(newUsers)
        hasMoreSearchResults = searchResults.size < totalSearchResults
        if (hasMoreSearchResults) {
            currentSearchPage++
        }
        _uiState.value = searchResults.map { ListItemState.UserItemState(it) }
    }

    private fun handleSearchError(response: ApiResponse.Error) {
        _uiState.value = if (currentSearchPage == 1) {
            listOf(ListItemState.ErrorItemState(response.message))
        } else {
            buildErrorState(searchResults, response.message)
        }
        isLoading = false
    }

    private fun buildErrorState(
        existingItems: List<UserResponse>,
        errorMessage: String
    ): List<ListItemState> = buildList {
        addAll(existingItems.map { ListItemState.UserItemState(it) })
        add(ListItemState.ErrorItemState(errorMessage))
    }

    companion object {
        fun provideFactory(
            githubRepository: GitHubRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(githubRepository) as T
            }
        }
    }
}