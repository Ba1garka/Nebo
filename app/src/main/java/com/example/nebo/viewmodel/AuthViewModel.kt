package com.example.nebo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.load.engine.Resource
import com.example.nebo.config.ApiService
import com.example.nebo.model.LoginRequest
import com.example.nebo.model.RegisterRequest
import com.example.nebo.model.UserResponse
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiService.create(application)

    private val login = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> = login

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = apiService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    login.value = Result.success(Unit)
                } else {
                    val error = response.errorBody()?.string()?.let {
                        JSONObject(it).getString("error") ?: "Login failed"
                    } ?: "Login failed"
                    login.value = Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                login.value = Result.failure(e)
            }
        }
    }

    private val register = MutableLiveData<Result<Unit>>()
    val registerResult: LiveData<Result<Unit>> = register

    fun register(email: String, password: String, name: String, birthDate: LocalDate) {
        viewModelScope.launch {
            try {
                val response = apiService.register(
                    RegisterRequest(email, password, name, birthDate.toString())
                )
                if (response.isSuccessful) {
                    register.value = Result.success(Unit)
                } else {
                    val error = response.errorBody()?.string()?.let {
                        JSONObject(it).getString("error") ?: "Registration failed"
                    } ?: "Registration failed"
                    register.value = Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                register.value = Result.failure(e)
            }
        }
    }

    private val logout = MutableLiveData<Result<Unit>>()
    val logoutResult: LiveData<Result<Unit>> = logout

    fun logout(){
        viewModelScope.launch {
            try {
                val response = apiService.logout()
                if (response.isSuccessful) {
                    logout.value = Result.success(Unit)
                } else {
                    val error = response.errorBody()?.string()?.let {
                        JSONObject(it).getString("error") ?: "Logout failed"
                    } ?: "Logout failed"
                    logout.value = Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                logout.value = Result.failure(e)
            }
        }

    }

    private val userData = MutableLiveData<Result<UserResponse>>()
    val userDataResult: LiveData<Result<UserResponse>> = userData

    fun loadUserData() {
        viewModelScope.launch {
            try {
                val response = apiService.getCurrentUser()
                if (response.isSuccessful) {
                    val userResponse = UserResponse.fromJson(response.body()!!)
                    userData.value = Result.success(userResponse)
                } else {
                    userData.value = Result.failure(Exception("Failed to load user data"))
                }
            } catch (e: Exception) {
                userData.value = Result.failure(e)
            }
        }
    }
}