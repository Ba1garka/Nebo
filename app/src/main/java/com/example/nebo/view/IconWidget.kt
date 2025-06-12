package com.example.nebo.view

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build

import android.util.Log
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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

                val views = RemoteViews(context.packageName, R.layout.widget_icon)

                if (imageUrl != null) {
                    try {
                        Glide.with(context)
                            .asBitmap()
                            .load(imageUrl)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    views.setImageViewBitmap(R.id.widget_icon, resource)
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    views.setImageViewResource(R.id.widget_icon, android.R.color.transparent)
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                }
                            })
                    } catch (e: Exception) {
                        views.setImageViewResource(R.id.widget_icon, android.R.color.transparent)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } else {
                    views.setImageViewResource(R.id.widget_icon, android.R.color.transparent)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }


                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}