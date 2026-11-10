package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

/**
 * 9. Mechanical Clock — small 2x2, clean analog face drawn in code
 * (white dial straight on the wallpaper, black numerals and hands,
 * thin orange second hand — iPhone clock style, like the reference
 * picture the user sent).
 * Works on any launcher (including the Vivo default launcher): it is a
 * plain AppWidget, so the launcher app never needs to be the default Home.
 *
 * The face is redrawn once a second from a chained AlarmManager tick
 * (see {@link WidgetKit#scheduleWidgetTick}), because
 * AppWidgetProvider's own updatePeriodMillis cannot go below 30 minutes.
 * Every tick re-arms the next one, and onUpdate/onEnabled re-arm it too,
 * so a missed tick cannot stall the chain.
 *
 * Battery note — please read before changing this: the tick is a
 * one-shot ELAPSED_REALTIME alarm (never wakes the device) and runs
 * only while at least one instance is placed; it stops when the last
 * instance is removed. While the screen is on the alarm fires within
 * about a second and the face always draws the real current time, so
 * all three hands move smoothly and stay correct. When the phone is
 * fully asleep (Doze / deep sleep) Android defers non-wakeup alarms —
 * no API can tick a widget every second while the phone sleeps without
 * a new permission (SCHEDULE_EXACT_ALARM) or a foreground service, and
 * this widget uses neither. So in deep sleep the second hand pauses
 * and jumps to the correct time the moment the phone wakes; that is
 * an Android limit, not a bug. (An earlier version used
 * setRepeating, which Android treats as inexact since API 19 and
 * batches aggressively — on Vivo OriginOS the ticks could stop
 * entirely while the screen was on, so the hands only jumped when the
 * screen changed. The chained one-shot alarm below is the most
 * reliable no-new-permission replacement.)
 */
public class MechanicalClockWidget extends AppWidgetProvider {
    /** Internal one-second tick, sent only to this receiver. */
    static final String ACTION_TICK =
            "com.sadique.launcher.mobile.action.MECHANICAL_CLOCK_TICK";
    private static final int TICK_REQUEST_CODE = 9001;

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    @Override public void onEnabled(Context context) {
        WidgetKit.scheduleWidgetTick(context, MechanicalClockWidget.class, ACTION_TICK, TICK_REQUEST_CODE);
    }

    @Override public void onDisabled(Context context) {
        WidgetKit.cancelWidgetTick(context, MechanicalClockWidget.class, ACTION_TICK, TICK_REQUEST_CODE);
    }

    @Override public void onDeleted(Context context, int[] ids) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] remaining = manager.getAppWidgetIds(new ComponentName(context, MechanicalClockWidget.class));
        if (remaining.length == 0) WidgetKit.cancelWidgetTick(context, MechanicalClockWidget.class, ACTION_TICK, TICK_REQUEST_CODE);
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

    /** Redraws every instance for the current time and re-arms the next tick. */
    static void update(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        WidgetKit.scheduleWidgetTick(context, MechanicalClockWidget.class, ACTION_TICK, TICK_REQUEST_CODE);
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
}
