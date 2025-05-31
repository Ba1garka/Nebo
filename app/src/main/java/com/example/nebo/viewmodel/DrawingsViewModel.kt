package com.example.nebo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.nebo.config.ApiService
import com.example.nebo.model.DrawingResponse
import kotlinx.coroutines.launch

class DrawingsViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiService.create("http://10.0.2.2:8082/", application)

    private val _drawingsResult = MutableLiveData<Result<List<DrawingResponse>>>()
    val drawingsResult: LiveData<Result<List<DrawingResponse>>> = _drawingsResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadUserDrawings() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = apiService.getUserDrawings()
                if (response.isSuccessful) {
                    response.body()?.let { drawings ->
                        _drawingsResult.value = Result.success(drawings)
                    } ?: run {
                        _drawingsResult.value = Result.failure(Exception("Empty response body"))
                    }
                } else {
                    val error = when (response.code()) {
                        401 -> "Unauthorized - please login again"
                        500 -> "Server error occurred"
                        else -> "Error fetching drawings (code ${response.code()})"
                    }
                    _drawingsResult.value = Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                _drawingsResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}