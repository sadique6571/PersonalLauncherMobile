package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** 1. Analog Clock — small 2x2, light face, glass card. Hands are drawn by Android from the system time. */
public class AnalogClockWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    static void update(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            manager.updateAppWidget(id, WidgetKit.base(context, R.layout.widget_analog_clock, id));
        }
    }
}
