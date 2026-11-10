package com.sadique.launcher.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
public class WeatherTest {
    private static final String SAMPLE="{\"current\":{\"time\":\"2026-11-09T15:00\",\"temperature_2m\":24.3,\"weather_code\":2}}";
    private static final String DAILY="{\"current\":{\"time\":\"2026-11-09T15:00\",\"temperature_2m\":24.3,\"weather_code\":2},\"daily\":{\"time\":[\"2026-11-09\"],\"temperature_2m_max\":[26.1],\"temperature_2m_min\":[18.4]}}";
    private static final String DETAIL="{\"current\":{\"time\":\"2026-11-09T15:00\",\"temperature_2m\":24.3,\"relative_humidity_2m\":62,\"apparent_temperature\":26.1,\"precipitation\":0.2,\"weather_code\":2,\"wind_speed_10m\":12.4,\"wind_direction_10m\":315,\"pressure_msl\":1013.2,\"cloud_cover\":20},\"hourly\":{\"time\":[\"2026-11-09T14:00\",\"2026-11-09T15:00\",\"2026-11-09T16:00\",\"2026-11-09T17:00\"],\"temperature_2m\":[23.1,24.3,25.0,24.0],\"weather_code\":[1,2,2,3],\"precipitation_probability\":[10,20,null,30]},\"daily\":{\"time\":[\"2026-11-09\",\"2026-11-10\",\"2026-11-11\"],\"weather_code\":[2,3,0],\"temperature_2m_max\":[26.1,25.0,27.0],\"temperature_2m_min\":[18.4,17.0,19.0],\"sunrise\":[\"2026-11-09T06:23\",\"2026-11-10T06:24\",\"2026-11-11T06:25\"],\"sunset\":[\"2026-11-09T17:20\",\"2026-11-10T17:19\",\"2026-11-11T17:18\"],\"precipitation_probability_max\":[40,10,0],\"uv_index_max\":[5.2,4.8,6.0]}}";
    @Test public void summaryFormatsPlaceTemperatureAndCondition() {
        assertEquals("Okara · 24° · Partly cloudy",Weather.summary(SAMPLE,"Okara"));
        assertEquals("Current location · 24° · Partly cloudy",Weather.summary(SAMPLE,"Current location"));
    }
    @Test public void summaryIsNullWithoutAReading() {
        assertNull(Weather.summary("{}", "Okara"));
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
    @Test public void dailyUrlAsksForTodayHighLow() {
        assertTrue(Weather.dailyUrl(31.5,74.3).contains("daily=temperature_2m_max,temperature_2m_min"));
        assertTrue(Weather.dailyUrl(31.5,74.3).contains("forecast_days=1"));
        assertTrue(Weather.dailyUrl(31.5,74.3).contains("current=temperature_2m,weather_code"));
    }
    @Test public void temperatureConditionHighLow() {
        assertEquals("24°",Weather.temperature(DAILY));
        assertEquals("Partly cloudy",Weather.condition(DAILY));
        assertEquals("High 26° · Low 18°",Weather.highLow(DAILY));
        assertNull(Weather.temperature("{}"));
        assertNull(Weather.condition("{}"));
        assertNull(Weather.highLow("{}"));
        assertNull(Weather.highLow("{\"daily\":{\"temperature_2m_max\":[26.1]}}"));
    }
    @Test public void detailUrlAsksForAllFields() {
        String u=Weather.detailUrl(31.5,74.3);
        assertTrue(u.contains("relative_humidity_2m"));
        assertTrue(u.contains("apparent_temperature"));
        assertTrue(u.contains("wind_speed_10m"));
        assertTrue(u.contains("pressure_msl"));
        assertTrue(u.contains("cloud_cover"));
        assertTrue(u.contains("hourly=temperature_2m,weather_code,precipitation_probability"));
        assertTrue(u.contains("sunrise,sunset"));
        assertTrue(u.contains("uv_index_max"));
        assertTrue(u.contains("forecast_days=7"));
    }
    @Test public void detailFieldsParse() {
        assertEquals("26°",Weather.feelsLike(DETAIL));
        assertEquals("62%",Weather.humidity(DETAIL));
        assertEquals("12 km/h NW",Weather.wind(DETAIL));
        assertEquals("1013 hPa",Weather.pressure(DETAIL));
        assertEquals("20%",Weather.cloudCover(DETAIL));
        assertEquals("0.2 mm",Weather.precipitation(DETAIL));
        assertEquals("40%",Weather.rainChance(DETAIL));
        assertEquals("5.2",Weather.uvIndex(DETAIL));
        assertNull(Weather.feelsLike("{}"));
        assertNull(Weather.humidity("{}"));
        assertNull(Weather.wind("{}"));
        assertNull(Weather.pressure("{}"));
        assertNull(Weather.cloudCover("{}"));
        assertNull(Weather.rainChance("{}"));
        assertNull(Weather.uvIndex("{}"));
    }
    @Test public void windCompassCoversEightPoints() {
        assertEquals("N",Weather.windCompass(0));
        assertEquals("N",Weather.windCompass(359));
        assertEquals("NE",Weather.windCompass(45));
        assertEquals("E",Weather.windCompass(90));
        assertEquals("SE",Weather.windCompass(135));
        assertEquals("S",Weather.windCompass(180));
        assertEquals("SW",Weather.windCompass(225));
        assertEquals("W",Weather.windCompass(270));
        assertEquals("NW",Weather.windCompass(315));
        assertNull(Weather.windCompass(Double.NaN));
    }
    @Test public void timeFormatsAreTwelveHour() {
        assertEquals("6:23 AM",Weather.formatTime12("2026-11-09T06:23"));
        assertEquals("5:20 PM",Weather.formatTime12("2026-11-09T17:20"));
        assertEquals("12:00 AM",Weather.formatTime12("2026-11-09T00:00"));
        assertEquals("12:00 PM",Weather.formatTime12("2026-11-09T12:00"));
        assertNull(Weather.formatTime12("not-a-time"));
        assertEquals("3 PM",Weather.hourLabel("2026-11-09T15:00"));
        assertEquals("3:30 PM",Weather.hourLabel("2026-11-09T15:30"));
    }
    @Test public void sunEventBeforeSunriseShowsSunrise() {
        String morning=DETAIL.replace("\"time\":\"2026-11-09T15:00\"","\"time\":\"2026-11-09T05:00\"");
        Weather.SunEvent e=Weather.sunEvent(morning);
        assertNotNull(e);
        assertTrue(e.sunrise);
        assertFalse(e.tomorrow);
        assertEquals("Sunrise 6:23 AM",e.label);
    }
    @Test public void sunEventMiddayShowsSunset() {
        Weather.SunEvent e=Weather.sunEvent(DETAIL);
        assertNotNull(e);
        assertFalse(e.sunrise);
        assertFalse(e.tomorrow);
        assertEquals("Sunset 5:20 PM",e.label);
    }
    @Test public void sunEventAfterSunsetShowsTomorrowSunrise() {
        String night=DETAIL.replace("\"time\":\"2026-11-09T15:00\"","\"time\":\"2026-11-09T19:00\"");
        Weather.SunEvent e=Weather.sunEvent(night);
        assertNotNull(e);
        assertTrue(e.sunrise);
        assertTrue(e.tomorrow);
        assertEquals("Sunrise tomorrow · 6:24 AM",e.label);
    }
    @Test public void sunEventIsNullWithoutData() {
        assertNull(Weather.sunEvent("{}"));
        assertNull(Weather.sunEvent(null));
    }
    @Test public void hourlyStartsAtCurrentHour() {
        List<Weather.Hour> hours=Weather.hourly(DETAIL,12);
        assertEquals(3,hours.size());
        assertEquals("3 PM",hours.get(0).label);
        assertEquals("24°",hours.get(0).temp);
        assertEquals(2,hours.get(0).code);
        assertEquals("20%",hours.get(0).rain);
        assertEquals("4 PM",hours.get(1).label);
        assertEquals("—",hours.get(1).rain);
        assertEquals("5 PM",hours.get(2).label);
    }
    @Test public void dailyListsSevenDaysWithNames() {
        List<Weather.Day> days=Weather.daily(DETAIL);
        assertEquals(3,days.size());
        assertEquals("Today",days.get(0).name);
        assertEquals(2,days.get(0).code);
        assertEquals("26°",days.get(0).high);
        assertEquals("18°",days.get(0).low);
        assertEquals("40%",days.get(0).rain);
        assertEquals("Mon",days.get(1).name);
        assertEquals("Wed",days.get(2).name);
    }
    @Test public void conditionIconMapsCodes() {
        assertEquals(R.drawable.ic_weather_clear,Weather.conditionIcon(0));
        assertEquals(R.drawable.ic_weather_partly,Weather.conditionIcon(1));
        assertEquals(R.drawable.ic_weather_partly,Weather.conditionIcon(2));
        assertEquals(R.drawable.ic_weather_cloudy,Weather.conditionIcon(3));
        assertEquals(R.drawable.ic_weather_fog,Weather.conditionIcon(45));
        assertEquals(R.drawable.ic_weather_rain,Weather.conditionIcon(63));
        assertEquals(R.drawable.ic_weather_snow,Weather.conditionIcon(71));
        assertEquals(R.drawable.ic_weather_storm,Weather.conditionIcon(95));
        assertNotEquals(Weather.conditionIcon(0),Weather.conditionIcon(3));
    }
}
