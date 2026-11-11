package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** The widget host animates the clock hands from system time. */
public class MechanicalClockWidget extends AppWidgetProvider {
    private static final String LEGACY_TICK = "com.sadique.launcher.mobile.action.MECHANICAL_CLOCK_TICK";

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
                    manager.getAppWidgetIds(new ComponentName(context, MechanicalClockWidget.class)));
            return;
        }
        super.onReceive(context, intent);
    }

    static void update(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        WidgetKit.cancelWidgetTick(context, MechanicalClockWidget.class, LEGACY_TICK, 9001);
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_mechanical_clock);
            views.setOnClickPendingIntent(R.id.widget_root,
                    WidgetKit.tapToOpenApp(context, id));
            manager.updateAppWidget(id, views);
        }
    }
}
