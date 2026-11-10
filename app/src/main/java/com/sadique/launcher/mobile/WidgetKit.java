package com.sadique.launcher.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
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
import android.os.SystemClock;
import android.widget.RemoteViews;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * Shared code for the home-screen widget pack (10 widgets). Every widget
 * provider is a thin class that builds its own RemoteViews and uses this
 * helper for the date line, the tap actions, the cached weather and the
 * Open-Meteo fetch — the fetch logic lives here once, not once per widget.
 *
 * Weather reuses {@link Weather} (Open-Meteo, free, no key): the current
 * coarse location once per refresh, Okara (30.81, 73.45) as the labelled
 * fallback, cached reading offline. A weather line is never blank: without
 * any reading it says "Weather unavailable" and each missing detail shows
 * "—". Coordinates go only to Open-Meteo for that one request and are
 * never stored, except the coarse ~2 km grid reference kept on the
 * device for the place-name cache (see {@link PlaceName}) — no
 * coordinates are ever uploaded or logged anywhere else. The place
 * name shown on the widgets ("54-2L", "Okara", ...) comes from a free
 * keyless reverse lookup (Nominatim / OpenStreetMap); when no name is
 * found the label stays "Current location".
 */
final class WidgetKit {
    static final int UPDATE_PERIOD_MILLIS = 1800000;
    static final String WEATHER_UNAVAILABLE = "Weather unavailable";
    static final String PREFS = "mobile_home";
    static final String WEATHER_KEY = "weather_text";
    static final String WEATHER_JSON_KEY = "weather_json";
    static final String WEATHER_LABEL_KEY = "weather_label";
    /** When the last weather fetch was attempted (millis); gates re-fetches. */
    static final String WEATHER_FETCHED_AT_KEY = "weather_fetched_at";
    /** The user's own Google Weather API key, typed in Launcher settings — phone only, never in the repo. */
    static final String GOOGLE_API_KEY_PREF = "google_weather_api_key";

    /** The user's Google Weather API key, or "" when none is set (Open-Meteo is used then). */
    static String googleApiKey(Context context) {
        String key = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(GOOGLE_API_KEY_PREF, "");
        return key == null ? "" : key;
    }

    /** Saves the Google Weather API key (trimmed; empty removes it). Phone only — never uploaded. */
    static void setGoogleApiKey(Context context, String key) {
        String trimmed = key == null ? "" : key.trim();
        android.content.SharedPreferences.Editor edit =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (trimmed.isEmpty()) edit.remove(GOOGLE_API_KEY_PREF);
        else edit.putString(GOOGLE_API_KEY_PREF, trimmed);
        edit.apply();
    }

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

    /** Label for the cached reading: the real place name ("54-2L"), "Current location", or "Okara · fallback". */
    static String cachedLabel(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(WEATHER_LABEL_KEY, "Okara · fallback");
    }

    /** When the last weather fetch was attempted, or 0 when never. */
    static long lastWeatherFetchAt(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(WEATHER_FETCHED_AT_KEY, 0);
    }

