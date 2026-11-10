package com.sadique.launcher.mobile;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import java.util.Calendar;

/**
 * 9. Mechanical Clock — clean analog face drawn in code on a Bitmap.
 *
 * The look follows the reference pictures the user sent (an iPhone-style
 * clock widget on the home screen): a plain white dial, black Arabic
 * numerals 1-12, black minute ticks, thick black hour/minute hands and
 * a thin orange second hand. No gears, no skeleton, no date window, no
 * jewels — the dial is completely clean, exactly like the pictures.
 * Everything is drawn with Canvas (no image assets), so the hands show
 * the real time and the face scales cleanly.
 *
 * Two styles: the standalone Mechanical Clock widget uses the black
 * rounded-square tile (reference picture image-6: black tile, white
 * dial inside it); the combined Clock + Weather widget uses the
 * transparent background (reference picture image-7: white dial
 * straight on the wallpaper, no tile). See {@link #draw(int, long,
 * boolean)}.
 *
 * The launcher's own Home screen reuses the same face through
 * {@link #drawOn} in {@link HomeAnalogClockView}: an in-app View
 * redrawn by a Handler about ten times a second while Home is
 * resumed, so its second hand sweeps smoothly. That path is not
 * subject to the alarm batching that makes the AppWidget second hand
 * step about every 5.5 seconds on the user's Vivo phone.
 *
 * The face is redrawn once a second by the widget tick (see
 * {@link #millisToNextSecond}), so the second hand steps 6 degrees
 * a second — the 100 ms sweep was tried on the widgets and Vivo
 * OriginOS batches those alarms into steps of about 5.5 seconds, so
 * the widgets stay on the one-second tick the user confirmed works
 * on his phone. The smooth sweep lives only on the launcher's own
 * Home clock, which redraws about ten times a second from a Handler
 * (see {@link #millisToNextFrame}). Only the angle math is
 * unit-tested (it uses no Android classes); {@link #draw(int, long,
 * boolean)} needs a device or CI.
 */
final class MechanicalClockFace {
    /** Bitmap edge in pixels; the widget ImageView scales it to fit. */
    static final int BITMAP_SIZE_PX = 480;
    /** Home clock redraw interval in milliseconds: 10 frames a second, a smooth sweep (Handler-driven; the widgets use TICK_MILLIS instead). */
    static final long FRAME_MILLIS = 100;
    /** Widget tick interval in milliseconds: one tick a second. */
    static final long TICK_MILLIS = 1000;

    private static final int TILE = 0xFF000000;
    private static final int DIAL = 0xFFFFFFFF;
    private static final int INK = 0xFF000000;
    /** iOS orange for the second hand, like the reference pictures. */
    private static final int ORANGE = 0xFFFF9500;

    private static final String[] NUMERALS = {
            "12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"
    };

    private MechanicalClockFace() { }

    // ---- Pure time math (no Android classes, unit-tested) ----

    /** Second hand: one full turn per minute, smooth (uses milliseconds). */
    static double secondAngle(long nowMillis) {
        Calendar c = calendar(nowMillis);
        return (c.get(Calendar.SECOND) + c.get(Calendar.MILLISECOND) / 1000.0) * 6.0;
    }

    /** Minute hand: one full turn per hour. */
    static double minuteAngle(long nowMillis) {
        Calendar c = calendar(nowMillis);
        return (c.get(Calendar.MINUTE) + (c.get(Calendar.SECOND)
                + c.get(Calendar.MILLISECOND) / 1000.0) / 60.0) * 6.0;
    }

    /** Hour hand: one full turn per 12 hours. */
    static double hourAngle(long nowMillis) {
        Calendar c = calendar(nowMillis);
        return ((c.get(Calendar.HOUR) % 12) + c.get(Calendar.MINUTE) / 60.0
                + (c.get(Calendar.SECOND) + c.get(Calendar.MILLISECOND) / 1000.0) / 3600.0) * 30.0;
    }

    private static Calendar calendar(long nowMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(nowMillis);
        return c;
    }

    /**
     * Milliseconds until the next 100 ms frame boundary, for aligning
     * the Home clock sweep loop. Used by the Home clock so every
     * frame lands on a real frame boundary and drift never
     * accumulates: 100 at an exact boundary, 50 halfway through a
     * frame, 1 just before the next one. Ten frames a second make
     * the second hand sweep (0.6 degrees a frame) instead of stepping
     * once a second. The widgets do NOT use this — see
     * {@link #millisToNextSecond}.
     */
    static long millisToNextFrame(long nowMillis) {
        return FRAME_MILLIS - (nowMillis % FRAME_MILLIS);
    }

    /**
     * Milliseconds until the next whole second, for aligning the
     * widget tick. Used by the widget tick so every tick lands on a
     * real second boundary and drift never accumulates: 1000 at an
     * exact boundary, 500 halfway through a second, 1 just before
     * the next one. One tick a second makes the second hand step
     * 6 degrees a second — the 100 ms sweep ({@link #millisToNextFrame})
     * was tried on the widgets and Vivo OriginOS batches those alarms
     * into steps of about 5.5 seconds, so the widgets stay on the
     * one-second tick the user confirmed works on his phone. The
     * smooth sweep lives only on the launcher's own Home clock,
     * which redraws from a Handler, not from alarms.
     */
    static long millisToNextSecond(long nowMillis) {
        return TICK_MILLIS - (nowMillis % TICK_MILLIS);
    }

    // ---- Drawing ----

