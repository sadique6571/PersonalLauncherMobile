package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

/** 5. Digital Clock — small 2x2: big digits (self-updating TextClock) and the date, no weather. */
public class DigitalClockWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    static void update(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            RemoteViews views = WidgetKit.base(context, R.layout.widget_digital_clock, id);
            views.setTextViewText(R.id.widget_date, WidgetKit.dateLine());
            manager.updateAppWidget(id, views);
        }
    }
}
