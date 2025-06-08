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
    private val apiService = ApiService.create( application)

    private val drawings = MutableLiveData<Result<List<DrawingResponse>>>()
    val drawingsResult: LiveData<Result<List<DrawingResponse>>> = drawings

    private val load= MutableLiveData<Boolean>()

    fun loadUserDrawings() {
        load.value = true
        viewModelScope.launch {
            try {
                val response = apiService.getUserDrawings()
                if (response.isSuccessful) {
                    response.body()?.let { draw ->
                        drawings.value = Result.success(draw)
                    } ?: run {
                        drawings.value = Result.failure(Exception("Empty response body"))
                    }
                } else {
                    val error = when (response.code()) {
                        401 -> "Unauthorized - please login again"
                        500 -> "Server error occurred"
                        else -> "Error fetching drawings (code ${response.code()})"
                    }
                    drawings.value = Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                drawings.value = Result.failure(e)
            } finally {
                load.value = false
            }
        }
    }

    private val delete = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = delete

    fun delete(drawingId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteDrawing(drawingId)
                if (response.isSuccessful) {
                    delete.value = Result.success(Unit)
                    loadUserDrawings()
                } else {
                    val error = when (response.code()) {
                        404 -> "Drawing not found"
                        403 -> "You don't have permission to delete this drawing"
                        else -> "Error deleting drawing (code ${response.code()})"
                    }
                    delete.value = Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                delete.value = Result.failure(e)
            }
        }
    }
}