package com.sadique.launcher.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.widget.RemoteViews;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * Shared code for the home-screen widget pack (9 widgets). Every widget
 * provider is a thin class that builds its own RemoteViews and uses this
 * helper for the date line, the tap actions, the cached weather and the
 * Open-Meteo fetch — the fetch logic lives here once, not once per widget.
 *
 * Weather reuses {@link Weather} (Open-Meteo, free, no key): the current
 * coarse location once per refresh, Okara (30.81, 73.45) as the labelled
 * fallback, cached reading offline. A weather line is never blank: without
 * any reading it says "Weather unavailable" and each missing detail shows
 * "—". Coordinates go only to Open-Meteo for that one request and are
 * never stored.
 */
final class WidgetKit {
    static final int UPDATE_PERIOD_MILLIS = 1800000;
    static final String WEATHER_UNAVAILABLE = "Weather unavailable";
    static final String PREFS = "mobile_home";
    static final String WEATHER_KEY = "weather_text";
    static final String WEATHER_JSON_KEY = "weather_json";
    static final String WEATHER_LABEL_KEY = "weather_label";

    private WidgetKit() { }

    /** "EEEE, d MMMM" in the phone's locale, e.g. "Monday, 9 November". */
    static String dateLine() {
        return android.text.format.DateFormat.format("EEEE, d MMMM", new Date()).toString();
    }

    /** Fresh reading wins, else the cached reading, else "Weather unavailable" — never blank. */
    static String weatherText(String fresh, String cached) {
        if (fresh != null && !fresh.isEmpty()) return fresh;
        if (cached != null && !cached.isEmpty()) return cached;
        return WEATHER_UNAVAILABLE;
    }

