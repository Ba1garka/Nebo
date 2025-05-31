package com.example.nebo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.load.engine.Resource

import com.example.nebo.config.ApiService
import com.example.nebo.model.DrawingResponse
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class CanvasViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiService.create("http://10.0.2.2:8082/", getApplication())

    private val _uploadResult = MutableLiveData<Result<DrawingResponse>>()
    val uploadResult: LiveData<Result<DrawingResponse>> = _uploadResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun uploadDrawing(filePart: MultipartBody.Part) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = apiService.uploadDrawing(filePart)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _uploadResult.value = Result.success(body)
                    } ?: run {
                        _uploadResult.value = Result.failure(Exception("Empty response body"))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    _uploadResult.value = Result.failure(Exception("Server error: $errorMsg"))
                }
            } catch (e: Exception) {
                _uploadResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}