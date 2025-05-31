package com.example.nebo.view

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nebo.viewmodel.DrawingsViewModel
import com.example.nebo.databinding.FragmentPostsBinding
import com.example.nebo.viewmodel.PostViewModel
import com.google.android.material.snackbar.Snackbar


class PostsFragment : Fragment() {
    private lateinit var binding: FragmentPostsBinding
    private val viewModel: PostViewModel by viewModels()
    private val drawingsViewModel: DrawingsViewModel by viewModels()

    private var selectedDrawingId: Long? = null
    private lateinit var adapter: PostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Загружаем рисунки пользователя
        drawingsViewModel.loadUserDrawings()

        // Кнопка добавления нового поста
        binding.fabAddPost.setOnClickListener {
            showDrawingSelectionDialog()
        }

        setupRecyclerView()
        setupObservers()
        viewModel.loadPosts()
    }

    private fun setupRecyclerView() {
        adapter = PostsAdapter { postId, isLiked ->
            viewModel.toggleLike(postId, isLiked)
        }
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PostsFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
            )
        }
    }

    private fun setupObservers() {
        //наблюдаем за выводом постов
        viewModel.posts.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    val posts = result.getOrNull()!!
                    if (posts.isEmpty()) {
                        showEmptyState(true)
                    } else {
                        showEmptyState(false)
                        adapter.submitList(posts)
                    }
                }
                result.isFailure -> {
                    showError(result.exceptionOrNull()?.message ?: "Error loading posts")
                }
            }
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Наблюдаем за результатом создания поста
        viewModel.createPostResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    Toast.makeText(requireContext(), "Post created successfully", Toast.LENGTH_SHORT).show()
                    viewModel.loadPosts()
                }
                result.isFailure -> {
                    Toast.makeText(requireContext(), "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Наблюдаем за списком рисунков
        drawingsViewModel.drawingsResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                val drawings = result.getOrNull()!!
                if (drawings.isEmpty()) {
                    Toast.makeText(requireContext(), "You have no drawings to post", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //наблюдаем за лайками
        viewModel.likeActionResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isFailure -> {
                    showError("Like action failed: ${result.exceptionOrNull()?.message}")
                    viewModel.loadPosts()
                }
                result.isSuccess -> {
                    viewModel.loadPosts()
                }
            }
        }

    }

    private fun showEmptyState(show: Boolean) {
        //binding.emptyStateView.visibility = if (show) View.VISIBLE else View.GONE
        binding.postsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Log.e("POSTS", message)
    }

    private fun showDrawingSelectionDialog() {
        drawingsViewModel.drawingsResult.value?.let { result ->
            if (result.isSuccess) {
                val drawings = result.getOrNull()!!
                if (drawings.isEmpty()) {
                    Toast.makeText(requireContext(), "You have no drawings to post", Toast.LENGTH_SHORT).show()
                    return
                }

                val dialog = AlertDialog.Builder(requireContext())
                    .setTitle("Select a drawing to post")
                    .setAdapter(
                        ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1,
                        drawings.map { it.title })
                    ) { dialog, which ->
                        selectedDrawingId = drawings[which].id
                        showDescriptionInputDialog()
                    }
                    .setNegativeButton("Cancel", null)
                    .create()

                dialog.show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "Loading drawings...", Toast.LENGTH_SHORT).show()
            drawingsViewModel.loadUserDrawings()
        }
    }

    private fun showDescriptionInputDialog() {
        val inputEditText = EditText(requireContext()).apply {
            hint = "Enter description"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add description")
            .setView(inputEditText)
            .setPositiveButton("Post") { _, _ ->
                val description = inputEditText.text.toString()
                if (description.isNotBlank() && selectedDrawingId != null) {
                    viewModel.createPost(selectedDrawingId!!, description)
                } else {
                    Toast.makeText(requireContext(), "Description cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}