package com.example.nebo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.nebo.config.ApiService
import com.example.nebo.model.LoginRequest
import com.example.nebo.model.RegisterRequest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiService.create("http://10.0.2.2:8082/", getApplication())

    private val _loginResult = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> = _loginResult

    private val _registerResult = MutableLiveData<Result<Unit>>()
    val registerResult: LiveData<Result<Unit>> = _registerResult

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = apiService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    _loginResult.value = Result.success(Unit)
                } else {
                    val error = response.errorBody()?.string()?.let {
                        JSONObject(it).getString("error") ?: "Login failed"
                    } ?: "Login failed"
                    _loginResult.value = Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                _loginResult.value = Result.failure(e)
            }
        }
    }

    fun register(email: String, password: String, name: String, birthDate: LocalDate) {
        viewModelScope.launch {
            try {
                val response = apiService.register(
                    RegisterRequest(email, password, name, birthDate.toString())
                )
                if (response.isSuccessful) {
                    _registerResult.value = Result.success(Unit)
                } else {
                    val error = response.errorBody()?.string()?.let {
                        JSONObject(it).getString("error") ?: "Registration failed"
                    } ?: "Registration failed"
                    _registerResult.value = Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                _registerResult.value = Result.failure(e)
            }
        }
    }
}