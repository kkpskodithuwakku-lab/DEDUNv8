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
import java.util.Locale

class DedunWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAppWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        const val PREFS_NAME = "widget_prefs"

        fun updateAppWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isInitialized = prefs.getBoolean("is_initialized", false)
            val balance = prefs.getFloat("balance", 0f).toDouble()
            val safeToSpend = prefs.getFloat("safe_to_spend", 0f).toDouble()
            val streak = prefs.getInt("streak", 0)
            val daysToAllowance = prefs.getInt("days_to_allowance", 0)
            val dailySpent = prefs.getFloat("daily_spent", 0f).toDouble()
            val dailyLimit = prefs.getFloat("daily_limit", 2500f).toDouble()

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_large)

                // Tapping anywhere on the widget launches MainActivity
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                if (!isInitialized) {
                    views.setViewVisibility(R.id.widget_content_container, View.GONE)
                    views.setViewVisibility(R.id.widget_fallback_container, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_fallback_container, View.GONE)
                    views.setViewVisibility(R.id.widget_content_container, View.VISIBLE)

                    // Safe to spend (prominent)
                    views.setTextViewText(R.id.tv_safe_to_spend, formatLkr(safeToSpend))
                    if (balance <= 0 || (safeToSpend > 0 && dailySpent > safeToSpend)) {
                        views.setTextColor(R.id.tv_safe_to_spend, 0xFFD95F5F.toInt())
                    } else {
                        views.setTextColor(R.id.tv_safe_to_spend, 0xFFC9A66B.toInt())
                    }

                    // Net Balance
                    views.setTextViewText(R.id.tv_balance, formatLkr(balance))

                    // Streak count
                    val streakText = "🔥 $streak"
                    views.setTextViewText(R.id.tv_streak, streakText)

                    // Days to allowance
                    val daysText = when {
                        daysToAllowance <= 0 -> "Due"
                        daysToAllowance == 1 -> "1d left"
                        else -> "${daysToAllowance}d left"
                    }
                    views.setTextViewText(R.id.tv_allowance_days, "• $daysText")

                    // Daily spend progress bar
                    val limit = if (dailyLimit > 0) dailyLimit else 2500.0
                    val pct = Math.min(100, Math.max(0, ((dailySpent / limit) * 100).toInt()))
                    views.setProgressBar(R.id.widget_progress_bar, 100, pct, false)
                    views.setTextViewText(R.id.tv_progress_pct, "$pct% used")
                    views.setTextViewText(R.id.tv_daily_spend_summary, "Spent: ${formatLkr(dailySpent)} / ${formatLkr(limit)}")
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, DedunWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                updateAppWidgets(context, appWidgetManager, ids)
            }
        }

        fun formatLkr(amount: Double): String {
            val isNegative = amount < 0
            val absVal = Math.abs(amount)
            val formatted = String.format(Locale.US, "%,.2f", absVal)
            return if (isNegative) "-Rs. $formatted" else "Rs. $formatted"
        }
    }
}
