package com.example.nebo.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nebo.R
import com.example.nebo.databinding.ItemSendBinding
import com.example.nebo.model.SendDto


class SendAdapter(
    private val onItemClick: (SendDto) -> Unit
) : ListAdapter<SendDto, SendAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemSendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(send: SendDto) {
            with(binding) {

                val url = send.senderAvatarPath?.substringBefore('?')?.replace("http://localhost", "http://10.0.2.2")

                Glide.with(root)
                    .load(url)
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(authorAvatar)

                authorName.text = send.senderName
                postDate.text = send.formatCreatedAt()

                val imageUrl = send.drawingPath.substringBefore('?').replace("http://localhost", "http://10.0.2.2")

                Glide.with(root)
                    .load(imageUrl)
                    .into(postImage)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SendDto>() {
        override fun areItemsTheSame(oldItem: SendDto, newItem: SendDto) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SendDto, newItem: SendDto) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        println("Binding item at $position: $item")
        holder.bind(getItem(position))
    }
}