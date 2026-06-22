package com.tidely.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tidely.R
import com.tidely.TidelyApplication
import com.tidely.data.model.TidalEvent
import com.tidely.data.model.TideType
import com.tidely.ui.main.MainActivity
import com.tidely.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TideWidgetProvider : AppWidgetProvider() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.UK)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val app = context.applicationContext as TidelyApplication
                val preferencesManager = PreferencesManager(context)

                val stationId = preferencesManager.selectedStationId
                if (stationId == null) {
                    updateWidgetWithNoData(context, appWidgetManager, appWidgetId)
                    return@launch
                }

                // Fetch fresh data if needed
                if (app.tideRepository.needsRefresh(stationId)) {
                    app.tideRepository.fetchAndCacheTidalEvents(stationId)
                }

                // Get next tidal events
                val events = app.tideRepository.getNextTidalEvents(stationId, limit = 4)

                if (events.isEmpty()) {
                    updateWidgetWithNoData(context, appWidgetManager, appWidgetId)
                    return@launch
                }

                val now = Date()
                val nextHigh = events.firstOrNull { it.eventType == TideType.HIGH && it.dateTime.after(now) }
                val nextLow = events.firstOrNull { it.eventType == TideType.LOW && it.dateTime.after(now) }

                if (nextHigh != null && nextLow != null) {
                    updateWidgetWithData(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        nextHigh,
                        nextLow
                    )

                    // Schedule next update
                    scheduleNextUpdate(context, events)
                } else {
                    updateWidgetWithNoData(context, appWidgetManager, appWidgetId)
                }
            } finally {
                scope.cancel()
            }
        }
    }

    private fun updateWidgetWithData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        nextHigh: TidalEvent,
        nextLow: TidalEvent
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_tide_compact)

        // Set tide data
        views.setTextViewText(R.id.tvHighTime, timeFormat.format(nextHigh.dateTime))
        views.setTextViewText(R.id.tvHighHeight, String.format("%.1f m", nextHigh.height))
        views.setTextViewText(R.id.tvLowTime, timeFormat.format(nextLow.dateTime))
        views.setTextViewText(R.id.tvLowHeight, String.format("%.1f m", nextLow.height))

        // Click to open app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_tide_compact, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateWidgetWithNoData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_tide_compact)
        views.setTextViewText(R.id.tvHighTime, "--:--")
        views.setTextViewText(R.id.tvHighHeight, "- m")
        views.setTextViewText(R.id.tvLowTime, "--:--")
        views.setTextViewText(R.id.tvLowHeight, "- m")

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun scheduleNextUpdate(context: Context, events: List<TidalEvent>) {
        val now = Date()
        val nextEvent = events.firstOrNull { it.dateTime.after(now) } ?: return

        // Schedule update for 1 minute after the next tide event
        val delayMillis = nextEvent.dateTime.time - now.time + (60 * 1000)

        if (delayMillis > 0) {
            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget added - schedule initial update
        val intent = Intent(context, TideWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, TideWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        context.sendBroadcast(intent)
    }

    override fun onDisabled(context: Context) {
        // Last widget removed - cancel scheduled updates
        WorkManager.getInstance(context).cancelAllWorkByTag("widget_update")
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, TideWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TideWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
        }
    }
}
