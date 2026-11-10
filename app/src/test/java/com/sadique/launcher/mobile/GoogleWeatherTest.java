package com.sadique.launcher.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Test;

/** Google Weather API adapter (Weather.fromGoogle) + condition-type mapping. */
public final class GoogleWeatherTest {
    private static final String CURRENT = "{\"currentTime\":\"2026-11-10T07:00:00Z\","
            + "\"timeZone\":{\"id\":\"Asia/Karachi\"},"
            + "\"weatherCondition\":{\"iconBaseUri\":\"https://maps.gstatic.com/weather/v1/clear\","
            + "\"description\":{\"text\":\"Clear\",\"languageCode\":\"en\"},\"type\":\"CLEAR\"},"
            + "\"temperature\":{\"degrees\":24.0,\"unit\":\"CELSIUS\"},"
            + "\"feelsLikeTemperature\":{\"degrees\":26.0,\"unit\":\"CELSIUS\"},"
            + "\"relativeHumidity\":66,"
            + "\"precipitation\":{\"probability\":{\"percent\":0},\"qpf\":{\"quantity\":0.0,\"unit\":\"MILLIMETERS\"}},"
            + "\"airPressure\":{\"meanSeaLevelMillibars\":1013.0},"
            + "\"wind\":{\"direction\":{\"degrees\":120,\"cardinal\":\"ESE\"},\"speed\":{\"value\":12.0,\"unit\":\"KILOMETERS_PER_HOUR\"}},"
            + "\"cloudCover\":10,\"isDaytime\":true}";

    private static String hoursJson() {
        StringBuilder sb = new StringBuilder("{\"forecastHours\":[");
        OffsetDateTime start = OffsetDateTime.parse("2026-11-10T07:00:00Z");
        for (int h = 0; h < 48; h++) {
            if (h > 0) sb.append(",");
            sb.append("{\"interval\":{\"startTime\":\"").append(start.plusHours(h)).append("\",\"endTime\":\"")
                    .append(start.plusHours(h + 1)).append("\"},");
            sb.append("\"displayDateTime\":{\"year\":2026,\"month\":11,\"day\":10,\"hours\":12,\"minutes\":0,\"seconds\":0,\"utcOffset\":\"+05:00\"},");
            sb.append("\"weatherCondition\":{\"type\":\"").append(h % 5 == 0 ? "PARTLY_CLOUDY" : "CLEAR").append("\"},");
            sb.append("\"temperature\":{\"degrees\":").append(24 + (h % 6)).append(".0,\"unit\":\"CELSIUS\"},");
            sb.append("\"feelsLikeTemperature\":{\"degrees\":26.0,\"unit\":\"CELSIUS\"},");
            sb.append("\"precipitation\":{\"probability\":{\"percent\":0}},");
            sb.append("\"relativeHumidity\":66,");
            sb.append("\"wind\":{\"direction\":{\"degrees\":120},\"speed\":{\"value\":12.0,\"unit\":\"KILOMETERS_PER_HOUR\"}},");
            sb.append("\"cloudCover\":10,\"uvIndex\":5,\"isDaytime\":true}");
        }
        return sb.append("]}").toString();
    }

    private static String daysJson() {
        StringBuilder sb = new StringBuilder("{\"forecastDays\":[");
        LocalDate base = LocalDate.of(2026, 11, 10);
        for (int d = 0; d < 7; d++) {
            LocalDate date = base.plusDays(d);
            if (d > 0) sb.append(",");
            sb.append("{\"interval\":{\"startTime\":\"2026-11-10T02:00:00Z\",\"endTime\":\"2026-11-11T02:00:00Z\"},");
            sb.append("\"displayDate\":{\"year\":").append(date.getYear()).append(",\"month\":").append(date.getMonthValue())
                    .append(",\"day\":").append(date.getDayOfMonth()).append("},");
            sb.append("\"daytimeForecast\":{\"weatherCondition\":{\"type\":\"CLEAR\"},\"precipitation\":{\"probability\":{\"percent\":0}},\"relativeHumidity\":60,\"uvIndex\":5,\"cloudCover\":10},");
            sb.append("\"nighttimeForecast\":{\"weatherCondition\":{\"type\":\"CLEAR\"},\"precipitation\":{\"probability\":{\"percent\":0}},\"relativeHumidity\":70,\"uvIndex\":0,\"cloudCover\":5},");
            sb.append("\"maxTemperature\":{\"degrees\":28.0,\"unit\":\"CELSIUS\"},");
            sb.append("\"minTemperature\":{\"degrees\":17.0,\"unit\":\"CELSIUS\"},");
            sb.append("\"feelsLikeMaxTemperature\":{\"degrees\":30.0,\"unit\":\"CELSIUS\"},");
            sb.append("\"feelsLikeMinTemperature\":{\"degrees\":18.0,\"unit\":\"CELSIUS\"},");
            sb.append("\"sunEvents\":{\"sunriseTime\":\"").append(date).append("T06:28:00+05:00\",\"sunsetTime\":\"").append(date).append("T17:11:00+05:00\"}}");
        }
        return sb.append("]}").toString();
    }

