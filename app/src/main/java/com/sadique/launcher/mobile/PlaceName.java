package com.sadique.launcher.mobile;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Real place name for the current location ("54-2L", "Okara", ...),
 * from a free keyless reverse lookup — Nominatim (OpenStreetMap).
 * Open-Meteo returns no place name, so the weather panel used to say
 * only "Current location"; the user travels (village 54-2L today,
 * somewhere else tomorrow), so the panel now shows the real name.
 *
 * Nominatim usage policy: no key and no account, but a valid
 * User-Agent identifying the app is required, and at most one
 * request a second. The weather fetch runs at most once every
 * 30 minutes, so that limit is never approached. A failed lookup
 * (offline, or a place Nominatim does not know) returns null and
 * the label stays "Current location" — never blank, never wrong.
 *
 * Privacy: the resolved NAME is cached on the device, together with
 * the coordinates rounded to 2 decimals (about 1 km) and a timestamp,
 * only so the name is re-asked when the phone has moved a lot or a
 * day has passed. Nothing is uploaded or logged anywhere else, and
 * the exact coordinates are never stored. If Nominatim has no name
 * for a village, the panel keeps showing "Current location".
 */
final class PlaceName {
    /** Nominatim reverse endpoint (no key, no account). */
    static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse";
    /** User-Agent is required by the Nominatim usage policy. */
    static final String USER_AGENT = "PersonalLauncherMobile/7.0 (contact sadique6571@gmail.com)";
    /** Cache keys in the shared "mobile_home" prefs (see WidgetKit.PREFS). */
    static final String PLACE_NAME_KEY = "place_name";
    static final String PLACE_LAT_KEY = "place_lat";
    static final String PLACE_LON_KEY = "place_lon";
    static final String PLACE_AT_KEY = "place_at";
    /** A cached name is reused for a day, or while the phone stays within ~2 km. */
    static final long PLACE_MAX_AGE_MILLIS = 24L * 60 * 60 * 1000;
    /** ~2 km in degrees (latitude). */
    static final double PLACE_REUSE_DEGREES = 0.02;

    private PlaceName() { }

    /**
     * Best place name from a Nominatim reverse response, or null.
     * Pure string logic, unit-tested. Prefers the address village /
     * town / city (a chak like "54-2L" comes back as the village),
     * then the top-level name, then the first part of display_name.
     */
    static String pickName(String json) {
        if (json == null) return null;
        String address = section(json, "address");
        if (address != null) {
            String[] keys = {"village", "town", "city", "municipality",
                    "suburb", "neighbourhood", "hamlet", "county", "state_district"};
            for (String key : keys) {
                String value = stringAfter(address, "\"" + key + "\":");
                if (value != null && !value.isEmpty()) return value;
            }
        }
        String name = stringAfter(json, "\"name\":");
        if (name != null && !name.isEmpty()) return name;
        String display = stringAfter(json, "\"display_name\":");
        if (display != null && !display.isEmpty()) {
            int comma = display.indexOf(',');
            return comma > 0 ? display.substring(0, comma).trim() : display;
        }
        return null;
    }

    /** Cached name for these coordinates, or null when stale or moved away. */
    static String cachedName(Context context, double latitude, double longitude) {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(WidgetKit.PREFS, Context.MODE_PRIVATE);
        String name = prefs.getString(PLACE_NAME_KEY, null);
        if (name == null || name.isEmpty()) return null;
        long at = prefs.getLong(PLACE_AT_KEY, 0);
        if (System.currentTimeMillis() - at > PLACE_MAX_AGE_MILLIS) return null;
        double lat, lon;
        try {
            lat = Double.parseDouble(prefs.getString(PLACE_LAT_KEY, ""));
            lon = Double.parseDouble(prefs.getString(PLACE_LON_KEY, ""));
        } catch (Exception bad) { return null; }
        if (Math.abs(latitude - lat) > PLACE_REUSE_DEGREES
                || Math.abs(longitude - lon) > PLACE_REUSE_DEGREES) return null;
        return name;
    }

    /**
     * Name for these coordinates: cache first, else one Nominatim
     * request. Call only on a background thread (the weather fetch
     * thread). Returns null when no name is known.
     */
    static String resolve(Context context, double latitude, double longitude) {
        String cached = cachedName(context, latitude, longitude);
        if (cached != null) return cached;
        String fresh = reverseGeocode(latitude, longitude);
        if (fresh != null && !fresh.isEmpty()) {
            context.getSharedPreferences(WidgetKit.PREFS, Context.MODE_PRIVATE).edit()
                    .putString(PLACE_NAME_KEY, fresh)
                    .putString(PLACE_LAT_KEY, Double.toString(Math.round(latitude * 100.0) / 100.0))
                    .putString(PLACE_LON_KEY, Double.toString(Math.round(longitude * 100.0) / 100.0))
                    .putLong(PLACE_AT_KEY, System.currentTimeMillis()).apply();
            return fresh;
        }
        return null;
    }

    /** One Nominatim reverse request, or null on any failure. */
    private static String reverseGeocode(double latitude, double longitude) {
        try {
            String url = NOMINATIM_URL + "?format=json&lat=" + latitude + "&lon=" + longitude
                    + "&zoom=14&addressdetails=1";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            if (connection.getResponseCode() != 200) { connection.disconnect(); return null; }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] block = new byte[4096];
            int length;
            try (InputStream in = connection.getInputStream()) {
                while ((length = in.read(block)) != -1) {
                    if (out.size() + length > 32768) break;
                    out.write(block, 0, length);
                }
            }
            connection.disconnect();
            return pickName(out.toString("UTF-8"));
        } catch (Exception offline) { return null; }
    }

    /** Object body of "name":{...} (including the braces), or null. */
    private static String section(String json, String name) {
        if (json == null) return null;
        int at = json.indexOf("\"" + name + "\":{");
        if (at < 0) return null;
        int start = json.indexOf('{', at);
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return json.substring(start, i + 1); }
        }
        return null;
    }

    /** String value after "key": — the first quoted string — or null. */
    private static String stringAfter(String json, String key) {
        if (json == null) return null;
        int at = json.indexOf(key);
        if (at < 0) return null;
        int start = at + key.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length() || json.charAt(start) != '"') return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }
}
