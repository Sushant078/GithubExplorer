package com.arcane78.githubexplorer.ui.home.adapters

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

class UsersAdapter(private val onRetry: () -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<ListItemState>()

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_LOADING = 1
        private const val VIEW_TYPE_ERROR = 2
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ListItemState.UserItemState -> VIEW_TYPE_USER
        is ListItemState.LoadingItemState -> VIEW_TYPE_LOADING
        is ListItemState.ErrorItemState -> VIEW_TYPE_ERROR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> UserViewHolder(
                UserItemViewBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_LOADING -> LoadingViewHolder(
                LoaderItemViewBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_ERROR -> ErrorViewHolder(
                ErrorItemViewBinding.inflate(inflater, parent, false),
                onRetry
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItemState.UserItemState -> (holder as UserViewHolder).bind(item.user)
            is ListItemState.ErrorItemState -> (holder as ErrorViewHolder).bind(item.message)
            ListItemState.LoadingItemState -> Unit
        }
    }

    fun updateItems(newItems: List<ListItemState>) {
        val diffCallback = createDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    private class UserViewHolder(private val binding: UserItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserResponse) {
            binding.apply {
                usernameText.text = user.username
                bioText.text = user.username
                Glide.with(avatarImage.context)
                    .load(user.avatarUrl)
                    .into(avatarImage)

                root.setSafeOnClickListener {
                    navigateToProfile(user.username)
                }
            }
        }

        private fun navigateToProfile(username: String) {
            val context = binding.root.context
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
        private val onRetry: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: String) {
            binding.errorMessage.text = message
            binding.retryButton.setSafeOnClickListener { onRetry() }
        }
    }

    private fun createDiffCallback(
        oldItems: List<ListItemState>,
        newItems: List<ListItemState>
    ) = object : DiffUtil.Callback() {
        override fun getOldListSize() = oldItems.size
        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return when {
                oldItem is ListItemState.UserItemState && newItem is ListItemState.UserItemState ->
                    oldItem.user.id == newItem.user.id
                oldItem is ListItemState.LoadingItemState && newItem is ListItemState.LoadingItemState -> true
                oldItem is ListItemState.ErrorItemState && newItem is ListItemState.ErrorItemState -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}