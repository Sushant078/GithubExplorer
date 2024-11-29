package com.arcane78.githubexplorer.models

import com.google.gson.annotations.SerializedName

data class UserDetails(
    val name: String? = null,
    val bio: String? = null,
    @SerializedName("repos_url")
    val reposUrl: String? = null,
    @SerializedName("avatar_url")
    val avatar: String? = null,
    @SerializedName("public_repos")
    val publicReposCount: Int? = null,
    @SerializedName("followers")
    val followersCount: Int? = null,
)
