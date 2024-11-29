package com.arcane78.githubexplorer.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
    private var isFabMenuExpanded = false
    private val searchQueryFlow = MutableStateFlow("")

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
        adapter = UsersAdapter(onRetry = { viewModel.retry() })

        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = this@HomeActivity.adapter
            hideKeyboardOnScroll()
            addPaginationScrollListener()
        }
    }

    private fun RecyclerView.addPaginationScrollListener() {
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
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

    private fun loadMoreItems() {
        if (binding.searchEditText.text.isNotEmpty()) {
            viewModel.loadMoreSearchResults()
        } else {
            viewModel.loadMoreUsers()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (binding.searchEditText.text.isNotEmpty()) {
                viewModel.searchUsers(binding.searchEditText.text.toString(), true)
            } else {
                viewModel.retry()
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.clearSearchButton.visibility =
                    if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {
                searchQueryFlow.value = s?.toString() ?: ""
            }
        })

        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.setText("")
            viewModel.clearSearch()
        }
    }

    private fun setupFabMenu() {
        binding.menuFab.setOnClickListener {
            if (isFabMenuExpanded) collapseFabMenu() else expandFabMenu()
        }

        binding.themeFab.setOnClickListener {
            toggleTheme()
            collapseFabMenu()
        }
    }

    private fun expandFabMenu() {
        isFabMenuExpanded = true
        binding.themeFab.show()
        binding.futureActionFab.show()
    }

    private fun collapseFabMenu() {
        isFabMenuExpanded = false
        binding.themeFab.hide()
        binding.futureActionFab.hide()
    }

    private fun showEmptyState(show: Boolean) {
        binding.emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
        binding.usersRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun observeData() {
        observeUiState()
        observeSearch()
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
            items.isEmpty() -> handleEmptyList()
            items.size == 1 -> handleSingleItem(items.first())
            else -> handleUsersList(items)
        }
    }

    private fun handleEmptyList() {
        toggleLoadingState(true)
    }

    private fun handleSingleItem(item: ListItemState) {
        when (item) {
            is ListItemState.LoadingItemState -> {
                toggleLoadingState(true)
            }
            is ListItemState.ErrorItemState -> {
                toggleLoadingState(false)
                showEmptyState(true)
                binding.emptyStateMessage.text = item.message
                binding.retryButton.setOnClickListener { viewModel.retry() }
            }
            is ListItemState.UserItemState -> {
                handleUsersList(listOf(item))
            }
        }
    }

    private fun handleUsersList(items: List<ListItemState>) {
        toggleLoadingState(false)
        showEmptyState(false)
        adapter.updateItems(items)
    }

    private fun toggleLoadingState(isLoading: Boolean) {
        binding.apply {
            initialLoadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            usersRecyclerView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        lifecycleScope.launch {
            searchQueryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        viewModel.clearSearch()
                    } else {
                        viewModel.searchUsers(query.trim(), isNewSearch = true)
                    }
                }
        }
    }

    private fun toggleTheme() {
        val isNightMode = ThemeManager.isDarkMode(this)
        ThemeManager.setThemeMode(this, !isNightMode)
    }
}