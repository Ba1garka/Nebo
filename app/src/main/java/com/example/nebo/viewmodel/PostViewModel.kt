package com.example.nebo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.nebo.config.ApiService
import com.example.nebo.model.CreatePostRequest
import com.example.nebo.model.PostResponse
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiService.create( application)

    private val createPost = MutableLiveData<Result<PostResponse>>()
    val createPostResult: LiveData<Result<PostResponse>> = createPost

    fun createPost(drawingId: Long, description: String) {
        viewModelScope.launch {
            try {
                val response = apiService.createPost(CreatePostRequest(drawingId, description))
                if (response.isSuccessful) {
                    createPost.value = Result.success(response.body()!!)
                } else {
                    createPost.value = Result.failure(Exception("Failed to create post"))
                }
            } catch (e: Exception) {
                createPost.value = Result.failure(e)
            }
        }
    }

    private val posts = MutableLiveData<Result<List<PostResponse>>>()
    val postsResult: LiveData<Result<List<PostResponse>>> = posts

    private val load = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = load

    fun loadPosts() {
        load.value = true
        viewModelScope.launch {
            try {
                val response = apiService.getAllPosts()
                if (response.isSuccessful) {
                    posts.value = Result.success(response.body()!!)
                } else {
                    posts.value = Result.failure(Exception("Failed to load posts"))
                }
            } catch (e: Exception) {
                posts.value = Result.failure(e)
            } finally {
                load.value = false
            }
        }
    }

    private val likeAction = MutableLiveData<Result<PostResponse>>()
    val likeActionResult: LiveData<Result<PostResponse>> = likeAction

    fun toggleLike(postId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                val response = if (isLiked) {
                    apiService.likePost(postId)
                } else {
                    apiService.unlikePost(postId)
                }

                if (response.isSuccessful) {
                    likeAction.value = Result.success(response.body()!!)
                    loadPosts() // Обновляем список постов
                } else {
                    likeAction.value = Result.failure(Exception("Failed to update like status"))
                }
            } catch (e: Exception) {
                likeAction.value = Result.failure(e)
            }
        }
    }
}