package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** The host animates the clock while weather refreshes on the widget interval. */
public class ClockWeatherWidget extends AppWidgetProvider {
    private static final String LEGACY_TICK = "com.sadique.launcher.mobile.action.CLOCK_WEATHER_TICK";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (LEGACY_TICK.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            update(context, manager,
                    manager.getAppWidgetIds(new ComponentName(context, ClockWeatherWidget.class)));
            return;
        }
        super.onReceive(context, intent);
    }

    static void update(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        WidgetKit.cancelWidgetTick(context, ClockWeatherWidget.class, LEGACY_TICK, 9002);
        render(context, manager, ids, WidgetKit.cachedJson(context));
        maybeFetchWeather(context, manager, ids);
    }

    /** Renders clock + weather panel for every instance from cached data (no network). */
    private static void render(Context context, AppWidgetManager manager, int[] ids, String json) {
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_clock_weather);
            views.setOnClickPendingIntent(R.id.widget_clock_face,
                    WidgetKit.tapToOpenApp(context, id));
            views.setOnClickPendingIntent(R.id.widget_weather_panel,
                    WidgetKit.tapToOpenWeather(context, id));
            fillWeatherPanel(context, views, json);
            manager.updateAppWidget(id, views);
        }
    }

    /** Fills the blue weather panel; a missing value shows "—", never a blank. */
    private static void fillWeatherPanel(Context context, RemoteViews views, String json) {
        views.setTextViewText(R.id.widget_location, WidgetKit.cachedLabel(context) + " ↗");
        String temp = json == null ? null : Weather.temperature(json);
        views.setTextViewText(R.id.widget_temp, temp != null ? temp : "—");
        int code = json == null ? Integer.MIN_VALUE : Weather.code(json);
        String condition = json == null ? null : Weather.condition(json);
        views.setImageViewResource(R.id.widget_condition_icon,
                code == Integer.MIN_VALUE ? R.drawable.ic_weather_cloudy : Weather.conditionIcon(code));
        views.setTextViewText(R.id.widget_condition,
                condition != null ? condition : WidgetKit.WEATHER_UNAVAILABLE);
        views.setTextViewText(R.id.widget_highlow, WidgetKit.highLowShortLine(json));
        WidgetKit.fillSun(context, views, json);
    }

    /**
     * Fetches fresh weather when the
     * last attempt is older than the 30-minute widget interval. The
     * attempt is recorded before fetching. The callback re-renders with the
     * fresh reading (or the cache when the fetch failed).
     */
    private static void maybeFetchWeather(Context context, AppWidgetManager manager, int[] ids) {
        long last = WidgetKit.lastWeatherFetchAt(context);
        long age = System.currentTimeMillis() - last;
        if (age >= 0 && age < WidgetKit.UPDATE_PERIOD_MILLIS) return;
        WidgetKit.noteWeatherFetchAttempt(context);
        WidgetKit.fetchWeather(context, (summary, highLow, fresh) -> {
            String shown = fresh != null ? fresh : WidgetKit.cachedJson(context);
            render(context, manager, ids, shown);
        });
    }
}
