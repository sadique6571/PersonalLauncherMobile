package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

/**
 * 10. Clock + Weather — medium 4x2: the transparent analog clock on
 * the left and a blue iPhone-style weather panel on the right, in one
 * widget — like the reference picture the user sent (image-7).
 *
 * Clock: the same clean face as the standalone Mechanical Clock widget
 * ({@link MechanicalClockFace}) — white dial straight on the wallpaper,
 * black Arabic numerals and hands, thin orange second hand that sweeps
 * smoothly. It is redrawn once a second from a chained AlarmManager
 * tick (see {@link WidgetKit#scheduleWidgetTick}).
 *
 * Weather: the blue gradient panel shows the location label, the big
 * current temperature, the condition icon + text, today's high/low
 * ("H:26° L:18°") and the sunset/sunrise line — sunrise before sunrise,
 * today's sunset during the day, tomorrow's sunrise after sunset, all
 * 12-hour AM/PM (see {@link WidgetKit#fillSun}). The weather comes from
 * the cached Open-Meteo reading; it is fetched only when the cache is
 * older than 30 minutes (or missing), never once a second — the clock
 * ticks, the weather waits. Tap the clock to open the app, tap the
 * weather panel to open the full Weather screen.
 *
 * Location name: Open-Meteo returns no place name, so the panel shows
 * the same label as the other weather widgets — "Current location" when
 * the user allowed coarse location, "Okara · fallback" otherwise.
 */
public class ClockWeatherWidget extends AppWidgetProvider {
    /** Internal one-second tick, sent only to this receiver. */
    static final String ACTION_TICK =
            "com.sadique.launcher.mobile.action.CLOCK_WEATHER_TICK";
    private static final int TICK_REQUEST_CODE = 9002;

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    @Override public void onEnabled(Context context) {
        WidgetKit.scheduleWidgetTick(context, ClockWeatherWidget.class, ACTION_TICK, TICK_REQUEST_CODE);
    }

    @Override public void onDisabled(Context context) {
        WidgetKit.cancelWidgetTick(context, ClockWeatherWidget.class, ACTION_TICK, TICK_REQUEST_CODE);
    }

    @Override public void onDeleted(Context context, int[] ids) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] remaining = manager.getAppWidgetIds(new ComponentName(context, ClockWeatherWidget.class));
        if (remaining.length == 0) WidgetKit.cancelWidgetTick(context, ClockWeatherWidget.class, ACTION_TICK, TICK_REQUEST_CODE);
    }

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_TICK.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            update(context, manager,
                    manager.getAppWidgetIds(new ComponentName(context, ClockWeatherWidget.class)));
            return;
        }
        super.onReceive(context, intent);
    }

    /**
     * Redraws every instance for the current time (clock, from the cache
     * for the weather) and re-arms the next tick; fetches fresh weather
     * only when the cache is stale — see {@link #maybeFetchWeather}.
     */
    static void update(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        WidgetKit.scheduleWidgetTick(context, ClockWeatherWidget.class, ACTION_TICK, TICK_REQUEST_CODE);
        render(context, manager, ids, WidgetKit.cachedJson(context));
        maybeFetchWeather(context, manager, ids);
    }

    /** Renders clock + weather panel for every instance from cached data (no network). */
    private static void render(Context context, AppWidgetManager manager, int[] ids, String json) {
        Bitmap face = MechanicalClockFace.draw(MechanicalClockFace.BITMAP_SIZE_PX,
                System.currentTimeMillis());
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_clock_weather);
            views.setImageViewBitmap(R.id.widget_clock_face, face);
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
     * Fetches fresh weather only when there is no cached reading or the
     * last attempt is older than the 30-minute widget interval. The
     * attempt is recorded before fetching, so an offline phone does not
     * retry on every one-second tick. The callback re-renders with the
     * fresh reading (or the cache when the fetch failed).
     */
    private static void maybeFetchWeather(Context context, AppWidgetManager manager, int[] ids) {
        long last = WidgetKit.lastWeatherFetchAt(context);
        if (WidgetKit.cachedJson(context) != null
                && System.currentTimeMillis() - last < WidgetKit.UPDATE_PERIOD_MILLIS) return;
        WidgetKit.noteWeatherFetchAttempt(context);
        WidgetKit.fetchWeather(context, (summary, highLow, fresh) -> {
            String shown = fresh != null ? fresh : WidgetKit.cachedJson(context);
            render(context, manager, ids, shown);
        });
    }
}
