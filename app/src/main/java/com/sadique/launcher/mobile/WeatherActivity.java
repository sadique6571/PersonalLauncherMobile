package com.sadique.launcher.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/**
 * Full Weather screen (glass/dark design): big current temperature,
 * condition and icon, a details grid, the sunrise/sunset path, the next
 * 12 hours and the 7-day forecast. Data is the same Open-Meteo feed the
 * widgets use (free, no key): current coarse location, Okara fallback,
 * cached offline. The location line shows the real place name ("54-2L",
 * "Okara", ... — free keyless reverse lookup, see {@link PlaceName});
 * with no name it says "Current location". Tapping a weather widget opens
 * this screen; the weather line on Home opens it too. Refresh reloads the
 * feed; with no data at all the screen says "Weather unavailable" instead
 * of staying blank.
 */
public class WeatherActivity extends Activity {
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = 0xff9fb3c8;
    private static final int CARD = 0x3320344f;

    private LinearLayout content;
    private TextView status;
    private TextView locationLabel, tempView, conditionView, dateView;
    private ImageView conditionIcon;
    private LinearLayout detailsBox, hourlyBox, dailyBox;
    private SunArcView sunArc;
    private TextView sunriseView, sunsetView, sunEventView;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff070d18);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -1, 1));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(16), dp(20), dp(20));
        scroll.addView(page, new LinearLayout.LayoutParams(-1, -2));

        status = text("", 16, false, MUTED);
        status.setGravity(Gravity.CENTER);
        status.setVisibility(View.GONE);
        page.addView(status);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        page.addView(content);

        locationLabel = text("", 14, false, MUTED);
        content.addView(locationLabel);
        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout headText = new LinearLayout(this);
        headText.setOrientation(LinearLayout.VERTICAL);
        tempView = text("", 64, true, TEXT);
        headText.addView(tempView);
        conditionView = text("", 18, false, TEXT);
        headText.addView(conditionView);
        head.addView(headText, new LinearLayout.LayoutParams(0, -2, 1));
        conditionIcon = new ImageView(this);
        conditionIcon.setImageResource(R.drawable.ic_weather_clear);
        head.addView(conditionIcon, new LinearLayout.LayoutParams(dp(72), dp(72)));
        content.addView(head);
        dateView = text("", 14, false, MUTED);
        content.addView(dateView);

        content.addView(sectionTitle("Details"));
        detailsBox = new LinearLayout(this);
        detailsBox.setOrientation(LinearLayout.VERTICAL);
        detailsBox.setBackgroundColor(CARD);
        detailsBox.setPadding(dp(12), dp(8), dp(12), dp(8));
        content.addView(detailsBox);

        content.addView(sectionTitle("Sunrise & Sunset"));
        sunArc = new SunArcView(this);
        content.addView(sunArc, new LinearLayout.LayoutParams(-1, dp(120)));
        LinearLayout sunRow = row();
        sunriseView = text("", 14, true, TEXT);
        sunRow.addView(sunriseView, new LinearLayout.LayoutParams(0, -2, 1));
        sunsetView = text("", 14, true, TEXT);
        sunsetView.setGravity(Gravity.END);
        sunRow.addView(sunsetView, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(sunRow);
        sunEventView = text("", 14, false, MUTED);
        sunEventView.setGravity(Gravity.CENTER);
        content.addView(sunEventView);

        content.addView(sectionTitle("Hourly"));
        HorizontalScrollView hourlyScroll = new HorizontalScrollView(this);
        hourlyScroll.setHorizontalScrollBarEnabled(false);
        hourlyBox = row();
        hourlyScroll.addView(hourlyBox);
        content.addView(hourlyScroll);

        content.addView(sectionTitle("7-day forecast"));
        dailyBox = new LinearLayout(this);
        dailyBox.setOrientation(LinearLayout.VERTICAL);
        dailyBox.setBackgroundColor(CARD);
        dailyBox.setPadding(dp(12), dp(8), dp(12), dp(8));
        content.addView(dailyBox);

        Button refresh = new Button(this);
        refresh.setText(R.string.weather_refresh);
        refresh.setOnClickListener(v -> fetch());
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(-1, -2);
        refreshParams.topMargin = dp(16);
        page.addView(refresh, refreshParams);

        setContentView(root);
        String cached = WidgetKit.cachedJson(this);
        if (cached != null) show(cached);
        else showUnavailable();
        fetch();
    }

    private void fetch() {
        WidgetKit.fetchWeather(this, (summary, highLow, json) -> runOnUiThread(() -> {
            if (json != null) show(json);
            else if (WidgetKit.cachedJson(this) == null) showUnavailable();
        }));
    }

    private void showUnavailable() {
        content.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);
        status.setText(R.string.weather_unavailable);
    }

    private void show(String json) {
        content.setVisibility(View.VISIBLE);
        status.setVisibility(View.GONE);
        locationLabel.setText(WidgetKit.cachedLabel(this));
        String temp = Weather.temperature(json);
        tempView.setText(temp == null ? "—" : temp);
        String condition = Weather.condition(json);
        conditionView.setText(condition == null ? "—" : condition);
        int code = Weather.code(json);
        conditionIcon.setImageResource(code == Integer.MIN_VALUE ? R.drawable.ic_weather_cloudy : Weather.conditionIcon(code));
        dateView.setText(WidgetKit.dateLine());
        fillDetails(json);
        fillSun(json);
        fillHourly(json);
        fillDaily(json);
    }

    private void fillDetails(String json) {
        detailsBox.removeAllViews();
        String[][] pairs = {
            {"Feels like", Weather.feelsLike(json)},
            {"High", Weather.high(json)},
            {"Low", Weather.low(json)},
            {"Humidity", Weather.humidity(json)},
            {"Wind", Weather.wind(json)},
            {"Pressure", Weather.pressure(json)},
            {"Cloud cover", Weather.cloudCover(json)},
            {"Precipitation", Weather.precipitation(json)},
            {"Rain chance", Weather.rainChance(json)},
            {"UV index", Weather.uvIndex(json)},
        };
        for (int i = 0; i < pairs.length; i += 2) {
            LinearLayout row = row();
            row.addView(detailCell(pairs[i][0], pairs[i][1]), new LinearLayout.LayoutParams(0, -2, 1));
            if (i + 1 < pairs.length) {
                row.addView(detailCell(pairs[i + 1][0], pairs[i + 1][1]), new LinearLayout.LayoutParams(0, -2, 1));
            } else {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, -2, 1));
            }
            detailsBox.addView(row);
        }
    }

    private LinearLayout detailCell(String label, String value) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setPadding(0, dp(4), 0, dp(4));
        cell.addView(text(label, 12, false, MUTED));
        cell.addView(text(value == null ? "—" : value, 16, true, TEXT));
        return cell;
    }

    private void fillSun(String json) {
        int[] sun = Weather.sunMinutes(json);
        int now = Weather.minutesOfDay(Weather.currentTime(json));
        if (sun == null) {
            sunArc.setTimes(-1, -1, -1);
            sunriseView.setText("Sunrise —");
            sunsetView.setText("Sunset —");
            sunEventView.setText("Sunrise/Sunset —");
            return;
        }
        sunArc.setTimes(sun[0], sun[1], now);
        sunriseView.setText("Sunrise " + Weather.formatMinutes12(sun[0]));
        sunsetView.setText("Sunset " + Weather.formatMinutes12(sun[1]));
        Weather.SunEvent e = Weather.sunEvent(json);
        sunEventView.setText(e == null ? "Sunrise/Sunset —" : e.label);
    }

    private void fillHourly(String json) {
        hourlyBox.removeAllViews();
        List<Weather.Hour> hours = Weather.hourly(json, 12);
        if (hours.isEmpty()) {
            hourlyBox.addView(text("Hourly forecast unavailable", 14, false, MUTED));
            return;
        }
        for (Weather.Hour hour : hours) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(10), 0, dp(10), 0);
            cell.addView(text(hour.label, 12, false, MUTED));
            ImageView icon = new ImageView(this);
            icon.setImageResource(Weather.conditionIcon(hour.code));
            cell.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));
            cell.addView(text(hour.temp, 14, true, TEXT));
            cell.addView(text(hour.rain, 11, false, MUTED));
            hourlyBox.addView(cell);
        }
    }

    private void fillDaily(String json) {
        dailyBox.removeAllViews();
        List<Weather.Day> days = Weather.daily(json);
        if (days.isEmpty()) {
            dailyBox.addView(text("7-day forecast unavailable", 14, false, MUTED));
            return;
        }
        for (Weather.Day day : days) {
            LinearLayout row = row();
            row.setPadding(0, dp(6), 0, dp(6));
            row.addView(text(day.name, 14, false, TEXT), new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(text(day.rain, 12, false, MUTED));
            ImageView icon = new ImageView(this);
            icon.setImageResource(Weather.conditionIcon(day.code));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
            iconParams.leftMargin = dp(8);
            iconParams.rightMargin = dp(8);
            row.addView(icon, iconParams);
            row.addView(text(day.low, 14, false, MUTED));
            TextView high = text(day.high, 14, true, TEXT);
            LinearLayout.LayoutParams highParams = new LinearLayout.LayoutParams(-2, -2);
            highParams.leftMargin = dp(8);
            row.addView(high, highParams);
            dailyBox.addView(row);
        }
    }

    private TextView sectionTitle(String title) {
        TextView v = text(title, 16, true, TEXT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.topMargin = dp(16);
        params.bottomMargin = dp(6);
        v.setLayoutParams(params);
        return v;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    private LinearLayout row() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
}
