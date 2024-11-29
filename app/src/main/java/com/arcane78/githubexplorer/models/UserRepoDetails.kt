package com.arcane78.githubexplorer.models

import com.google.gson.annotations.SerializedName

data class UserRepoDetails(
    val id:Int,
    val name:String? = null,
    val description: String? = null,
    @SerializedName("stargazers_count")
    val starCount: Int? = null,
    @SerializedName("forks_count")
    val forkCount: Int? = null,
    @SerializedName("html_url")
    val url: String? = null,
)