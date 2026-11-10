package com.sadique.launcher.mobile;

import org.junit.Test;
import java.util.Calendar;
import static org.junit.Assert.*;

/**
 * Pins the sweep contract the launcher's own Home analog clock
 * (HomeAnalogClockView, redrawn by HomeActivity's Handler loop)
 * relies on: 100 ms frames, so the second hand glides 0.6 degrees a
 * frame instead of stepping once a second — and the minute/hour hands
 * move smoothly too. The frame math itself lives in
 * MechanicalClockFace; this test pins it from the Home clock's side
 * so a future change to the frame interval cannot silently break the
 * sweep. The Handler loop's lifecycle (start in onResume, stop in
 * onPause) is checked statically, not here — unit tests cannot drive
 * an Activity lifecycle.
 */
public class HomeAnalogClockTest {
    private static long time(int hour, int minute, int second, int millis) {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.NOVEMBER, 10, hour, minute, second);
        c.set(Calendar.MILLISECOND, millis);
        return c.getTimeInMillis();
    }

    @Test public void sweepFramesAre100ms() {
        assertEquals(100, MechanicalClockFace.FRAME_MILLIS);
        // Every frame lands inside one 100 ms window — the Handler loop
        // never waits longer than a frame, so the hand glides.
        for (long t = 0; t < 1000; t += 37) {
            long delay = MechanicalClockFace.millisToNextFrame(t);
            assertTrue(delay > 0 && delay <= 100);
        }
    }

    @Test public void secondHandGlidesEachFrame() {
        long t1 = time(10, 9, 30, 0);
        long t2 = time(10, 9, 30, 100);
        assertEquals(0.6, MechanicalClockFace.secondAngle(t2)
                - MechanicalClockFace.secondAngle(t1), 0.001);
        // The minute and hour hands move too (smoothly, not stepped).
        assertTrue(MechanicalClockFace.minuteAngle(t2) > MechanicalClockFace.minuteAngle(t1));
        assertTrue(MechanicalClockFace.hourAngle(t2) > MechanicalClockFace.hourAngle(t1));
    }
}
