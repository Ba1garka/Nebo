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
                        drawings.value = Result.failure(Exception("Empty body"))
                    }
                } else {
                    val error = "Error fetching drawings code ${response.code()}"
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

    fun delete(drawingId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteDrawing(drawingId)
                if (response.isSuccessful) {
                    delete.value = Result.success(Unit)
                    loadUserDrawings()
                } else {
                    val error = "Error deleting drawing code ${response.code()}"
                    delete.value = Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                delete.value = Result.failure(e)
            }
        }
    }
}