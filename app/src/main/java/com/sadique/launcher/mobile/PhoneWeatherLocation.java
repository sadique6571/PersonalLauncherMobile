package com.sadique.launcher.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.Locale;
import java.util.function.Consumer;

final class PhoneWeatherLocation implements LocationListener {
    private static final long MAX_AGE_MS = 15 * 60_000L;
    private static final long TIMEOUT_MS = 12_000L;

    private final Context context;
    private final LocationManager manager;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable timeout = () -> finish(null);
    private Consumer<Location> result;

    PhoneWeatherLocation(Context context) {
        this.context = context.getApplicationContext();
        manager = context.getSystemService(LocationManager.class);
    }

    @SuppressLint("MissingPermission")
    void request(Consumer<Location> callback) {
        cancel();
        result = callback;
        if (manager == null || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            finish(null);
            return;
        }

        Location recent = null;
        boolean listening = false;
        for (String provider : manager.getProviders(true)) {
            if (LocationManager.GPS_PROVIDER.equals(provider)) continue;
            try {
                Location last = manager.getLastKnownLocation(provider);
                long age = last == null ? -1
                        : SystemClock.elapsedRealtime() - last.getElapsedRealtimeNanos() / 1_000_000L;
                if (age >= 0 && age <= MAX_AGE_MS && (recent == null
                        || last.getElapsedRealtimeNanos() > recent.getElapsedRealtimeNanos())) {
                    recent = last;
                }
                if (LocationManager.PASSIVE_PROVIDER.equals(provider)) continue;
                manager.requestLocationUpdates(provider, 0, 0f, this, Looper.getMainLooper());
                listening = true;
            } catch (IllegalArgumentException | SecurityException ignored) {
                // Some devices expose providers that require more than approximate permission.
            }
        }
        if (recent != null || !listening) finish(recent);
        else main.postDelayed(timeout, TIMEOUT_MS);
    }

    void cancel() {
        if (result == null) return;
        result = null;
        main.removeCallbacks(timeout);
        if (manager != null) manager.removeUpdates(this);
    }

    private void finish(Location location) {
        Consumer<Location> callback = result;
        cancel();
        if (callback != null) callback.accept(location);
    }

    @Override public void onLocationChanged(Location location) { finish(location); }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }

    static String code(double latitude, double longitude) {
        // Keep this format in sync with TV WeatherSettings. Round to a 0.05° grid.
        long north = Math.round((latitude + 90) * 20);
        long east = Math.round((longitude + 180) * 20);
        String body = Long.toString(north * 7201 + east, 36);
        body = "00000".substring(body.length()) + body;
        int check = 0;
        for (int i = 0; i < body.length(); i++) {
            check += Character.digit(body.charAt(i), 36) * ((i + 1) * 6 - 1);
        }
        return ("W" + body + Character.forDigit(check % 36, 36)).toUpperCase(Locale.ROOT);
    }
}
