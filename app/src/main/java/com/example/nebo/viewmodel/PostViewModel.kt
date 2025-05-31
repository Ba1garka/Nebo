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
    private val apiService = ApiService.create("http://10.0.2.2:8082/", application)

    private val _createPostResult = MutableLiveData<Result<PostResponse>>()
    val createPostResult: LiveData<Result<PostResponse>> = _createPostResult

    private val _posts = MutableLiveData<Result<List<PostResponse>>>()
    val posts: LiveData<Result<List<PostResponse>>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _likeActionResult = MutableLiveData<Result<PostResponse>>()
    val likeActionResult: LiveData<Result<PostResponse>> = _likeActionResult

    fun createPost(drawingId: Long, description: String) {
        viewModelScope.launch {
            try {
                val response = apiService.createPost(CreatePostRequest(drawingId, description))
                if (response.isSuccessful) {
                    _createPostResult.value = Result.success(response.body()!!)
                } else {
                    _createPostResult.value = Result.failure(Exception("Failed to create post"))
                }
            } catch (e: Exception) {
                _createPostResult.value = Result.failure(e)
            }
        }
    }

    fun loadPosts() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = apiService.getAllPosts()
                if (response.isSuccessful) {
                    _posts.value = Result.success(response.body()!!)
                } else {
                    _posts.value = Result.failure(Exception("Failed to load posts"))
                }
            } catch (e: Exception) {
                _posts.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLike(postId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                val response = if (isLiked) {
                    apiService.likePost(postId)
                } else {
                    apiService.unlikePost(postId)
                }

                if (response.isSuccessful) {
                    _likeActionResult.value = Result.success(response.body()!!)
                    loadPosts() // Обновляем список постов
                } else {
                    _likeActionResult.value = Result.failure(Exception("Failed to update like status"))
                }
            } catch (e: Exception) {
                _likeActionResult.value = Result.failure(e)
            }
        }
    }
}