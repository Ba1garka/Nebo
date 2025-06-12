package com.example.nebo.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.load.engine.Resource
import com.example.nebo.config.ApiService
import com.example.nebo.model.SendDto
import com.example.nebo.view.WidgetWorker
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
                    loadReceivedSends(application)
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

    private val widget = MutableLiveData<String?>()
    val widgetResult: LiveData<String?> = widget

    fun loadReceivedSends(context: Context) {
        viewModelScope.launch {
            try {
                val response = apiService.getReceivedSends()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    val reversedList = list.reversed()
                    showSends.value = Result.success(reversedList)
                    val latestSend = reversedList.firstOrNull()
                    val imageUrl = latestSend?.drawingPath?.substringBefore('?')
                        ?.replace("http://localhost", "http://10.0.2.2")
                    widget.value = imageUrl
                    if (imageUrl != null) {
                        val inputData = Data.Builder().putString("image_url", imageUrl).build()
                        val workRequest = OneTimeWorkRequestBuilder<WidgetWorker>().setInputData(inputData).build()
                        //WorkManager.getInstance(context).enqueue(workRequest)
                        WorkManager.getInstance(context.applicationContext).enqueue(workRequest)
                    }

                } else {
                    showSends.value = Result.failure(Exception("Ошибка загрузки: ${response.message()}"))
                }
            } catch (e: Exception) {
                showSends.value = Result.failure(Exception("Ошибка сети: ${e.message}"))
            }
        }
    }
}