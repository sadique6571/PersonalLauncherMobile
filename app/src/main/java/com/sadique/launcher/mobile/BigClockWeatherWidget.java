package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

/** 8. Big Clock + Weather — large 4x4: big analog clock, digital time, date, live weather and details. Tap opens the Weather screen. */
public class BigClockWeatherWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    static void update(Context context, AppWidgetManager manager, int[] ids) {
        if (ids.length == 0) return;
        String json = WidgetKit.cachedJson(context);
        for (int id : ids) {
            RemoteViews views = WidgetKit.baseWeather(context, R.layout.widget_big_clock_weather, id);
            fill(context, views, json);
            manager.updateAppWidget(id, views);
        }
        WidgetKit.fetchWeather(context, (summary, highLow, fresh) -> {
            for (int id : ids) {
                RemoteViews views = WidgetKit.baseWeather(context, R.layout.widget_big_clock_weather, id);
                fill(context, views, fresh);
                manager.updateAppWidget(id, views);
            }
        });
    }

    private static void fill(Context context, RemoteViews views, String json) {
        String shown = json != null ? json : WidgetKit.cachedJson(context);
        views.setTextViewText(R.id.widget_date, WidgetKit.dateLine());
        views.setTextViewText(R.id.widget_weather, WidgetKit.weatherText(null, WidgetKit.cachedWeather(context)));
        views.setTextViewText(R.id.widget_highlow, WidgetKit.highLowLine(shown));
        views.setTextViewText(R.id.widget_feels, WidgetKit.feelsLine(shown));
        views.setTextViewText(R.id.widget_humidity, WidgetKit.humidityLine(shown));
        views.setTextViewText(R.id.widget_wind, WidgetKit.windLine(shown));
        views.setTextViewText(R.id.widget_rain, WidgetKit.rainLine(shown));
        views.setTextViewText(R.id.widget_uv, WidgetKit.uvLine(shown));
        views.setTextViewText(R.id.widget_pressure, WidgetKit.pressureLine(shown));
        views.setTextViewText(R.id.widget_cloud, WidgetKit.cloudLine(shown));
        WidgetKit.fillSun(context, views, shown);
    }
}
