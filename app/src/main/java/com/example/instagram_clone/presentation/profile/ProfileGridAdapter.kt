package com.example.instagram_clone.presentation.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.instagram_clone.R
import com.example.instagram_clone.domain.model.Post
import com.example.instagram_clone.databinding.ItemProfileGridBinding

class ProfileGridAdapter(
    private val onPostClick: (Post) -> Unit = {}
) : ListAdapter<Post, ProfileGridAdapter.GridViewHolder>(GridDiffCallback) {

    companion object {
        private val GridDiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemProfileGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val size = parent.context.resources.displayMetrics.widthPixels / 3
        binding.root.layoutParams = ViewGroup.LayoutParams(size, size)
        return GridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val post = getItem(position)
        Glide.with(holder.binding.root.context)
            .load(post.downloadUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder_image)
            .centerCrop()
            .into(holder.binding.gridImage)

        holder.binding.root.setOnClickListener { onPostClick(post) }
    }

    class GridViewHolder(val binding: ItemProfileGridBinding) : RecyclerView.ViewHolder(binding.root)
}
