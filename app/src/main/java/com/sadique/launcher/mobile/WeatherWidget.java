package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.widget.RemoteViews;

/** Standalone weather for Android Home screens, with detail lines when resized. */
public class WeatherWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        if (ids.length == 0) return;
        PendingResult pending = goAsync();
        update(context.getApplicationContext(), manager, ids, () -> { if (pending != null) pending.finish(); });
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
            int id, Bundle options) {
        render(context, manager, id, WidgetKit.cachedJson(context));
    }

    static void requestPin(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        try {
            if (manager.isRequestPinAppWidgetSupported()
                    && manager.requestPinAppWidget(new ComponentName(context, WeatherWidget.class),
                            null, null)) {
                return;
            }
        } catch (RuntimeException unsupported) {
            // Some stock launchers only offer the widget through their picker.
        }
        Ui.toast(context, context.getString(R.string.weather_widget_picker_hint));
    }

    static void update(Context context, AppWidgetManager manager, int[] ids) {
        if (ids.length > 0) update(context.getApplicationContext(), manager, ids, () -> {});
    }

    private static void update(Context context, AppWidgetManager manager, int[] ids, Runnable done) {
        boolean fetching = false;
        try {
            String cached = WidgetKit.cachedJson(context);
            for (int id : ids) render(context, manager, id, cached);
            WidgetKit.fetchWeather(context, (summary, highLow, json) -> {
                try {
                    for (int id : ids) render(context, manager, id, json != null ? json : cached);
                } finally {
                    done.run();
                }
            });
            fetching = true;
        } finally {
            if (!fetching) done.run();
        }
    }

    private static void render(Context context, AppWidgetManager manager, int id, String json) {
        Bundle options = manager.getAppWidgetOptions(id);
        boolean wide = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) >= 300
                && options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) >= 145;
        RemoteViews views = WidgetKit.baseWeather(context,
                wide ? R.layout.widget_weather : R.layout.widget_weather_compact, id);
        if (wide) {
            WidgetKit.fillWeather(context, views, json);
        } else {
            String temp = json == null ? null : Weather.temperature(json);
            String condition = json == null ? null : Weather.condition(json);
            views.setTextViewText(R.id.widget_location, json == null
                    ? context.getString(R.string.widget_weather_label) : WidgetKit.cachedLabel(context));
            views.setTextViewText(R.id.widget_temp, temp != null ? temp : "—");
            views.setTextViewText(R.id.widget_condition,
                    condition != null ? condition : WidgetKit.WEATHER_UNAVAILABLE);
            views.setTextViewText(R.id.widget_highlow, WidgetKit.highLowShortLine(json));
            views.setImageViewResource(R.id.widget_condition_icon, json == null
                    ? R.drawable.ic_weather_cloudy : Weather.conditionIcon(Weather.code(json)));
        }
        manager.updateAppWidget(id, views);
    }
}
