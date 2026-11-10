package com.sadique.launcher.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Simple sun path: a semicircle arc with the sun's position on it. */
final class SunArcView extends View {
    private int riseMin = -1, setMin = -1, nowMin = -1;
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    SunArcView(Context context) {
        super(context);
        init();
    }

    SunArcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(4);
        arcPaint.setColor(0x66ffffff);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        sunPaint.setStyle(Paint.Style.FILL);
        sunPaint.setColor(0xffff9500);
        dimPaint.setStyle(Paint.Style.FILL);
        dimPaint.setColor(0x88ffffff);
    }

    /** Times in minutes since midnight; pass -1 for any unknown value. */
    void setTimes(int riseMin, int setMin, int nowMin) {
        this.riseMin = riseMin;
        this.setMin = setMin;
        this.nowMin = nowMin;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float cx = w / 2, cy = h - 8;
        float r = Math.min(w / 2 - 16, h - 24);
        if (r <= 0) return;
        RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
        canvas.drawArc(oval, 180, 180, false, arcPaint);
        canvas.drawLine(8, cy, w - 8, cy, arcPaint);
        if (riseMin < 0 || setMin <= riseMin) return;
        float fraction;
        boolean day;
        if (nowMin < 0) { fraction = 0.5f; day = false; }
        else if (nowMin < riseMin) { fraction = 0; day = false; }
        else if (nowMin > setMin) { fraction = 1; day = false; }
        else { fraction = (float) (nowMin - riseMin) / (setMin - riseMin); day = true; }
        double angle = Math.PI * (1 - fraction);
        float x = cx + r * (float) Math.cos(angle);
        float y = cy - r * (float) Math.sin(angle);
        canvas.drawCircle(x, y, day ? 14 : 10, day ? sunPaint : dimPaint);
    }
}
