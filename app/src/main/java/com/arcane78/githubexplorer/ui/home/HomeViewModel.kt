package com.arcane78.githubexplorer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcane78.githubexplorer.api.ApiResponse
import com.arcane78.githubexplorer.models.UserResponse
import com.arcane78.githubexplorer.models.response.SearchUsersResponse
import com.arcane78.githubexplorer.repository.GitHubRepository
import com.arcane78.githubexplorer.ui.home.states.ListItemState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: GitHubRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<List<ListItemState>>(emptyList())
    val uiState = _uiState.asStateFlow()

    private var isLoading = false
    private var isSearchActive = false
    private var lastUserId = 0
    private var lastSearchPage = 1

    private var searchJob: Job? = null
    private var currentSearchQuery = ""
    private var hasMoreSearchResults = true
    private var totalSearchResults = 0

    private val usersList = mutableListOf<UserResponse>()
    private val searchResults = mutableListOf<UserResponse>()


    private val _searchQueryFlow = MutableStateFlow("")
    val searchQueryFlow = _searchQueryFlow.asStateFlow()

    private val _intermediateQueryFlow = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    fun observeSearchQuery() {
        viewModelScope.launch {
            _intermediateQueryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    _searchQueryFlow.value = query

                    if (query.isBlank()) {
                        clearSearch()
                    } else {
                        searchUsers(query.trim(), isNewSearch = true)
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _intermediateQueryFlow.value = query
    }

    init {
        loadInitialUsers()
    }

    private fun loadInitialUsers() {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            repository.getAllUsers(since = 0).collect { response ->
                when (response) {
                    is ApiResponse.Success -> handleInitialUsersSuccess(response.data)
                    is ApiResponse.Error -> handleInitialError(response.message)
                    is ApiResponse.Loading -> handleInitialLoading()
                }
            }
        }
    }

    fun loadMoreUsers() {
        if (isLoading || isSearchActive) return
        isLoading = true

        viewModelScope.launch {
            appendLoadingItem()
            repository.getAllUsers(since = lastUserId).collect { response ->
                when (response) {
                    is ApiResponse.Success -> handleLoadMoreSuccess(response.data)
                    is ApiResponse.Error -> handleLoadMoreError(response.message)
                    is ApiResponse.Loading -> Unit
                }
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

    private fun handleInitialUsersSuccess(users: List<UserResponse>?) {
        users?.let {
            usersList.clear()
            usersList.addAll(it)
            lastUserId = usersList.lastOrNull()?.id ?: 0
            updateUiState(usersList.map { ListItemState.UserItemState(it) })
        }
        isLoading = false
    }

    private fun handleLoadMoreSuccess(newUsers: List<UserResponse>?) {
        newUsers?.let {
            usersList.addAll(it)
            lastUserId = usersList.lastOrNull()?.id ?: lastUserId
            updateUiState(usersList.map { ListItemState.UserItemState(it) })
        }
        isLoading = false
    }

    private fun handleInitialError(message: String) {
        updateUiState(listOf(ListItemState.ErrorItemState(message)))
        isLoading = false
    }

    private fun handleLoadMoreError(message: String) {
        updateUiState(buildErrorState(usersList, message))
        isLoading = false
    }

    private fun handleInitialLoading() {
        if (_uiState.value.isEmpty()) {
            updateUiState(listOf(ListItemState.LoadingItemState))
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

    private fun handleSearchError(response: ApiResponse.Error) {
        updateUiState(if (lastSearchPage == 1) {
            listOf(ListItemState.ErrorItemState(response.message))
        } else {
            buildErrorState(searchResults, response.message)
        })
        isLoading = false
    }

    private fun executeSearch() {
        searchJob = viewModelScope.launch {
            repository.searchUsers(query = currentSearchQuery, page = lastSearchPage)
                .collect { response ->
                    handleSearchResponse(response)
                }
        }
    }

    private fun updateSearchState(searchResponse: SearchUsersResponse) {
        if (lastSearchPage == 1) {
            totalSearchResults = searchResponse.totalCount
        }
    }

    private fun updateSearchResults(newUsers: List<UserResponse>) {
        when {
            lastSearchPage == 1 && (newUsers.isEmpty() || totalSearchResults == 0) -> {
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
        updateUiState(listOf(ListItemState.ErrorItemState("No users found matching '$currentSearchQuery'")))
        hasMoreSearchResults = false
    }

    private fun handleNoMoreSearchResults() {
        hasMoreSearchResults = false
        updateUiState(searchResults.map { ListItemState.UserItemState(it) })
    }

    private fun handleValidSearchResults(newUsers: List<UserResponse>) {
        if (lastSearchPage == 1) {
            searchResults.clear()
        }
        searchResults.addAll(newUsers)
        hasMoreSearchResults = searchResults.size < totalSearchResults
        if (hasMoreSearchResults) {
            lastSearchPage++
        }
        updateUiState(searchResults.map { ListItemState.UserItemState(it) })
    }

    private fun appendLoadingItem() {
        val currentItems = _uiState.value.toMutableList()
        currentItems.add(ListItemState.LoadingItemState)
        updateUiState(currentItems)
    }

    private fun shouldSkipSearch(query: String, isNewSearch: Boolean): Boolean {
        return (!isNewSearch && (!hasMoreSearchResults || isLoading))
    }

    private fun setupSearchState(query: String, isNewSearch: Boolean) {
        if (isNewSearch) {
            searchJob?.cancel()
            resetSearchForNewQuery(query)
            updateUiState(listOf(ListItemState.LoadingItemState))
        } else if (!isLoading) {
            appendLoadingItem()
        }
        isSearchActive = true
        isLoading = true
    }

    private fun resetSearchForNewQuery(query: String) {
        currentSearchQuery = query
        lastSearchPage = 1
        searchResults.clear()
        hasMoreSearchResults = true
        totalSearchResults = 0
    }

    private fun resetSearchState() {
        isSearchActive = false
        currentSearchQuery = ""
        lastSearchPage = 1
        searchResults.clear()
        hasMoreSearchResults = true
        totalSearchResults = 0
    }

    fun clearSearch() {
        resetSearchState()
        updateUiState(usersList.map { ListItemState.UserItemState(it) })
    }

    fun retry() {
        if (isSearchActive) {
            searchResults.clear()
            lastSearchPage = 1
            searchUsers(currentSearchQuery, isNewSearch = true)
        } else {
            usersList.clear()
            lastUserId = 0
            loadInitialUsers()
        }
    }

    fun retryPagination() {
        if (isSearchActive) {
            searchUsers(currentSearchQuery, isNewSearch = false)
        } else {
            loadMoreUsers()
        }
    }

    private fun buildErrorState(
        existingItems: List<UserResponse>,
        errorMessage: String
    ): List<ListItemState> = buildList {
        addAll(existingItems.map { ListItemState.UserItemState(it) })
        add(ListItemState.ErrorItemState(errorMessage))
    }

    private fun updateUiState(newState: List<ListItemState>) {
        _uiState.value = newState
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