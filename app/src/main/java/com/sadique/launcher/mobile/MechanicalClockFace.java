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
 * 9. Mechanical Clock — skeleton watch face drawn in code on a Bitmap.
 *
 * The look follows the demo image the user picked: a dark skeleton dial,
 * Roman numerals, steel + gold gears, a balance wheel with hairspring,
 * ruby jewels and blued steel hands. Everything is drawn with Canvas
 * (no XML drawables), so the gears really turn: each gear's angle comes
 * from the real time through its tooth ratio, the balance wheel
 * oscillates on a sine wave and the escape wheel steps one tooth per
 * beat, exactly like a mechanical movement.
 *
 * The face is redrawn once a second by {@link MechanicalClockWidget}.
 * Only the angle math is unit-tested (it uses no Android classes);
 * {@link #draw(int, long)} needs a device or CI.
 */
final class MechanicalClockFace {
    /** Bitmap edge in pixels; the widget ImageView scales it to fit. */
    static final int BITMAP_SIZE_PX = 480;

    private static final int STEEL = 0xFF9AA1A8;
    private static final int STEEL_EDGE = 0xFF6E757C;
    private static final int STEEL_DARK = 0xFF565C63;
    private static final int GOLD = 0xFFC9A227;
    private static final int GOLD_EDGE = 0xFF8C6D1F;
    private static final int GOLD_LIGHT = 0xFFE3C565;
    private static final int RUBY = 0xFFD81B60;
    private static final int RUBY_LIGHT = 0xFFF06292;
    private static final int BLUED = 0xFF1E3A8A;
    private static final int BLUED_EDGE = 0xFF16265E;
    private static final int DIAL = 0xFF17191C;
    private static final int PLATE = 0xFF101215;
    private static final int NUMERAL = 0xFFC7CCD1;
    private static final int DATE_BG = 0xFFD8DDE2;
    private static final int DATE_TEXT = 0xFF1A1D20;

    private static final String[] ROMAN = {
            "XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"
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

    /** Day of the month, for the date window at 3 o'clock. */
    static int dayOfMonth(long nowMillis) {
        return calendar(nowMillis).get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Angle of a driven gear meshing with a driver gear. Meshing gears
     * turn in opposite directions and their angles scale by the tooth
     * ratio, so a 48-tooth minute wheel driving a 40-tooth wheel turns
     * it 48/40 as fast. Result is normalised to [0, 360).
     */
    static double gearAngle(double driverAngle, int driverTeeth, int teeth, boolean reverse) {
        double angle = driverAngle * driverTeeth / (double) teeth;
        if (reverse) angle = -angle;
        return ((angle % 360.0) + 360.0) % 360.0;
    }

    /** Balance wheel swing, +/-40 degrees on a 2 Hz beat (500 ms period). */
    static double balanceAngle(long nowMillis) {
        return 40.0 * Math.sin(2 * Math.PI * (nowMillis % 500L) / 500.0);
    }

    /**
     * Escape wheel angle: it does not sweep, it steps one tooth per
     * beat (5 beats per second), like a real escapement.
     */
    static double escapeAngle(long nowMillis, int teeth) {
        long beats = nowMillis / 200L;
        return (beats % teeth) * (360.0 / teeth);
    }

    private static Calendar calendar(long nowMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(nowMillis);
        return c;
    }

    private static int shade(int color, float factor) {
        return Color.argb(Color.alpha(color),
                Math.min(255, Math.round(Color.red(color) * factor)),
                Math.min(255, Math.round(Color.green(color) * factor)),
                Math.min(255, Math.round(Color.blue(color) * factor)));
    }

    // ---- Drawing ----

    /** Draws the whole watch face for the given moment; transparent outside the case. */
    static Bitmap draw(int sizePx, long nowMillis) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float cx = sizePx / 2f;
        float cy = sizePx / 2f;
        float s = sizePx / 480f; // all radii below are in 480-space units

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);

        double minute = minuteAngle(nowMillis);
        double hour = hourAngle(nowMillis);
        double second = secondAngle(nowMillis);

        // Case: brushed steel outer ring, then the dark movement plate.
        fill.setColor(0xFF2B2F34);
        canvas.drawCircle(cx, cy, 235 * s, fill);
        stroke.setColor(0xFF565C63);
        stroke.setStrokeWidth(3 * s);
        canvas.drawCircle(cx, cy, 232 * s, stroke);
        stroke.setColor(0xFF7A8187);
        stroke.setStrokeWidth(2 * s);
        canvas.drawCircle(cx, cy, 218 * s, stroke);
        fill.setColor(PLATE);
        canvas.drawCircle(cx, cy, 214 * s, fill);
        fill.setColor(DIAL);
        canvas.drawCircle(cx, cy, 206 * s, fill);

        // Faint brushed circles on the plate.
        stroke.setColor(Color.argb(18, 255, 255, 255));
        stroke.setStrokeWidth(2 * s);
        canvas.drawCircle(cx, cy, 190 * s, stroke);
        canvas.drawCircle(cx, cy, 150 * s, stroke);
        canvas.drawCircle(cx, cy, 110 * s, stroke);

        // --- Gear train (all angles from the real time) ---
        // Mainspring barrel, top-left: turns once per 8 hours.
        drawBarrel(canvas, fill, stroke, cx - 100 * s, cy - 78 * s, 48 * s, (float) (hour / 8.0));
        // Balance wheel with hairspring, top centre: oscillates.
        drawBalance(canvas, fill, stroke, cx, cy - 108 * s, 52 * s, (float) balanceAngle(nowMillis));
        // Centre gold minute wheel: one turn per hour.
        drawGear(canvas, fill, stroke, cx, cy, 64 * s, 48, (float) minute, GOLD, GOLD_EDGE, 6, 7 * s);
        // Two steel wheels meshing with the centre wheel, lower left/right.
        float steelAngle = (float) gearAngle(minute, 48, 40, true);
        drawGear(canvas, fill, stroke, cx - 96 * s, cy + 82 * s, 56 * s, 40, steelAngle, STEEL, STEEL_EDGE, 5, 6 * s);
        drawGear(canvas, fill, stroke, cx + 96 * s, cy + 82 * s, 56 * s, 40, steelAngle, STEEL, STEEL_EDGE, 5, 6 * s);
        // Small gold pinion, upper right.
        drawGear(canvas, fill, stroke, cx + 100 * s, cy - 66 * s, 30 * s, 20,
                (float) gearAngle(minute, 48, 20, true), GOLD, GOLD_EDGE, 4, 4 * s);
        // Escape wheel, bottom centre: steps one tooth per beat.
        drawEscapeWheel(canvas, fill, stroke, cx, cy + 142 * s, 26 * s, 15,
                (float) escapeAngle(nowMillis, 15));

        // Gold screws around the chapter ring.
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0 + 22.5);
            drawScrew(canvas, fill, stroke,
                    (float) (cx + 192 * s * Math.sin(a)), (float) (cy - 192 * s * Math.cos(a)),
                    9 * s, (float) (i * 45.0));
        }

        // Minute track: 60 ticks, hour ticks longer.
        for (int i = 0; i < 60; i++) {
            double a = Math.toRadians(i * 6.0);
            boolean hourTick = i % 5 == 0;
            float r1 = (hourTick ? 196 : 202) * s;
            float r2 = 210 * s;
            stroke.setColor(hourTick ? NUMERAL : 0xFF8A9096);
            stroke.setStrokeWidth((hourTick ? 4 : 2) * s);
            canvas.drawLine((float) (cx + r1 * Math.sin(a)), (float) (cy - r1 * Math.cos(a)),
                    (float) (cx + r2 * Math.sin(a)), (float) (cy - r2 * Math.cos(a)), stroke);
        }

        // Roman numerals, tops pointing outward. III is replaced by the date window.
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(NUMERAL);
        text.setTypeface(Typeface.SERIF);
        text.setFakeBoldText(true);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(36 * s);
        for (int i = 0; i < 12; i++) {
            if (i == 3) continue; // date window sits at 3 o'clock
            double a = Math.toRadians(i * 30.0);
            float x = (float) (cx + 168 * s * Math.sin(a));
            float y = (float) (cy - 168 * s * Math.cos(a));
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(i * 30f);
            canvas.drawText(ROMAN[i], 0, 12 * s, text);
            canvas.restore();
        }

        // Date window at 3 o'clock with today's date.
        RectF window = new RectF(cx + 128 * s, cy - 18 * s, cx + 172 * s, cy + 18 * s);
        fill.setColor(DATE_BG);
        canvas.drawRoundRect(window, 6 * s, 6 * s, fill);
        stroke.setColor(0xFF5A6067);
        stroke.setStrokeWidth(2 * s);
        canvas.drawRoundRect(window, 6 * s, 6 * s, stroke);
        text.setColor(DATE_TEXT);
        text.setTextSize(26 * s);
        canvas.drawText(String.valueOf(dayOfMonth(nowMillis)), cx + 150 * s, cy + 9 * s, text);

        // Blued steel hands on the real time.
        drawHand(canvas, fill, stroke, cx, cy, (float) hour, 118 * s, 15 * s, BLUED, BLUED_EDGE);
        drawHand(canvas, fill, stroke, cx, cy, (float) minute, 158 * s, 11 * s, BLUED, BLUED_EDGE);
        drawSecondHand(canvas, stroke, cx, cy, (float) second, 182 * s, s);

        // Centre cap.
        fill.setColor(STEEL);
        canvas.drawCircle(cx, cy, 17 * s, fill);
        stroke.setColor(STEEL_EDGE);
        stroke.setStrokeWidth(2 * s);
        canvas.drawCircle(cx, cy, 17 * s, stroke);
        fill.setColor(GOLD_LIGHT);
        canvas.drawCircle(cx, cy, 6 * s, fill);

        // Soft glass glare, top-left.
        stroke.setColor(Color.argb(28, 255, 255, 255));
        stroke.setStrokeWidth(14 * s);
        canvas.drawArc(new RectF(cx - 200 * s, cy - 200 * s, cx + 200 * s, cy + 200 * s),
                200, 60, false, stroke);
        return bitmap;
    }

    /** Toothed wheel with spokes, hub and a ruby jewel at its centre. */
    private static void drawGear(Canvas canvas, Paint fill, Paint stroke,
                                 float cx, float cy, float radius, int teeth, float angleDeg,
                                 int color, int edgeColor, int spokes, float spokeWidth) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(angleDeg);
        float toothDepth = radius * 0.12f;
        float outer = radius;
        float inner = radius - toothDepth;
        Path path = new Path();
        for (int i = 0; i < teeth; i++) {
            double step = 2 * Math.PI / teeth;
            double a = i * step;
            float[] xs = {
                    (float) (inner * Math.sin(a)),
                    (float) (outer * Math.sin(a + step * 0.18)),
                    (float) (outer * Math.sin(a + step * 0.42)),
                    (float) (inner * Math.sin(a + step * 0.60))
            };
            float[] ys = {
                    (float) (-inner * Math.cos(a)),
                    (float) (-outer * Math.cos(a + step * 0.18)),
                    (float) (-outer * Math.cos(a + step * 0.42)),
                    (float) (-inner * Math.cos(a + step * 0.60))
            };
            if (i == 0) path.moveTo(xs[0], ys[0]);
            else path.lineTo(xs[0], ys[0]);
            path.lineTo(xs[1], ys[1]);
            path.lineTo(xs[2], ys[2]);
            path.lineTo(xs[3], ys[3]);
        }
        path.close();
        fill.setColor(color);
        canvas.drawPath(path, fill);
        stroke.setColor(edgeColor);
        stroke.setStrokeWidth(radius * 0.03f);
        canvas.drawPath(path, stroke);
        // Inner disc, slightly darker, so the teeth stand out.
        fill.setColor(shade(color, 0.82f));
        canvas.drawCircle(0, 0, inner * 0.86f, fill);
        // Spokes.
        stroke.setColor(edgeColor);
        stroke.setStrokeWidth(spokeWidth);
        for (int i = 0; i < spokes; i++) {
            double a = i * 2 * Math.PI / spokes;
            canvas.drawLine((float) (radius * 0.20 * Math.sin(a)), (float) (-radius * 0.20 * Math.cos(a)),
                    (float) (radius * 0.78 * Math.sin(a)), (float) (-radius * 0.78 * Math.cos(a)), stroke);
        }
        // Hub + ruby jewel.
        fill.setColor(edgeColor);
        canvas.drawCircle(0, 0, radius * 0.20f, fill);
        fill.setColor(RUBY);
        canvas.drawCircle(0, 0, radius * 0.12f, fill);
        fill.setColor(RUBY_LIGHT);
        canvas.drawCircle(-radius * 0.04f, -radius * 0.04f, radius * 0.04f, fill);
        canvas.restore();
    }

    /** Mainspring barrel: a drum with a coiled spring visible inside. */
    private static void drawBarrel(Canvas canvas, Paint fill, Paint stroke,
                                   float cx, float cy, float radius, float angleDeg) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(angleDeg);
        fill.setColor(STEEL_DARK);
        canvas.drawCircle(0, 0, radius, fill);
        stroke.setColor(STEEL_EDGE);
        stroke.setStrokeWidth(radius * 0.06f);
        canvas.drawCircle(0, 0, radius, stroke);
        // Coiled mainspring (Archimedean spiral).
        stroke.setColor(GOLD_LIGHT);
        stroke.setStrokeWidth(radius * 0.07f);
        drawSpiral(canvas, stroke, 0, 0, radius * 0.78f, 4);
        fill.setColor(GOLD);
        canvas.drawCircle(0, 0, radius * 0.14f, fill);
        canvas.restore();
    }

    /** Balance wheel: a ring that swings back and forth over a fixed hairspring. */
    private static void drawBalance(Canvas canvas, Paint fill, Paint stroke,
                                    float cx, float cy, float radius, float angleDeg) {
        // Hairspring stays fixed; only the wheel swings.
        stroke.setColor(0xFFB9C0C7);
        stroke.setStrokeWidth(radius * 0.03f);
        drawSpiral(canvas, stroke, cx, cy, radius * 0.72f, 5);
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(angleDeg);
        stroke.setColor(STEEL);
        stroke.setStrokeWidth(radius * 0.11f);
        canvas.drawCircle(0, 0, radius * 0.88f, stroke);
        stroke.setColor(STEEL_EDGE);
        stroke.setStrokeWidth(radius * 0.05f);
        canvas.drawLine(-radius * 0.88f, 0, radius * 0.88f, 0, stroke);
        fill.setColor(RUBY);
        canvas.drawCircle(0, 0, radius * 0.16f, fill);
        fill.setColor(RUBY_LIGHT);
        canvas.drawCircle(-radius * 0.05f, -radius * 0.05f, radius * 0.05f, fill);
        canvas.restore();
    }

    /** Escape wheel: pointed teeth that step one tooth per beat, not sweep. */
    private static void drawEscapeWheel(Canvas canvas, Paint fill, Paint stroke,
                                        float cx, float cy, float radius, int teeth, float angleDeg) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(angleDeg);
        Path path = new Path();
        for (int i = 0; i < teeth; i++) {
            double step = 2 * Math.PI / teeth;
            double a = i * step;
            float x0 = (float) (radius * 0.82 * Math.sin(a));
            float y0 = (float) (-radius * 0.82 * Math.cos(a));
            float x1 = (float) (radius * Math.sin(a + step * 0.5));
            float y1 = (float) (-radius * Math.cos(a + step * 0.5));
            float x2 = (float) (radius * 0.82 * Math.sin(a + step));
            float y2 = (float) (-radius * 0.82 * Math.cos(a + step));
            if (i == 0) path.moveTo(x0, y0);
            else path.lineTo(x0, y0);
            path.lineTo(x1, y1);
            path.lineTo(x2, y2);
        }
        path.close();
        fill.setColor(STEEL);
        canvas.drawPath(path, fill);
        stroke.setColor(STEEL_EDGE);
        stroke.setStrokeWidth(radius * 0.05f);
        canvas.drawPath(path, stroke);
        fill.setColor(STEEL_DARK);
        canvas.drawCircle(0, 0, radius * 0.30f, fill);
        fill.setColor(RUBY);
        canvas.drawCircle(0, 0, radius * 0.16f, fill);
        canvas.restore();
    }

    /** Gold screw with a slot. */
    private static void drawScrew(Canvas canvas, Paint fill, Paint stroke,
                                  float cx, float cy, float radius, float slotDeg) {
        fill.setColor(GOLD);
        canvas.drawCircle(cx, cy, radius, fill);
        stroke.setColor(GOLD_EDGE);
        stroke.setStrokeWidth(radius * 0.12f);
        canvas.drawCircle(cx, cy, radius, stroke);
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(slotDeg);
        stroke.setColor(GOLD_EDGE);
        stroke.setStrokeWidth(radius * 0.22f);
        canvas.drawLine(-radius * 0.7f, 0, radius * 0.7f, 0, stroke);
        canvas.restore();
    }

    /** Archimedean spiral, centred where asked. */
    private static void drawSpiral(Canvas canvas, Paint stroke, float cx, float cy,
                                   float maxRadius, int turns) {
        Path path = new Path();
        int steps = 120;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float angle = (float) (t * turns * 2 * Math.PI);
            float r = maxRadius * (0.15f + 0.85f * t);
            float x = cx + r * (float) Math.cos(angle);
            float y = cy + r * (float) Math.sin(angle);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        canvas.drawPath(path, stroke);
    }

    /** Tapered hand pointing up at 0 degrees (12 o'clock). */
    private static void drawHand(Canvas canvas, Paint fill, Paint stroke,
                                 float cx, float cy, float angleDeg,
                                 float length, float width, int color, int edgeColor) {
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
        stroke.setColor(edgeColor);
        stroke.setStrokeWidth(width * 0.12f);
        canvas.drawPath(path, stroke);
        canvas.restore();
    }

    /** Slim second hand with a counterweight tail. */
    private static void drawSecondHand(Canvas canvas, Paint stroke,
                                       float cx, float cy, float angleDeg, float length, float s) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(angleDeg);
        stroke.setColor(BLUED);
        stroke.setStrokeWidth(5 * s);
        canvas.drawLine(0, length * 0.16f, 0, -length, stroke);
        stroke.setColor(GOLD);
        stroke.setStrokeWidth(3 * s);
        canvas.drawLine(0, length * 0.16f, 0, -length * 0.92f, stroke);
        canvas.restore();
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(BLUED);
        canvas.drawCircle(cx, cy, 8 * s, fill);
    }
}
