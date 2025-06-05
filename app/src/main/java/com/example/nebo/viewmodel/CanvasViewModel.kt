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
    private val apiService = ApiService.create( application)

    private val upload = MutableLiveData<Result<DrawingResponse>>()
    val uploadResult: LiveData<Result<DrawingResponse>> = upload

    private val load = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = load

    fun uploadDrawing(filePart: MultipartBody.Part) {
        load.value = true
        viewModelScope.launch {
            try {
                val response = apiService.uploadDrawing(filePart)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        upload.value = Result.success(body)
                    } ?: run {
                        upload.value = Result.failure(Exception("Empty response body"))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    upload.value = Result.failure(Exception("Server error: $errorMsg"))
                }
            } catch (e: Exception) {
                upload.value = Result.failure(e)
            } finally {
                load.value = false
            }
        }
    }
}