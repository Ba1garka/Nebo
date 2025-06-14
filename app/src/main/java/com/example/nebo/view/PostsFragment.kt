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


class PostsFragment : Fragment() {
    private var binding: FragmentPostsBinding? = null
    private val bind get() = binding!!
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
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawingsViewModel.loadUserDrawings()

        bind.fabAddPost.setOnClickListener {
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
        bind.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PostsFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
            )
        }
    }

    private fun setupObservers() {
        viewModel.postsResult.observe(viewLifecycleOwner) { result ->
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
                    showE(result.exceptionOrNull()?.message ?: "Error loading posts")
                }
            }
            bind.swipeRefresh.isRefreshing = false
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            bind.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

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

        drawingsViewModel.drawingsResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                val drawings = result.getOrNull()!!
                if (drawings.isEmpty()) {
                    Toast.makeText(requireContext(), "You have no drawings to post", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.likeActionResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isFailure -> {
                    showE("Like action failed: ${result.exceptionOrNull()?.message}")
                    viewModel.loadPosts()
                }
                result.isSuccess -> {
                    viewModel.loadPosts()
                }
            }
        }

    }

    private fun showEmptyState(show: Boolean) {
        bind.postsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showE(message: String) {
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
                    ) { _, which ->
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}