package com.example.instagram_clone.presentation.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.instagram_clone.domain.model.User
import com.example.instagram_clone.databinding.ItemUserRowBinding

class UserListAdapter(
    private val onUserClick: (User) -> Unit = {},
    private val showActionButton: Boolean = false,
    private val showRequestButtons: Boolean = false,
    private val onActionClick: (User) -> Unit = {},
    private val onAcceptClick: (User) -> Unit = {},
    private val onRejectClick: (User) -> Unit = {}
) : ListAdapter<User, UserListAdapter.UserViewHolder>(UserDiffCallback()) {

    class UserViewHolder(val binding: ItemUserRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(ItemUserRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        with(holder.binding) {
            userName.text = user.username.ifEmpty { user.email.substringBefore("@") }
            userFullName.text = user.displayName.ifEmpty { user.email }
            userInitial.text = (user.username.ifEmpty { user.email }).firstOrNull()?.uppercase() ?: "?"

            if (user.profileImageUrl.isNotEmpty()) {
                userProfileImage.visibility = android.view.View.VISIBLE
                com.bumptech.glide.Glide.with(root.context).load(user.profileImageUrl).centerCrop().into(userProfileImage)
            } else {
                userProfileImage.visibility = android.view.View.GONE
            }

            root.setOnClickListener { onUserClick(user) }

            if (showActionButton) {
                actionButton.visibility = View.VISIBLE
                actionButton.setOnClickListener { onActionClick(user) }
            }
            if (showRequestButtons) {
                acceptButton.visibility = View.VISIBLE
                rejectButton.visibility = View.VISIBLE
                acceptButton.setOnClickListener { onAcceptClick(user) }
                rejectButton.setOnClickListener { onRejectClick(user) }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
