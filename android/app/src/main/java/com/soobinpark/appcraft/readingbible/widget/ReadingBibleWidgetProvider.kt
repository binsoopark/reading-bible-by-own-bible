package com.soobinpark.appcraft.readingbible.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.soobinpark.appcraft.readingbible.MainActivity
import com.soobinpark.appcraft.readingbible.R

class ReadingBibleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { widgetId ->
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_reading_bible).apply {
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                setTextViewText(R.id.widget_title, context.getString(R.string.app_name))
                setTextViewText(R.id.widget_body, context.getString(R.string.widget_body))
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
