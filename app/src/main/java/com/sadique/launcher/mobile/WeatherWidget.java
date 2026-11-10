package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

/** 6. Weather — medium 4x2: big current temperature, condition, high/low and details. No clock. Tap opens the Weather screen. */
public class WeatherWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    static void update(Context context, AppWidgetManager manager, int[] ids) {
        if (ids.length == 0) return;
        for (int id : ids) {
            RemoteViews views = WidgetKit.baseWeather(context, R.layout.widget_weather, id);
            WidgetKit.fillWeather(context, views, null);
            manager.updateAppWidget(id, views);
        }
        WidgetKit.fetchWeather(context, (summary, highLow, json) -> {
            for (int id : ids) {
                RemoteViews views = WidgetKit.baseWeather(context, R.layout.widget_weather, id);
                WidgetKit.fillWeather(context, views, json);
                manager.updateAppWidget(id, views);
            }
        });
    }
}
