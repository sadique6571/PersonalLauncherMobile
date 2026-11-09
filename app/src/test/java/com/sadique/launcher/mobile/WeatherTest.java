package com.sadique.launcher.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class WeatherTest {
    private static final String SAMPLE="{\"current\":{\"time\":\"2026-11-09T15:00\",\"temperature_2m\":24.3,\"weather_code\":2}}";
    @Test public void summaryFormatsPlaceTemperatureAndCondition() {
        assertEquals("Okara · 24° · Partly cloudy",Weather.summary(SAMPLE,"Okara"));
        assertEquals("Current location · 24° · Partly cloudy",Weather.summary(SAMPLE,"Current location"));
    }
    @Test public void summaryIsNullWithoutAReading() {
        assertNull(Weather.summary("{}","Okara"));
        assertNull(Weather.summary(null,"Okara"));
        assertNull(Weather.summary("{\"current\":{\"temperature_2m\":24.3}}","Okara"));
    }
    @Test public void urlUsesGivenCoordinates() {
        assertTrue(Weather.url(31.5,74.3).contains("latitude=31.5"));
        assertTrue(Weather.url(31.5,74.3).contains("longitude=74.3"));
        assertTrue(Weather.url(Weather.OKARA_LATITUDE,Weather.OKARA_LONGITUDE).contains("latitude=30.81"));
    }
    @Test public void describeCoversCommonCodes() {
        assertEquals("Clear",Weather.describe(0));
        assertEquals("Rain",Weather.describe(63));
        assertEquals("Thunderstorm",Weather.describe(95));
        assertEquals("—",Weather.describe(99));
    }
}
