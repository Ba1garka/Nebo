package com.example.nebo.view

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.example.nebo.R

class IconWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray //все активные экземпляры виджета
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val prefs = context.getSharedPreferences("WidgetPrefs", MODE_PRIVATE)
            val imageUrl = prefs.getString("widget_image_url", null)
            Log.i("WIDGET", "imageurl:" + imageUrl)
            appWidgetIds.forEach { appWidgetId ->

                val views = RemoteViews(context.packageName, R.layout.widget_icon).apply {

                    if(imageUrl != null) {
                        imageUrl.let { url ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                try {
                                    val bitmap = Glide.with(context)
                                        .asBitmap()
                                        .load(url)
                                        .submit()
                                        .get()
                                    setImageViewBitmap(R.id.widget_icon, bitmap)
                                } catch (e: Exception) {
                                    setImageViewResource(
                                        R.id.widget_icon,
                                        android.R.color.transparent
                                    )
                                }
                            }
                        }
                    } else {
                        setImageViewResource(
                            R.id.widget_icon,
                            android.R.color.transparent
                        )
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}