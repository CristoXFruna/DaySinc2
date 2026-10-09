package com.example.daysinc.models;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.example.daysinc.R;

public class StreakWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences("DaySincPrefs", Context.MODE_PRIVATE);
        int currentStreak = prefs.getInt("user_streak", 0);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_streak);

            views.setTextViewText(R.id.tvWidgetStreak, currentStreak + " Días");

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}