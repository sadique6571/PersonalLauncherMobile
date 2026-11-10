package com.sadique.launcher.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetKitTest {
    @Test public void updatePeriodIsThirtyMinutes() {
        assertEquals(1800000, WidgetKit.UPDATE_PERIOD_MILLIS);
    }
    @Test public void weatherTextNeverBlank() {
        assertEquals("Current location · 24° · Clear", WidgetKit.weatherText("Current location · 24° · Clear", "Okara · 20° · Rain"));
        assertEquals("Okara · 20° · Rain", WidgetKit.weatherText(null, "Okara · 20° · Rain"));
        assertEquals("Okara · 20° · Rain", WidgetKit.weatherText("", "Okara · 20° · Rain"));
        assertEquals("Weather unavailable", WidgetKit.weatherText(null, ""));
        assertEquals("Weather unavailable", WidgetKit.weatherText(null, null));
        assertEquals("Weather unavailable", WidgetKit.weatherText("", ""));
    }
    @Test public void weatherSummaryFeedsWidgetText() {
        String sample = "{\"current\":{\"time\":\"2026-11-09T15:00\",\"temperature_2m\":24.3,\"weather_code\":2}}";
        assertEquals("Current location · 24° · Partly cloudy", WidgetKit.weatherText(Weather.summary(sample, "Current location"), ""));
        assertEquals("Weather unavailable", WidgetKit.weatherText(Weather.summary("{}", "Okara"), ""));
    }
    @Test public void highLowFeedsWeatherWidget() {
        String daily = "{\"current\":{\"time\":\"2026-11-09T15:00\",\"temperature_2m\":24.3,\"weather_code\":2},\"daily\":{\"time\":[\"2026-11-09\"],\"temperature_2m_max\":[26.1],\"temperature_2m_min\":[18.4]}}";
        assertEquals("24°", Weather.temperature(daily));
        assertEquals("Partly cloudy", Weather.condition(daily));
        assertEquals("High 26° · Low 18°", Weather.highLow(daily));
    }
    @Test public void detailLinesNeverBlank() {
        String detail = "{\"current\":{\"time\":\"2026-11-09T15:00\",\"temperature_2m\":24.3,\"relative_humidity_2m\":62,\"apparent_temperature\":26.1,\"precipitation\":0.2,\"weather_code\":2,\"wind_speed_10m\":12.4,\"wind_direction_10m\":315,\"pressure_msl\":1013.2,\"cloud_cover\":20},\"daily\":{\"time\":[\"2026-11-09\",\"2026-11-10\"],\"temperature_2m_max\":[26.1,25.0],\"temperature_2m_min\":[18.4,17.0],\"sunrise\":[\"2026-11-09T06:23\",\"2026-11-10T06:24\"],\"sunset\":[\"2026-11-09T17:20\",\"2026-11-10T17:19\"],\"precipitation_probability_max\":[40,10],\"uv_index_max\":[5.2,4.8]}}";
        assertEquals("Feels like 26°", WidgetKit.feelsLine(detail));
        assertEquals("Wind 12 km/h NW", WidgetKit.windLine(detail));
        assertEquals("High 26° · Low 18°", WidgetKit.highLowLine(detail));
        assertEquals("Humidity 62%", WidgetKit.humidityLine(detail));
        assertEquals("Rain 40%", WidgetKit.rainLine(detail));
        assertEquals("UV 5.2", WidgetKit.uvLine(detail));
        assertEquals("Pressure 1013 hPa", WidgetKit.pressureLine(detail));
        assertEquals("Cloud 20%", WidgetKit.cloudLine(detail));
        assertEquals("Sunset 5:20 PM", WidgetKit.sunLine(detail));
        assertEquals(R.drawable.ic_sunset, WidgetKit.sunIcon(detail));
        assertEquals("Feels like —", WidgetKit.feelsLine("{}"));
        assertEquals("Wind —", WidgetKit.windLine(null));
        assertEquals("High — · Low —", WidgetKit.highLowLine("{}"));
        assertEquals("Sunrise/Sunset —", WidgetKit.sunLine("{}"));
        assertEquals("Sunrise/Sunset —", WidgetKit.sunLine(null));
        assertEquals(R.drawable.ic_sunrise, WidgetKit.sunIcon("{}"));
        String morning = detail.replace("\"time\":\"2026-11-09T15:00\"", "\"time\":\"2026-11-09T05:00\"");
        assertEquals("Sunrise 6:23 AM", WidgetKit.sunLine(morning));
        assertEquals(R.drawable.ic_sunrise, WidgetKit.sunIcon(morning));
        String night = detail.replace("\"time\":\"2026-11-09T15:00\"", "\"time\":\"2026-11-09T19:00\"");
        assertEquals("Sunrise tomorrow · 6:24 AM", WidgetKit.sunLine(night));
    }
}
