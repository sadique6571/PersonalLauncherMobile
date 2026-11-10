package com.sadique.launcher.mobile;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.widget.RemoteViews;

/**
 * 9. Mechanical Clock — small 2x2, skeleton watch face drawn in code.
 * Works on any launcher (including the Vivo default launcher): it is a
 * plain AppWidget, so the launcher app never needs to be the default Home.
 *
 * The face is redrawn once a second from an AlarmManager tick, because
 * AppWidgetProvider's own updatePeriodMillis cannot go below 30 minutes.
 *
 * Battery note: the tick is ELAPSED_REALTIME (never wakes the device)
 * and runs only while at least one instance is placed; it stops when the
 * last instance is removed. Each tick renders one 480 px bitmap, which
 * costs about the same as a normal clock widget refresh. In Doze the
 * system may batch or delay the ticks, so the second hand can pause
 * while the phone is idle — that is the trade-off for not needing the
 * SCHEDULE_EXACT_ALARM permission (no new permission is requested).
 */
public class MechanicalClockWidget extends AppWidgetProvider {
    /** Internal one-second tick, sent only to this receiver. */
    static final String ACTION_TICK =
            "com.sadique.launcher.mobile.action.MECHANICAL_CLOCK_TICK";
    private static final long TICK_MILLIS = 1000L;
    private static final int TICK_REQUEST_CODE = 9001;

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    @Override public void onEnabled(Context context) {
        startTick(context);
    }

    @Override public void onDisabled(Context context) {
        stopTick(context);
    }

    @Override public void onDeleted(Context context, int[] ids) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] remaining = manager.getAppWidgetIds(new ComponentName(context, MechanicalClockWidget.class));
        if (remaining.length == 0) stopTick(context);
    }

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_TICK.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            update(context, manager,
                    manager.getAppWidgetIds(new ComponentName(context, MechanicalClockWidget.class)));
            return;
        }
        super.onReceive(context, intent);
    }

    /** Redraws every instance for the current time and keeps the tick running. */
    static void update(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        startTick(context);
        Bitmap face = MechanicalClockFace.draw(MechanicalClockFace.BITMAP_SIZE_PX,
                System.currentTimeMillis());
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_mechanical_clock);
            views.setImageViewBitmap(R.id.widget_mechanical_face, face);
            views.setOnClickPendingIntent(R.id.widget_root,
                    WidgetKit.tapToOpenApp(context, id));
            manager.updateAppWidget(id, views);
        }
    }

    private static void startTick(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        alarms.setRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + TICK_MILLIS, TICK_MILLIS, tickIntent(context));
    }

    private static void stopTick(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        alarms.cancel(tickIntent(context));
    }

    private static PendingIntent tickIntent(Context context) {
        Intent tick = new Intent(context, MechanicalClockWidget.class);
        tick.setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(context, TICK_REQUEST_CODE, tick,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
