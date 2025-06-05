package com.example.nebo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.nebo.config.ApiService
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AvatarViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiService.create( application)

    private val upload = MutableLiveData<Result<String>>()
    val uploadResult: LiveData<Result<String>> = upload

    fun uploadAvatar(file: File) {
        viewModelScope.launch {
            try {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = apiService.uploadAvatar(part)
                if (response.isSuccessful) {
                    val avatarUrl = response.body()?.get("avatarUrl")
                    upload.value = Result.success(avatarUrl ?: "")
                } else {
                    upload.value = Result.failure(Exception("Failed to upload avatar"))
                }
            } catch (e: Exception) {
                upload.value = Result.failure(e)
            }
        }
    }
}