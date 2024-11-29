package com.arcane78.githubexplorer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.arcane78.githubexplorer.api.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response

object Extensions {
    fun <T> getNetworkCallResultFlow(call: suspend () -> Response<T>?): Flow<ApiResponse<T?>> {
        return flow {
            emit(ApiResponse.Loading)
            try {
                val c = call()
                c?.let {
                    if (c.isSuccessful) {
                        emit(ApiResponse.Success(c.body()))
                    } else {
                        c.errorBody()?.let {
                            val error = it.string()
                            it.close()
                            emit(ApiResponse.Error(error))
                        }
                    }
                }
            } catch (e: Exception) {
                emit(ApiResponse.Error(e.toString()))
            }
        }.flowOn(Dispatchers.IO)
    }

    fun String?.ifValid(block: (String) -> Unit) {
        if (!this.isNullOrEmpty() && this.isNotBlank()) {
            block(this)
        }
    }

    fun String?.orDash(): String = this ?: "-"

    fun Int?.orDash(): String = this?.toString() ?: "-"

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    fun Int?.toFormattedCount(): String {
        return when {
            this == null -> "-"
            this < 1000 -> this.toString()
            this < 1_000_000 -> String.format("%.2fK", this / 1000.0)
            else -> String.format("%.2fM", this / 1_000_000.0)
        }
    }

    fun Context.openInBrowser(url: String) {
        try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Open with"))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open browser",Toast.LENGTH_SHORT).show()
        }
    }

    fun RecyclerView.hideKeyboardOnScroll() {
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard()
                }
            }
        })
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

}