    /** Last good weather line, from the shared cache (also written by HomeActivity). */
    static String cachedWeather(Context context) {
        String json = cachedJson(context);
        if (json != null) {
            String summary = Weather.summary(json, cachedLabel(context));
            if (summary != null) return summary;
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(WEATHER_KEY, "");
    }

    /** Last good full weather JSON, or null. */
    static String cachedJson(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(WEATHER_JSON_KEY, null);
    }

    /** Label for the cached reading: "Current location" or "Okara · fallback". */
    static String cachedLabel(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(WEATHER_LABEL_KEY, "Okara · fallback");
    }

    /** Tap a clock widget to open the app, where the location permission is first requested. */
    static PendingIntent tapToOpenApp(Context context, int requestCode) {
        Intent open = new Intent(context, HomeActivity.class);
        return PendingIntent.getActivity(context, requestCode, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Tap a weather widget to open the full Weather screen. */
    static PendingIntent tapToOpenWeather(Context context, int requestCode) {
        Intent open = new Intent(context, WeatherActivity.class);
        return PendingIntent.getActivity(context, requestCode, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** RemoteViews for a widget layout with the tap-to-open-app action already set. */
    static RemoteViews base(Context context, int layoutId, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        views.setOnClickPendingIntent(R.id.widget_root, tapToOpenApp(context, widgetId));
        return views;
    }

    /** RemoteViews for a weather widget layout with the tap-to-open-Weather action already set. */
    static RemoteViews baseWeather(Context context, int layoutId, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        views.setOnClickPendingIntent(R.id.widget_root, tapToOpenWeather(context, widgetId));
        return views;
    }

    /** Detail lines; a missing value shows "—" so the widget is never blank. */
    static String feelsLine(String json) {
        String v = json == null ? null : Weather.feelsLike(json);
        return "Feels like " + (v == null ? "—" : v);
    }

    static String windLine(String json) {
        String v = json == null ? null : Weather.wind(json);
        return "Wind " + (v == null ? "—" : v);
    }

    static String highLowLine(String json) {
        String v = json == null ? null : Weather.highLow(json);
        return v == null ? "High — · Low —" : v;
    }

    static String humidityLine(String json) {
        String v = json == null ? null : Weather.humidity(json);
        return "Humidity " + (v == null ? "—" : v);
    }

    static String rainLine(String json) {
        String v = json == null ? null : Weather.rainChance(json);
        return "Rain " + (v == null ? "—" : v);
    }

    static String uvLine(String json) {
        String v = json == null ? null : Weather.uvIndex(json);
        return "UV " + (v == null ? "—" : v);
    }

    static String pressureLine(String json) {
        String v = json == null ? null : Weather.pressure(json);
        return "Pressure " + (v == null ? "—" : v);
    }

    static String cloudLine(String json) {
        String v = json == null ? null : Weather.cloudCover(json);
        return "Cloud " + (v == null ? "—" : v);
    }

    /** Next sun event ("Sunset 5:20 PM"), or "Sunrise/Sunset —" with no data. */
    static String sunLine(String json) {
        Weather.SunEvent e = json == null ? null : Weather.sunEvent(json);
        return e == null ? "Sunrise/Sunset —" : e.label;
    }

    /** Sunrise or sunset icon for the next sun event (sunrise icon when unknown). */
    static int sunIcon(String json) {
        Weather.SunEvent e = json == null ? null : Weather.sunEvent(json);
        return e != null && !e.sunrise ? R.drawable.ic_sunset : R.drawable.ic_sunrise;
    }

    /** Fill the sun row (icon + line) of a weather widget; never blank. */
    static void fillSun(Context context, RemoteViews views, String json) {
        views.setImageViewResource(R.id.widget_sun_icon, sunIcon(json));
        views.setTextViewText(R.id.widget_sun, sunLine(json));
    }

    /** Fill the Weather widget's lines from a fetch result, or the cache when there is none. */
    static void fillWeather(Context context, RemoteViews views, String json) {
        if (json == null) json = cachedJson(context);
        String temp = json == null ? null : Weather.temperature(json);
        String condition = json == null ? null : Weather.condition(json);
        views.setTextViewText(R.id.widget_temp, temp != null ? temp : "—");
        views.setTextViewText(R.id.widget_condition, condition != null ? cachedLabel(context) + " · " + condition : WEATHER_UNAVAILABLE);
        views.setTextViewText(R.id.widget_highlow, highLowLine(json));
        views.setTextViewText(R.id.widget_feels, feelsLine(json));
        views.setTextViewText(R.id.widget_humidity, humidityLine(json));
        views.setTextViewText(R.id.widget_wind, windLine(json));
        views.setTextViewText(R.id.widget_rain, rainLine(json));
        fillSun(context, views, json);
    }

    /** Result of a weather fetch: the one-line summary, today's high/low, and the raw JSON. */
    interface FetchCallback {
        void onResult(String summary, String highLow, String json);
    }

    /** Fetch the full detail feed on a background thread; the callback runs on that thread. */
    @SuppressLint("MissingPermission")
    static void fetchWeather(Context context, FetchCallback callback) {
        new Thread(() -> {
            String summary = null, highLow = null, json = null;
            double latitude = Weather.OKARA_LATITUDE, longitude = Weather.OKARA_LONGITUDE;
            String label = "Okara · fallback";
            try {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    LocationManager locations = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    Location last = null;
                    if (locations != null) {
                        last = locations.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (last == null) last = locations.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                    if (last != null) { latitude = last.getLatitude(); longitude = last.getLongitude(); label = "Current location"; }
                }
                HttpURLConnection connection = (HttpURLConnection) new URL(Weather.detailUrl(latitude, longitude)).openConnection();
                connection.setConnectTimeout(5000); connection.setReadTimeout(5000); connection.setRequestMethod("GET");
                if (connection.getResponseCode() == 200) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] block = new byte[4096]; int length;
                    try (InputStream in = connection.getInputStream()) {
                        while ((length = in.read(block)) != -1) { if (out.size() + length > 65536) break; out.write(block, 0, length); }
                    }
                    json = out.toString("UTF-8");
                    summary = Weather.summary(json, label);
                    highLow = Weather.highLow(json);
                }
                connection.disconnect();
            } catch (Exception offline) { /* Cached readings stay; the line never goes blank. */ }
            if (summary != null) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(WEATHER_KEY, summary).putString(WEATHER_JSON_KEY, json).putString(WEATHER_LABEL_KEY, label).apply();
            }
            callback.onResult(summary, highLow, json);
        }).start();
    }

    /** Refresh every installed widget of the pack (called when the app resumes, e.g. after granting location). */
    static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        AnalogClockWidget.update(context, manager, manager.getAppWidgetIds(new ComponentName(context, AnalogClockWidget.class)));
        AnalogClockDarkWidget.update(context, manager, manager.getAppWidgetIds(new ComponentName(context, AnalogClockDarkWidget.class)));
        AnalogClockWeatherWidget.update(context, manager, manager.getAppWidgetIds(new ComponentName(context, AnalogClockWeatherWidget.class)));
        DigitalClockWeatherWidget.update(context, manager, manager.getAppWidgetIds(new ComponentName(context, DigitalClockWeatherWidget.class)));
        DigitalClockWidget.update(context, manager, manager.getAppWidgetIds(new ComponentName(context, DigitalClockWidget.class)));
        WeatherWidget.update(context, manager, manager.getAppWidgetIds(new ComponentName(context, WeatherWidget.class)));
        AnalogClockDateWidget.update(context, manager, manager.getAppWidgetIds(new ComponentName(context, AnalogClockDateWidget.class)));
        BigClockWeatherWidget.update(context, manager, manager.getAppWidgetIds(new ComponentName(context, BigClockWeatherWidget.class)));
        MechanicalClockWidget.update(context, manager, manager.getAppWidgetIds(new ComponentName(context, MechanicalClockWidget.class)));
    }
}
