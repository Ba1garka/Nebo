package com.example.nebo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.nebo.config.ApiService
import com.example.nebo.model.UserResponse
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiService.create("http://10.0.2.2:8082/", getApplication())

    private val _userData = MutableLiveData<Result<UserResponse>>()
    val userData: LiveData<Result<UserResponse>> = _userData

    fun loadUserData() {
        viewModelScope.launch {
            try {
                val response = apiService.getCurrentUser()
                if (response.isSuccessful) {
                    val userResponse = UserResponse.fromJson(response.body()!!)
                    _userData.value = Result.success(userResponse)
                } else {
                    _userData.value = Result.failure(Exception("Failed to load user data"))
                }
            } catch (e: Exception) {
                _userData.value = Result.failure(e)
            }
        }
    }
}