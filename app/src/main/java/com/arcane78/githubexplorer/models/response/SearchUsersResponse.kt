package com.arcane78.githubexplorer.models.response

import com.arcane78.githubexplorer.models.UserResponse
import com.google.gson.annotations.SerializedName

data class SearchUsersResponse(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("incomplete_results") val incompleteResults: Boolean,
    @SerializedName("items") val users: List<UserResponse>
)
