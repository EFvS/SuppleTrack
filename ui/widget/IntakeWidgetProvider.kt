package com.efvs.suppletrack.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.efvs.suppletrack.MainActivity
import com.efvs.suppletrack.R

class IntakeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val intent = Intent(context, IntakeWidgetService::class.java)
            val views = RemoteViews(context.packageName, R.layout.widget_intake_checklist)
            views.setRemoteAdapter(R.id.widget_list, intent)

            val activityIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}