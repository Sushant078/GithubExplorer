package com.arcane78.githubexplorer.models

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("login") val username: String,
    @SerializedName("id") val id: Int,
    @SerializedName("avatar_url") val avatarUrl: String,
    @SerializedName("url") val url: String,
)
