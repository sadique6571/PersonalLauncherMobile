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
        assertEquals(10, MechanicalClockFace.dayOfMonth(t));
    }

    @Test public void handsAtNoonAndThreeAndHalfPastSix() {
        assertEquals(0.0, MechanicalClockFace.hourAngle(time(12, 0, 0, 0)), 0.001);
        assertEquals(0.0, MechanicalClockFace.minuteAngle(time(12, 0, 0, 0)), 0.001);
        assertEquals(90.0, MechanicalClockFace.hourAngle(time(3, 0, 0, 0)), 0.001);
        assertEquals(195.0, MechanicalClockFace.hourAngle(time(6, 30, 0, 0)), 0.001);
        assertEquals(180.0, MechanicalClockFace.minuteAngle(time(6, 30, 0, 0)), 0.001);
    }

    @Test public void meshingGearsTurnOppositeByToothRatio() {
        // 48-tooth minute wheel at 360 degrees drives a 40-tooth wheel
        // the other way, 48/40 as fast: -432 degrees, normalised to 288.
        assertEquals(288.0, MechanicalClockFace.gearAngle(360.0, 48, 40, true), 0.001);
        // Same direction, 48 teeth driving 20: 90 * 48/20 = 216.
        assertEquals(216.0, MechanicalClockFace.gearAngle(90.0, 48, 20, false), 0.001);
        assertEquals(0.0, MechanicalClockFace.gearAngle(0.0, 48, 40, true), 0.001);
    }

    @Test public void balanceWheelOscillates() {
        assertEquals(0.0, MechanicalClockFace.balanceAngle(0L), 0.001);
        assertEquals(40.0, MechanicalClockFace.balanceAngle(125L), 0.001);
        assertEquals(0.0, MechanicalClockFace.balanceAngle(250L), 0.001);
        assertEquals(-40.0, MechanicalClockFace.balanceAngle(375L), 0.001);
    }

    @Test public void escapeWheelStepsOneToothPerBeat() {
        assertEquals(0.0, MechanicalClockFace.escapeAngle(0L, 15), 0.001);
        assertEquals(24.0, MechanicalClockFace.escapeAngle(200L, 15), 0.001);
        assertEquals(120.0, MechanicalClockFace.escapeAngle(1000L, 15), 0.001);
        // 15 beats make a full turn, so it wraps back to 0.
        assertEquals(0.0, MechanicalClockFace.escapeAngle(3000L, 15), 0.001);
    }
}
