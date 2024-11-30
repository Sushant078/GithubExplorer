package com.arcane78.githubexplorer.ui.home.adapters

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arcane78.githubexplorer.databinding.UserItemViewBinding
import com.arcane78.githubexplorer.databinding.ErrorItemViewBinding
import com.arcane78.githubexplorer.databinding.LoaderItemViewBinding
import com.bumptech.glide.Glide
import com.arcane78.githubexplorer.models.UserResponse
import com.arcane78.githubexplorer.ui.home.states.ListItemState
import com.arcane78.githubexplorer.ui.profile.ProfileActivity
import com.arcane78.githubexplorer.utils.Extensions.setSafeOnClickListener

class UsersAdapter(
    private val onInitialRetry: () -> Unit,
    private val onPaginationRetry: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ListItemState>()

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_LOADING = 1
        private const val VIEW_TYPE_ERROR = 2
    }

    // Core RecyclerView Functions
    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ListItemState.UserItemState -> VIEW_TYPE_USER
        is ListItemState.LoadingItemState -> VIEW_TYPE_LOADING
        is ListItemState.ErrorItemState -> VIEW_TYPE_ERROR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> createUserViewHolder(parent)
            VIEW_TYPE_LOADING -> createLoadingViewHolder(parent)
            VIEW_TYPE_ERROR -> createErrorViewHolder(parent)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItemState.UserItemState -> (holder as UserViewHolder).bind(item.user)
            is ListItemState.ErrorItemState -> (holder as ErrorViewHolder).bind(
                message = item.message,
                isPaginationError = items.any { it is ListItemState.UserItemState }
            )
            ListItemState.LoadingItemState -> Unit // Nothing to bind
        }
    }

    // ViewHolder Creation
    private fun createUserViewHolder(parent: ViewGroup): UserViewHolder {
        val binding = UserItemViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    private fun createLoadingViewHolder(parent: ViewGroup): LoadingViewHolder {
        val binding = LoaderItemViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LoadingViewHolder(binding)
    }

    private fun createErrorViewHolder(parent: ViewGroup): ErrorViewHolder {
        val binding = ErrorItemViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ErrorViewHolder(binding, onInitialRetry, onPaginationRetry)
    }

    // List Update
    fun updateItems(newItems: List<ListItemState>) {
        val diffResult = DiffUtil.calculateDiff(createDiffCallback(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    // ViewHolders
    private class UserViewHolder(private val binding: UserItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserResponse) {
            binding.apply {
                setupUserInfo(user)
                setupClickListener(user)
            }
        }

        private fun UserItemViewBinding.setupUserInfo(user: UserResponse) {
            usernameText.text = user.username
            bioText.text = user.username
            Glide.with(avatarImage.context)
                .load(user.avatarUrl)
                .into(avatarImage)
        }

        private fun UserItemViewBinding.setupClickListener(user: UserResponse) {
            root.setSafeOnClickListener {
                navigateToProfile(root.context, user.username)
            }
        }

        private fun navigateToProfile(context: Context, username: String) {
            val intent = Intent(context, ProfileActivity::class.java).apply {
                putExtras(Bundle().apply {
                    putString("user", username)
                })
            }
            context.startActivity(intent)
        }
    }

    private class LoadingViewHolder(binding: LoaderItemViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class ErrorViewHolder(
        private val binding: ErrorItemViewBinding,
        private val onInitialRetry: () -> Unit,
        private val onPaginationRetry: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: String, isPaginationError: Boolean) {
            binding.errorMessage.text = message
            binding.retryButton.setSafeOnClickListener {
                if (isPaginationError) {
                    onPaginationRetry()
                } else {
                    onInitialRetry()
                }
            }
        }
    }

    // DiffUtil
    private fun createDiffCallback(
        oldItems: List<ListItemState>,
        newItems: List<ListItemState>
    ) = object : DiffUtil.Callback() {
        override fun getOldListSize() = oldItems.size
        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            compareItems(oldItems[oldItemPosition], newItems[newItemPosition])

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition] == newItems[newItemPosition]

        private fun compareItems(oldItem: ListItemState, newItem: ListItemState): Boolean {
            return when {
                oldItem is ListItemState.UserItemState && newItem is ListItemState.UserItemState ->
                    oldItem.user.id == newItem.user.id
                oldItem is ListItemState.LoadingItemState && newItem is ListItemState.LoadingItemState -> true
                oldItem is ListItemState.ErrorItemState && newItem is ListItemState.ErrorItemState -> true
                else -> false
            }
        }
    }
}