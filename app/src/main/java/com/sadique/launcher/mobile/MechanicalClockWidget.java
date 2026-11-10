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
 * (black rounded-square tile with a white dial inside it, black
 * numerals and hands, thin orange second hand — iPhone clock style,
 * like the reference picture the user sent, image-6).
 * Works on any launcher (including the Vivo default launcher): it is a
 * plain AppWidget, so the launcher app never needs to be the default Home.
 *
 * The face is redrawn once a second from a chained AlarmManager
 * tick (see {@link WidgetKit#scheduleWidgetTick}), because
 * AppWidgetProvider's own updatePeriodMillis cannot go below
 * 30 minutes. Every tick re-arms the next one, and onUpdate/onEnabled
 * re-arm it too, so a missed tick cannot stall the chain. One tick
 * a second makes the second hand step 6 degrees a tick — the 100 ms
 * sweep was tried here and Vivo OriginOS batches those alarms into
 * steps of about 5.5 seconds, so the widget stays on the one-second
 * tick the user confirmed works on his phone. The smooth sweep lives
 * only on the launcher's own Home clock, which redraws from a
 * Handler, not from alarms.
 *
 * Battery note — please read before changing this: the tick is a
 * one-shot ELAPSED_REALTIME alarm (never wakes the device) and runs
 * only while at least one instance is placed; it stops when the last
 * instance is removed. One redraw a second (one 480 px bitmap + one
 * RemoteViews update per tick), only while the screen is on. When the
 * phone is fully asleep (Doze / deep sleep) Android defers
 * non-wakeup alarms — no API can tick a widget every second while
 * the phone sleeps without a new permission (SCHEDULE_EXACT_ALARM)
 * or a foreground service, and this widget uses neither. So in deep
 * sleep the second hand pauses and jumps to the correct time the
 * moment the phone wakes; that is an Android limit, not a bug. How
 * the tick is delivered also depends on alarm delivery — Android may
 * batch or delay a tick, and Vivo OriginOS restricts background
 * alarms unless the app is allowed in the battery settings (see README).
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
                System.currentTimeMillis(), true); // black tile, like reference picture image-6
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
