package com.arcane78.githubexplorer.ui.profile.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arcane78.githubexplorer.databinding.RepoItemViewBinding
import com.arcane78.githubexplorer.models.UserRepoDetails
import com.arcane78.githubexplorer.models.UserResponse
import com.arcane78.githubexplorer.utils.Extensions.ifValid
import com.arcane78.githubexplorer.utils.Extensions.openInBrowser
import com.arcane78.githubexplorer.utils.Extensions.orDash
import com.arcane78.githubexplorer.utils.Extensions.setSafeOnClickListener
import com.arcane78.githubexplorer.utils.Extensions.toFormattedCount


class UserReposAdapter: RecyclerView.Adapter<UserReposAdapter.UserRepoViewHolder>() {

    private val userRepoList = mutableListOf<UserRepoDetails>()

    class UserRepoViewHolder(private val binding: RepoItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(repoDetails: UserRepoDetails) {
            binding.tvRepoName.text = repoDetails.name.orDash()
            binding.tvDescription.text = repoDetails.description.orDash()
            binding.tvForkCount.text = repoDetails.forkCount.toFormattedCount()
            binding.tvStarCount.text = repoDetails.starCount.toFormattedCount()

            binding.root.setSafeOnClickListener {
                repoDetails.url.ifValid{ url->
                    binding.root.context.openInBrowser(url)
                }
            }
        }
    }

    fun submitList(newList: List<UserRepoDetails>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = userRepoList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                userRepoList[oldItemPosition].id == newList[newItemPosition].id
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                userRepoList[oldItemPosition] == newList[newItemPosition]
        })
        userRepoList.clear()
        userRepoList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserRepoViewHolder (
        RepoItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount(): Int {
        return userRepoList.size
    }

    override fun onBindViewHolder(holder: UserRepoViewHolder, position: Int) {
        holder.bind(userRepoList[position])
    }

}