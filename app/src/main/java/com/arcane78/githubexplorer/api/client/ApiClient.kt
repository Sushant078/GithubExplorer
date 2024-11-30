package com.arcane78.githubexplorer.api.client

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ApiClient {
    private const val BASE_URL = "https://api.github.com/"
//    private const val GITHUB_TOKEN = "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"

    fun getRetrofitClient(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    private var client: OkHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val newRequest: Request = chain.request().newBuilder()
//            .addHeader("Authorization", "Bearer $GITHUB_TOKEN")
            .build()
        chain.proceed(newRequest)
    }.build()
}