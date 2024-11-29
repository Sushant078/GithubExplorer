package com.arcane78.githubexplorer.ui.profile

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arcane78.githubexplorer.databinding.ActivityProfileBinding
import com.arcane78.githubexplorer.repository.GitHubRepository
import com.arcane78.githubexplorer.ui.profile.adapters.UserReposAdapter
import com.arcane78.githubexplorer.ui.profile.states.UserDetailsState
import com.arcane78.githubexplorer.ui.profile.states.UserRepoDetailsState
import com.arcane78.githubexplorer.utils.Extensions.ifValid
import com.arcane78.githubexplorer.utils.Extensions.orDash
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch


class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModel.provideFactory(GitHubRepository)
    }
    private val reposAdapter = UserReposAdapter()
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupViews()
        handleIntent()
    }

    private fun setupViews() {
        setupRecyclerView()
        setupErrorViews()
    }

    private fun setupRecyclerView() {
        binding.rvRepos.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = reposAdapter
        }
    }

    private fun setupErrorViews() {
        binding.apply {
            userDetailsError.retryButton.setOnClickListener { retryUserDetails() }
            reposError.retryButton.setOnClickListener { retryRepoDetails() }
        }
    }

    private fun handleIntent() {
        intent.extras?.getString("user")?.ifValid {
            username = it
            loadData()
        } ?: finish() // Invalid data, close activity
    }

    private fun loadData() {
        viewModel.getUserDetails(username)
        viewModel.getUserRepoDetails(username)
        observeData()
    }

    private fun retryUserDetails() {
        viewModel.getUserDetails(username)
    }

    private fun retryRepoDetails() {
        viewModel.getUserRepoDetails(username)
    }

    private fun observeData() {
        observeUserDetails()
        observeRepoDetails()
    }

    private fun observeUserDetails() {
        lifecycleScope.launch {
            viewModel.usersDetailsFlow.collect { state ->
                handleUserDetailsState(state)
            }
        }
    }

    private fun observeRepoDetails() {
        lifecycleScope.launch {
            viewModel.usersRepoDetailsFlow.collect { state ->
                handleRepoDetailsState(state)
            }
        }
    }

    private fun handleUserDetailsState(state: UserDetailsState) {
        binding.apply {
            userDetailsProgress.isVisible = state is UserDetailsState.Loading
            userDetailsError.root.isVisible = state is UserDetailsState.Error
            infoGroup.visibility = if(state is UserDetailsState.Error || state is UserDetailsState.Loading){
                View.INVISIBLE
            }else{
                View.VISIBLE
            }

            when (state) {
                is UserDetailsState.Success -> setupUserDetails(state)
                is UserDetailsState.Error -> userDetailsError.errorMessage.text = state.message
                is UserDetailsState.Loading -> Unit
            }
        }
    }


    private fun handleRepoDetailsState(state: UserRepoDetailsState) {
        binding.apply {
            reposProgress.isVisible = state is UserRepoDetailsState.Loading
            rvRepos.isVisible = state is UserRepoDetailsState.Success
            reposError.root.isVisible = state is UserRepoDetailsState.Error

            when (state) {
                is UserRepoDetailsState.Success -> reposAdapter.submitList(state.userRepos)
                is UserRepoDetailsState.Error -> reposError.errorMessage.text = state.message
                is UserRepoDetailsState.Loading -> Unit
            }
        }
    }

    private fun setupUserDetails(state: UserDetailsState.Success) {
        binding.apply {
            binding.infoGroup.visibility = View.VISIBLE
            textViewUsername.text = state.userDetails.name.orDash()
            textViewFollowersCount.text = state.userDetails.followersCount.orDash()
            textViewReposCount.text = state.userDetails.publicReposCount.orDash()
            textViewBio.text = state.userDetails.bio.orDash()

            Glide.with(this@ProfileActivity)
                .load(state.userDetails.avatar)
                .into(imageViewAvatar)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}