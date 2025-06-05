package com.example.nebo.view

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nebo.databinding.ItemPostBinding
import com.example.nebo.model.PostResponse

class PostsAdapter(private val onLikeClick: (Long, Boolean) -> Unit) : ListAdapter<PostResponse, PostsAdapter.ViewHolder>(PostDiffCallback()) {

    inner class ViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostResponse) {
            with(binding) {
                authorName.text = post.authorName
                postDescription.text = post.description
                likesCount.text = post.likesCount.toString()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    postDate.text = post.formatCreatedAt()
                    Log.i("DATE","date:"+ post.formatCreatedAt())
                }

                val postUrl = post.drawingUrl?.substringBefore('?')?.replace("http://localhost", "http://10.0.2.2")
                val avatar = post.authorAvatarUrl?.substringBefore('?')?.replace("http://localhost", "http://10.0.2.2")

                Glide.with(root)
                    .load(avatar)
                    .circleCrop()
                    .into(authorAvatar)

                Glide.with(root)
                    .load(postUrl)
                    .into(postImage)

                likeButton.apply {
                    isSelected = post.isLikedByCurrentUser
                    setOnClickListener {
                        val newLikeState = !isSelected
                        Log.i("LIKE", "islikebyUser" + post.isLikedByCurrentUser.toString() + "newLikeState" + newLikeState.toString())
                        isSelected = newLikeState
                        onLikeClick(post.id, newLikeState)
                        likesCount.text = post.likesCount.toString()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<PostResponse>() {
    override fun areItemsTheSame(oldItem: PostResponse, newItem: PostResponse): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PostResponse, newItem: PostResponse): Boolean {
        return oldItem == newItem
    }
}