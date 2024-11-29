package com.arcane78.githubexplorer.repository

import com.arcane78.githubexplorer.api.GithubApi
import com.arcane78.githubexplorer.api.client.ApiClient
import com.arcane78.githubexplorer.utils.Extensions.getNetworkCallResultFlow

object GitHubRepository {
    private val service = ApiClient.getRetrofitClient().create(GithubApi::class.java)

    suspend fun getAllUsers(
        since: Int = 0,
        perPage: Int = 30
    ) = getNetworkCallResultFlow {
        service.getAllUsers(since, perPage)
    }

    suspend fun searchUsers(
        query: String,
        page: Int = 1,
        perPage: Int = 30
    ) = getNetworkCallResultFlow {
        service.searchUsers(query, page, perPage)
    }

    suspend fun getUserDetails(
        userName: String,
    ) = getNetworkCallResultFlow {
        service.getUser(userName)
    }

    suspend fun getUserRepoDetails(
        userName: String,
    ) = getNetworkCallResultFlow {
        service.getUserRepos(userName)
    }
}