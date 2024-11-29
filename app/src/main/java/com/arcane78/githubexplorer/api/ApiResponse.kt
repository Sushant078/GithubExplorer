package com.arcane78.githubexplorer.api

sealed class ApiResponse <out T> {
    data class Success<out R>(val data: R): ApiResponse<R>()
    data class Error(val message: String): ApiResponse<Nothing>()
    data object Loading: ApiResponse<Nothing>()
}