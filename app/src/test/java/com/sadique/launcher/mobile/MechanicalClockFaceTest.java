package com.sadique.launcher.mobile;

import org.junit.Test;
import java.util.Calendar;
import static org.junit.Assert.*;

public class MechanicalClockFaceTest {
    private static long time(int hour, int minute, int second, int millis) {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.NOVEMBER, 10, hour, minute, second);
        c.set(Calendar.MILLISECOND, millis);
        return c.getTimeInMillis();
    }

    @Test public void handsShowRealTime() {
        long t = time(10, 9, 30, 500);
        assertEquals(183.0, MechanicalClockFace.secondAngle(t), 0.001);
        assertEquals(57.05, MechanicalClockFace.minuteAngle(t), 0.001);
        assertEquals(304.754, MechanicalClockFace.hourAngle(t), 0.001);
    }

    @Test public void handsAtNoonAndThreeAndHalfPastSix() {
        assertEquals(0.0, MechanicalClockFace.hourAngle(time(12, 0, 0, 0)), 0.001);
        assertEquals(0.0, MechanicalClockFace.minuteAngle(time(12, 0, 0, 0)), 0.001);
        assertEquals(90.0, MechanicalClockFace.hourAngle(time(3, 0, 0, 0)), 0.001);
        assertEquals(195.0, MechanicalClockFace.hourAngle(time(6, 30, 0, 0)), 0.001);
        assertEquals(180.0, MechanicalClockFace.minuteAngle(time(6, 30, 0, 0)), 0.001);
    }

    @Test public void secondHandIsSmoothNotStepped() {
        // 100 ms apart the second hand must move 0.6 degrees, never jump —
        // ten frames a second make it sweep instead of stepping once a second.
        long t1 = time(10, 9, 30, 0);
        long t2 = time(10, 9, 30, 100);
        assertEquals(0.6, MechanicalClockFace.secondAngle(t2)
                - MechanicalClockFace.secondAngle(t1), 0.001);
    }

    @Test public void tickAlignsToNextFrame() {
        assertEquals(100, MechanicalClockFace.millisToNextFrame(1_000));
        assertEquals(50, MechanicalClockFace.millisToNextFrame(1_050));
        assertEquals(1, MechanicalClockFace.millisToNextFrame(1_099));
        assertEquals(100, MechanicalClockFace.millisToNextFrame(2_000));
        assertEquals(MechanicalClockFace.FRAME_MILLIS, 100);
    }

    @Test public void widgetTickAlignsToNextSecond() {
        assertEquals(1000, MechanicalClockFace.millisToNextSecond(1_000));
        assertEquals(500, MechanicalClockFace.millisToNextSecond(1_500));
        assertEquals(1, MechanicalClockFace.millisToNextSecond(1_999));
        assertEquals(1000, MechanicalClockFace.millisToNextSecond(2_000));
        assertEquals(MechanicalClockFace.TICK_MILLIS, 1000);
    }

    @Test public void widgetSecondHandStepsSixDegreesPerSecond() {
        // Widgets tick once a second: the second hand steps 6 degrees
        // a tick (Vivo batches the 100 ms sweep into ~5.5 s steps, so
        // the widgets stay on the one-second tick).
        long t1 = time(10, 9, 30, 0);
        long t2 = time(10, 9, 31, 0);
        assertEquals(6.0, MechanicalClockFace.secondAngle(t2)
                - MechanicalClockFace.secondAngle(t1), 0.001);
    }
}
