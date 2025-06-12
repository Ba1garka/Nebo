package com.example.nebo.view

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class WidgetWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            val imageUrl = inputData.getString("image_url") ?: return Result.failure()

            appContext.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE).edit()
                .putString("widget_image_url", imageUrl)
                .apply()

            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(appContext, IconWidget::class.java)
            )
            IconWidget.updateWidgets(appContext, appWidgetManager, widgetIds)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

