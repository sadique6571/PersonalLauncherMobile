package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

/** 7. Analog Clock + Date — small 2x2: minimal dial (hour and minute hands only) with the date below. */
public class AnalogClockDateWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    static void update(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            RemoteViews views = WidgetKit.base(context, R.layout.widget_analog_clock_date, id);
            views.setTextViewText(R.id.widget_date, WidgetKit.dateLine());
            manager.updateAppWidget(id, views);
        }
    }
}
