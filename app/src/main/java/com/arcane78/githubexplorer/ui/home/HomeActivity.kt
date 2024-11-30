package com.arcane78.githubexplorer.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arcane78.githubexplorer.databinding.ActivityHomeBinding
import com.arcane78.githubexplorer.repository.GitHubRepository
import com.arcane78.githubexplorer.ui.home.states.ListItemState
import com.arcane78.githubexplorer.ui.home.adapters.UsersAdapter
import com.arcane78.githubexplorer.utils.Extensions.hideKeyboardOnScroll
import com.arcane78.githubexplorer.utils.ThemeManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.provideFactory(GitHubRepository)
    }
    private lateinit var adapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeData()
    }

    private fun setupViews() {
        setupRecyclerView()
        setupSearch()
        setupFabMenu()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        adapter = UsersAdapter(
            onInitialRetry = { viewModel.retry() },
            onPaginationRetry = { viewModel.retryPagination() }
        )

        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = this@HomeActivity.adapter
            hideKeyboardOnScroll()
            setupPagination()
        }
    }

    private fun RecyclerView.setupPagination() {
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (shouldLoadMoreItems()) {
                    loadMoreItems()
                }
            }
        })
    }

    private fun RecyclerView.shouldLoadMoreItems(): Boolean {
        val layoutManager = this.layoutManager as LinearLayoutManager
        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

        return !canScrollVertically(1) &&
                firstVisibleItem + visibleItemCount >= totalItemCount - 5
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            handleRefresh()
        }
    }

    private fun handleRefresh() {
        if (binding.searchEditText.text.isNotEmpty()) {
            viewModel.searchUsers(binding.searchEditText.text.toString(), true)
        } else {
            viewModel.retry()
        }
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun setupSearch() {
        setupSearchView()
        setupClearButton()
    }

    private fun setupSearchView() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateClearButtonVisibility(s)
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.updateSearchQuery(s?.toString() ?: "")
            }
        })
    }

    private fun observeViewModelSearchQuery() {
        lifecycleScope.launch {
            viewModel.searchQueryFlow.collect { query ->
                if (query != binding.searchEditText.text.toString()) {
                    binding.searchEditText.setText(query)
                    binding.searchEditText.setSelection(query.length)
                }
            }
        }
    }

    private fun updateClearButtonVisibility(text: CharSequence?) {
        binding.clearSearchButton.visibility =
            if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupClearButton() {
        binding.clearSearchButton.setOnClickListener {
            clearSearch()
        }
    }

    private fun setupFabMenu() {
        binding.themeFab.setOnClickListener {
            toggleTheme()
        }
    }

    private fun observeData() {
        observeUiState()
        viewModel.observeSearchQuery()
        observeViewModelSearchQuery()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { items ->
                handleUiState(items)
            }
        }
    }

    private fun handleUiState(items: List<ListItemState>) {
        when {
            items.isEmpty() || (items.size == 1 && items.first() is ListItemState.LoadingItemState) -> {
                if (!binding.emptyStateLayout.isVisible) {
                    toggleLoadingState(true)
                }
            }

            items.first() is ListItemState.ErrorItemState && items.size == 1 -> {
                handleErrorState(items.first() as ListItemState.ErrorItemState)
            }

            else -> {
                handleContentState(items)
            }
        }
    }

    private fun handleErrorState(errorState: ListItemState.ErrorItemState) {
        toggleLoadingState(false)
        showEmptyState(true)
        binding.emptyStateMessage.text = errorState.message
        binding.retryButton.setOnClickListener { viewModel.retry() }
    }

    private fun handleContentState(items: List<ListItemState>) {
        toggleLoadingState(false)
        showEmptyState(false)
        adapter.updateItems(items)
    }

    private fun showEmptyState(show: Boolean) {
        binding.emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
        binding.usersRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun toggleLoadingState(isLoading: Boolean) {
        binding.apply {
            initialLoadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            usersRecyclerView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    private fun loadMoreItems() {
        if (binding.searchEditText.text.isNotEmpty()) {
            viewModel.loadMoreSearchResults()
        } else {
            viewModel.loadMoreUsers()
        }
    }

    private fun clearSearch() {
        binding.searchEditText.setText("")
        viewModel.clearSearch()
    }

    private fun toggleTheme() {
        val isNightMode = ThemeManager.isDarkMode(this)
        ThemeManager.setThemeMode(this, !isNightMode)
    }
}