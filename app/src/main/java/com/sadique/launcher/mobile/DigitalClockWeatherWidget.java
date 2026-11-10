package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

/** 4. Digital Clock + Weather — medium 4x2: big digits (self-updating TextClock), date, live weather and details. Tap opens the Weather screen. */
public class DigitalClockWeatherWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    static void update(Context context, AppWidgetManager manager, int[] ids) {
        if (ids.length == 0) return;
        String json = WidgetKit.cachedJson(context);
        for (int id : ids) {
            RemoteViews views = WidgetKit.baseWeather(context, R.layout.widget_digital_clock_weather, id);
            views.setTextViewText(R.id.widget_date, WidgetKit.dateLine());
            views.setTextViewText(R.id.widget_weather, WidgetKit.weatherText(null, WidgetKit.cachedWeather(context)));
            views.setTextViewText(R.id.widget_highlow, WidgetKit.highLowLine(json));
            views.setTextViewText(R.id.widget_feels, WidgetKit.feelsLine(json));
            views.setTextViewText(R.id.widget_wind, WidgetKit.windLine(json));
            WidgetKit.fillSun(context, views, json);
            manager.updateAppWidget(id, views);
        }
        WidgetKit.fetchWeather(context, (summary, highLow, fresh) -> {
            String shown = fresh != null ? fresh : WidgetKit.cachedJson(context);
            for (int id : ids) {
                RemoteViews views = WidgetKit.baseWeather(context, R.layout.widget_digital_clock_weather, id);
                views.setTextViewText(R.id.widget_date, WidgetKit.dateLine());
                views.setTextViewText(R.id.widget_weather, WidgetKit.weatherText(summary, WidgetKit.cachedWeather(context)));
                views.setTextViewText(R.id.widget_highlow, WidgetKit.highLowLine(shown));
                views.setTextViewText(R.id.widget_feels, WidgetKit.feelsLine(shown));
                views.setTextViewText(R.id.widget_wind, WidgetKit.windLine(shown));
                WidgetKit.fillSun(context, views, shown);
                manager.updateAppWidget(id, views);
            }
        });
    }
}
