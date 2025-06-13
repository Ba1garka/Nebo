package com.example.nebo.view

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.nebo.R
import com.example.nebo.config.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SendService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var notificationManager: NotificationManager
    private lateinit var apiService: ApiService

    //@SuppressLint("ForegroundServiceType", "NewApi")
    override fun onCreate() {
        super.onCreate()
        apiService = ApiService.create(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(123, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTask()
        return START_STICKY // Сервис перезапустится, если система его убьёт
    }

    private fun startTask() {
        serviceScope.launch {
            while (true) {
                try {
                    applicationContext?.let { ctx ->
                        loadReceivedSends(ctx)
                    }
                    delay(1000L)
                } catch (e: Exception) {
                    Log.e("SEND_SERVICE", "message" + e.message)
                }
            }
        }
    }

    private suspend fun loadReceivedSends(context: Context) {
        try {
            val response = apiService.getReceivedSends()
            if (response.isSuccessful) {
                val list = response.body() ?: emptyList()
                val reversedList = list.reversed()
                val latestSend = reversedList.firstOrNull()
                val imageUrl = latestSend?.drawingPath?.substringBefore('?')
                    ?.replace("http://localhost", "http://10.0.2.2")
                if (imageUrl != null) {
                    context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE).edit()
                        .putString("widget_image_url", imageUrl).apply()
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val widgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, IconWidget::class.java)
                    )
                    IconWidget.updateWidgets(context, appWidgetManager, widgetIds)
                }
            }
        } catch (e: Exception) {
            Log.e("SEND_SERVICE", "message2" + e.message)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "send_channel"

        // канал для версий 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Фоновая синхронизация", NotificationManager.IMPORTANCE_LOW).apply {// нет звука и вибрации
                description = "Обновление данных в фоне"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Идёт синхронизация")
            .setContentText("Приложение работает в фоне")
            .setSmallIcon(R.drawable.baseline_brush_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Нельзя свайпнуть
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun stop(context: Context) {
            context.stopService(Intent(context, SendService::class.java))
        }
    }
}