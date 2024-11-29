package com.arcane78.githubexplorer.api

import com.arcane78.githubexplorer.models.UserDetails
import com.arcane78.githubexplorer.models.UserRepoDetails
import com.arcane78.githubexplorer.models.response.SearchUsersResponse
import com.arcane78.githubexplorer.models.UserResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubApi {
    @GET("/users")
    suspend fun getAllUsers(
        @Query("since") since: Int,
        @Query("per_page") perPage: Int
    ): Response<List<UserResponse>>

    @GET("/search/users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<SearchUsersResponse>

    @GET("users/{username}")
    suspend fun getUser(
        @Path("username") username: String
    ): Response<UserDetails>

    @GET("users/{username}/repos")
    suspend fun getUserRepos(
        @Path("username") username: String
    ): Response<List<UserRepoDetails>>
}