    @Test public void googleResponseParsesLikeOpenMeteo() {
        String adapted = Weather.fromGoogle(CURRENT, hoursJson(), daysJson());
        assertTrue(adapted != null && !adapted.isEmpty());
        assertEquals("24°", Weather.temperature(adapted));
        assertEquals("Clear", Weather.condition(adapted));
        assertEquals("High 28° · Low 17°", Weather.highLow(adapted));
        assertEquals("54-2L · 24° · Clear", Weather.summary(adapted, "54-2L"));
        // Noon in Okara, sunrise 6:28 AM passed -> today's sunset, 12h AM/PM.
        assertEquals("Sunset 5:11 PM", Weather.sunEvent(adapted).label);
        assertEquals(12, Weather.hourly(adapted, 12).size());
        assertEquals(7, Weather.daily(adapted).size());
        assertEquals(0, Weather.daily(adapted).get(0).code);
        assertEquals("28°", Weather.daily(adapted).get(0).high);
        assertEquals("17°", Weather.daily(adapted).get(0).low);
    }

    @Test public void conditionTypeMapping() {
        assertEquals(0, Weather.googleCode("CLEAR"));
        assertEquals(1, Weather.googleCode("MOSTLY_CLEAR"));
        assertEquals(2, Weather.googleCode("PARTLY_CLOUDY"));
        assertEquals(3, Weather.googleCode("CLOUDY"));
        assertEquals(61, Weather.googleCode("LIGHT_RAIN"));
        assertEquals(63, Weather.googleCode("RAIN"));
        assertEquals(65, Weather.googleCode("HEAVY_RAIN"));
        assertEquals(80, Weather.googleCode("RAIN_SHOWERS"));
        assertEquals(71, Weather.googleCode("LIGHT_SNOW"));
        assertEquals(95, Weather.googleCode("THUNDERSTORM"));
        assertEquals(Integer.MIN_VALUE, Weather.googleCode("BOGUS"));
        assertEquals(Integer.MIN_VALUE, Weather.googleCode(null));
    }

    @Test public void unusableGoogleResponseReturnsNull() {
        assertNull(Weather.fromGoogle(null, hoursJson(), daysJson()));
        assertNull(Weather.fromGoogle("{\"temperature\":{}}", hoursJson(), daysJson()));
        assertNull(Weather.fromGoogle("{\"temperature\":{\"degrees\":24.0},\"weatherCondition\":{\"type\":\"BOGUS\"}}", hoursJson(), daysJson()));
    }

    @Test public void utcTimesBecomeKarachiLocal() {
        assertEquals("2026-11-10T12:00", Weather.gLocalTime("2026-11-10T07:00:00Z"));
        assertEquals("2026-11-10T06:28", Weather.gLocalTime("2026-11-10T06:28:00+05:00"));
        assertEquals("2026-11-10", Weather.gLocalDate("{\"year\":2026,\"month\":11,\"day\":10}"));
        assertNull(Weather.gLocalTime("not-a-time"));
    }

    @Test public void objectArraysSplit() {
        List<String> days = Weather.gObjects(daysJson(), "forecastDays");
        assertEquals(7, days.size());
        assertTrue(days.get(0).contains("\"displayDate\""));
        assertEquals(48, Weather.gObjects(hoursJson(), "forecastHours").size());
        assertTrue(Weather.gObjects("{}", "forecastDays").isEmpty());
    }
}
