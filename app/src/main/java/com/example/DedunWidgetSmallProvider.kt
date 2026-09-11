package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews

class DedunWidgetSmallProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAppWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAppWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val prefs = context.getSharedPreferences(DedunWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
            val isInitialized = prefs.getBoolean("is_initialized", false)
            val balance = prefs.getFloat("balance", 0f).toDouble()
            val safeToSpend = prefs.getFloat("safe_to_spend", 0f).toDouble()

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_small)

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)
                views.setOnClickPendingIntent(R.id.widget_small_root, pendingIntent)

                if (!isInitialized) {
                    views.setViewVisibility(R.id.widget_small_content_container, View.GONE)
                    views.setViewVisibility(R.id.widget_small_fallback_container, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_small_fallback_container, View.GONE)
                    views.setViewVisibility(R.id.widget_small_content_container, View.VISIBLE)

                    views.setTextViewText(R.id.tv_small_safe_to_spend, DedunWidgetProvider.formatLkr(safeToSpend))
                    views.setTextViewText(R.id.tv_small_balance, DedunWidgetProvider.formatLkr(balance))
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, DedunWidgetSmallProvider::class.java))
            if (ids.isNotEmpty()) {
                updateAppWidgets(context, appWidgetManager, ids)
            }
        }
    }
}
