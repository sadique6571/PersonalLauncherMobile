package com.sadique.launcher.mobile;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** 2. Analog Clock Dark — small 2x2, dark navy face with white hands, glass card. */
public class AnalogClockDarkWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    static void update(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            manager.updateAppWidget(id, WidgetKit.base(context, R.layout.widget_analog_clock_dark, id));
        }
    }
}