    /** Record a weather fetch attempt (success or not) so the next one waits out the interval. */
    static void noteWeatherFetchAttempt(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(WEATHER_FETCHED_AT_KEY, System.currentTimeMillis()).apply();
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

    /** "H:26° L:18°" (iPhone style), or "H:— L:—" with no data. */
    static String highLowShortLine(String json) {
        String v = json == null ? null : Weather.highLowShort(json);
        return v == null ? "H:— L:—" : v;
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

    /** Fetch the full detail feed on a background thread; the callback runs on that thread.
     *  Data source is Open-Meteo by default; when the user set a Google
     *  Weather API key in Launcher settings, Google is tried first and
     *  Open-Meteo is the fallback (see {@link Weather#fromGoogle}). */
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
                // Real place name for the weather panel ("54-2L", "Okara", ...):
                // one free keyless reverse lookup, cached — see PlaceName.
                // No name found (or no network) keeps "Current location".
                if ("Current location".equals(label)) {
                    String place = PlaceName.resolve(context, latitude, longitude);
                    if (place != null && !place.isEmpty()) label = place;
                }
                // Google Weather API when the user set a key in Launcher
                // settings (3 calls a refresh: current + hourly + daily);
                // any failure falls through to Open-Meteo below, so the
                // widget never goes blank because of a bad key or quota.
                String key = googleApiKey(context);
                if (key != null && !key.isEmpty()) {
                    json = fetchGoogleJson(latitude, longitude, key);
                    if (json != null) {
                        summary = Weather.summary(json, label);
                        highLow = Weather.highLow(json);
                    }
                }
                if (json == null) {
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
                }
            } catch (Exception offline) { /* Cached readings stay; the line never goes blank. */ }
            if (summary != null) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(WEATHER_KEY, summary).putString(WEATHER_JSON_KEY, json).putString(WEATHER_LABEL_KEY, label).apply();
            }
            callback.onResult(summary, highLow, json);
        }).start();
    }

    /**
     * Google Weather API fetch: current conditions + 48 hourly + 7 daily,
     * mapped into the Open-Meteo JSON shape by {@link Weather#fromGoogle}.
     * Returns null on any failure (bad key, quota spent, offline) — the
     * caller then falls back to Open-Meteo. Runs on the caller's thread.
     */
    private static String fetchGoogleJson(double latitude, double longitude, String key) {
        try {
            String current = get(Weather.googleCurrentConditionsUrl(latitude, longitude, key));
            if (current == null) return null;
            String hours = get(Weather.googleForecastHoursUrl(latitude, longitude, key));
            String days = get(Weather.googleForecastDaysUrl(latitude, longitude, key));
            return Weather.fromGoogle(current, hours, days);
        } catch (Exception offline) { return null; }
    }

    /** One HTTP GET, response body as UTF-8 (capped at 256 KB), or null on any failure. */
    private static String get(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() != 200) { connection.disconnect(); return null; }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] block = new byte[4096];
            int length;
            try (InputStream in = connection.getInputStream()) {
                while ((length = in.read(block)) != -1) {
                    if (out.size() + length > 262144) break;
                    out.write(block, 0, length);
                }
            }
            connection.disconnect();
            return out.toString("UTF-8");
        } catch (Exception offline) { return null; }
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
        ClockWeatherWidget.update(context, manager, manager.getAppWidgetIds(new ComponentName(context, ClockWeatherWidget.class)));
    }

    /**
     * Schedules the next one-shot tick for a clock widget, aligned to
     * the next whole second so drift never accumulates. The face
     * redraws once a second, so the second hand steps 6 degrees a
     * tick — the 100 ms sweep was tried here and Vivo OriginOS
     * batches those alarms into steps of about 5.5 seconds, so the
     * widgets stay on the one-second tick the user confirmed works
     * on his phone. A one-shot {@link AlarmManager#set} (not
     * setRepeating) is used on purpose: since API 19 setRepeating
     * is inexact and Android batches it, and on Vivo OriginOS those
     * batches can stop entirely while the screen is on — that was the
     * bug where the hands froze and only jumped on a screen change.
     * Each tick is re-armed from onReceive/update/onEnabled, so the
     * chain keeps running while the screen is on. No exact-alarm API
     * is used, so no SCHEDULE_EXACT_ALARM permission is needed and no
     * foreground service is created. ACTION_SCREEN_ON cannot be used
     * instead: it is a protected broadcast that manifest receivers
     * never receive.
     *
     * Battery note — please read before changing this: one redraw a
     * second (one 480 px bitmap + one RemoteViews update per tick,
     * only while at least one clock widget instance is placed; it
     * stops when the last one is removed). When the phone is fully
     * asleep (Doze / deep sleep) Android defers non-wakeup alarms,
     * so the second hand pauses and jumps to the correct time the
     * moment the phone wakes; that is an Android limit, not a bug.
     * How the tick is delivered also depends on alarm delivery —
     * Android may batch or delay a tick, and Vivo OriginOS restricts
     * background alarms unless the app is allowed in the battery
     * settings (see README). The smooth sweep lives only on the
     * launcher's own Home clock, which redraws from a Handler, not
     * from alarms.
     */
    static void scheduleWidgetTick(Context context, Class<?> receiver, String action, int requestCode) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        long delay = MechanicalClockFace.millisToNextSecond(System.currentTimeMillis());
        alarms.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + delay, tickIntent(context, receiver, action, requestCode));
    }

    /** Cancels a widget's tick chain (when its last instance is removed). */
    static void cancelWidgetTick(Context context, Class<?> receiver, String action, int requestCode) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        alarms.cancel(tickIntent(context, receiver, action, requestCode));
    }

    private static PendingIntent tickIntent(Context context, Class<?> receiver, String action, int requestCode) {
        Intent tick = new Intent(context, receiver);
        tick.setAction(action);
        return PendingIntent.getBroadcast(context, requestCode, tick,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
