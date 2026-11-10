package com.sadique.launcher.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

/**
 * Analog clock View for the launcher's own Home screen.
 *
 * It draws the same clean face as the widgets (white dial, black Arabic
 * numerals, black hands, thin orange second hand — transparent
 * background, no black tile, reference picture image-7 style) straight
 * onto its own canvas, so no Bitmap is allocated per frame.
 *
 * HomeActivity redraws this View about ten times a second with a
 * Handler while Home is resumed, so the second hand sweeps smoothly
 * (0.6 degrees a frame). An in-app View is not subject to the alarm
 * batching that makes the AppWidget second hand step about every
 * 5.5 seconds on the user's Vivo phone — that is why the sweep lives
 * here, on Home, and not only in the widget.
 *
 * No exact-alarm API, no foreground service, no new permission.
 */
final class HomeAnalogClockView extends View {
    HomeAnalogClockView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        int size = Math.min(width, height);
        if (size <= 0) size = Math.max(width, height);
        setMeasuredDimension(size, size);
    }

    @Override protected void onDraw(Canvas canvas) {
        int size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;
        canvas.save();
        canvas.translate((getWidth() - size) / 2f, (getHeight() - size) / 2f);
        MechanicalClockFace.drawOn(canvas, size, System.currentTimeMillis(), false);
        canvas.restore();
    }
}
