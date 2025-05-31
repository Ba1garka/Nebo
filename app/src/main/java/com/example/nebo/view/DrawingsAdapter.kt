package com.example.nebo.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nebo.R
import com.example.nebo.databinding.ItemDrawingBinding
import com.example.nebo.model.DrawingResponse

class DrawingsAdapter : ListAdapter<DrawingResponse, DrawingsAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemDrawingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(drawing: DrawingResponse) {
            binding.titleTextView.text = drawing.title

            val imageUrl = drawing.filePath?.let { url ->
                url.substringBefore('?')
                    .replace("http://localhost", "http://10.0.2.2")
            }

            Glide.with(binding.root)
                .load(imageUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_clear)
                .into(binding.imageView)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DrawingResponse>() {
        override fun areItemsTheSame(oldItem: DrawingResponse, newItem: DrawingResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DrawingResponse, newItem: DrawingResponse): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDrawingBinding.inflate(
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