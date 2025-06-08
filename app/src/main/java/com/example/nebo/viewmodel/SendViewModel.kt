package com.example.nebo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.load.engine.Resource
import com.example.nebo.config.ApiService
import com.example.nebo.model.SendDto
import kotlinx.coroutines.launch

class SendViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiService.create( application)

    private val send = MutableLiveData<Result<Unit>>()
    val sendResult: LiveData<Result<Unit>> = send

    fun sendDrawing(drawingId: Long, recipientName: String) {
        viewModelScope.launch {
            try {
                val response = apiService.sendDrawing(drawingId, recipientName)

                if (response.isSuccessful) {
                    send.value = Result.success(Unit)
                } else {
                    send.value = Result.failure(Exception("Ошибка отправки: ${response.message()}"))
                }
            } catch (e: Exception) {
                send.value = Result.failure(Exception("Ошибка сети: ${e.message}"))
            }
        }
    }

    private val showSends = MutableLiveData<Result<List<SendDto>>>()
    val showSendsResult: LiveData<Result<List<SendDto>>> =showSends

    fun loadReceivedSends() {
        viewModelScope.launch {
            try {
                val response = apiService.getReceivedSends()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    val reversedList = list.reversed()
                    showSends.value = Result.success(reversedList)

                } else {
                    showSends.value = Result.failure(Exception("Ошибка загрузки: ${response.message()}"))
                }
            } catch (e: Exception) {
                showSends.value = Result.failure(Exception("Ошибка сети: ${e.message}"))
            }
        }
    }
}