    /** Draws the whole watch face for the given moment (transparent background, no tile). */
    static Bitmap draw(int sizePx, long nowMillis) {
        return draw(sizePx, nowMillis, false);
    }

    /**
     * Draws the whole watch face for the given moment.
     *
     * @param blackTile true draws the black rounded-square tile behind
     *                  the dial (standalone Mechanical Clock widget,
     *                  reference picture image-6); false leaves the
     *                  background transparent so the white dial sits
     *                  straight on the wallpaper (combined Clock +
     *                  Weather widget, reference picture image-7).
     */
    static Bitmap draw(int sizePx, long nowMillis, boolean blackTile) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        drawOn(new Canvas(bitmap), sizePx, nowMillis, blackTile);
        return bitmap;
    }

    /**
     * Draws the whole watch face directly onto the given canvas, inside
     * a {@code sizePx} square starting at the canvas origin. The
     * launcher's Home clock View uses this so no Bitmap is allocated
     * per frame; the widgets use {@link #draw(int, long, boolean)},
     * which draws onto a fresh Bitmap through this same method.
     *
     * @param blackTile true draws the black rounded-square tile behind
     *                  the dial (standalone Mechanical Clock widget,
     *                  reference picture image-6); false leaves the
     *                  background transparent so the white dial sits
     *                  straight on the wallpaper (combined Clock +
     *                  Weather widget and the Home clock, reference
     *                  picture image-7).
     */
    static void drawOn(Canvas canvas, int sizePx, long nowMillis, boolean blackTile) {
        float cx = sizePx / 2f;
        float cy = sizePx / 2f;
        float s = sizePx / 480f; // all sizes below are in 480-space units

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);

        double minute = minuteAngle(nowMillis);
        double hour = hourAngle(nowMillis);
        double second = secondAngle(nowMillis);

        // Black rounded-square tile (standalone widget only), like the
        // reference picture image-6: black tile, white dial inside it.
        // Otherwise the background stays transparent and the white dial
        // sits straight on the wallpaper (reference picture image-7).
        if (blackTile) {
            fill.setColor(TILE);
            fill.setShadowLayer(16 * s, 0, 6 * s, 0x66000000);
            RectF tile = new RectF(10 * s, 10 * s, 470 * s, 470 * s);
            canvas.drawRoundRect(tile, 88 * s, 88 * s, fill);
            fill.setShadowLayer(0, 0, 0, 0);
        }

        // Plain white dial, with a soft shadow so it lifts off a light wallpaper.
        fill.setColor(DIAL);
        fill.setShadowLayer(14 * s, 0, 5 * s, 0x55000000);
        canvas.drawCircle(cx, cy, 200 * s, fill);
        fill.setShadowLayer(0, 0, 0, 0);

        // Minute track: 60 black ticks, hour ticks longer and thicker.
        for (int i = 0; i < 60; i++) {
            double a = Math.toRadians(i * 6.0);
            boolean hourTick = i % 5 == 0;
            float r1 = (hourTick ? 178 : 186) * s;
            float r2 = 196 * s;
            stroke.setColor(INK);
            stroke.setStrokeWidth((hourTick ? 5 : 2.5f) * s);
            canvas.drawLine((float) (cx + r1 * Math.sin(a)), (float) (cy - r1 * Math.cos(a)),
                    (float) (cx + r2 * Math.sin(a)), (float) (cy - r2 * Math.cos(a)), stroke);
        }

        // Black Arabic numerals 1-12, upright, like the picture.
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(INK);
        text.setTypeface(Typeface.SANS_SERIF);
        text.setFakeBoldText(true);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(44 * s);
        Paint.FontMetrics fm = text.getFontMetrics();
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30.0);
            float x = (float) (cx + 146 * s * Math.sin(a));
            float y = (float) (cy - 146 * s * Math.cos(a));
            // Centre the text vertically on (x, y).
            float baseline = y - (fm.ascent + fm.descent) / 2;
            canvas.drawText(NUMERALS[i], x, baseline, text);
        }

        // Thick black hour and minute hands on the real time.
        drawHand(canvas, fill, stroke, cx, cy, (float) hour, 108 * s, 22 * s, INK);
        drawHand(canvas, fill, stroke, cx, cy, (float) minute, 152 * s, 17 * s, INK);

        // Thin orange second hand with a short tail.
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate((float) second);
        stroke.setColor(ORANGE);
        stroke.setStrokeWidth(4 * s);
        canvas.drawLine(0, 20 * s, 0, -170 * s, stroke);
        canvas.restore();

        // Small orange centre dot with a thin black ring, like the picture.
        fill.setColor(ORANGE);
        canvas.drawCircle(cx, cy, 10 * s, fill);
        stroke.setColor(INK);
        stroke.setStrokeWidth(2.5f * s);
        canvas.drawCircle(cx, cy, 10 * s, stroke);
    }

    /** Tapered hand pointing up at 0 degrees (12 o'clock). */
    private static void drawHand(Canvas canvas, Paint fill, Paint stroke,
                                 float cx, float cy, float angleDeg,
                                 float length, float width, int color) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(angleDeg);
        Path path = new Path();
        path.moveTo(-width / 2, length * 0.18f);
        path.lineTo(-width / 2, -length * 0.72f);
        path.lineTo(0, -length);
        path.lineTo(width / 2, -length * 0.72f);
        path.lineTo(width / 2, length * 0.18f);
        path.close();
        fill.setColor(color);
        canvas.drawPath(path, fill);
        canvas.restore();
    }
}
