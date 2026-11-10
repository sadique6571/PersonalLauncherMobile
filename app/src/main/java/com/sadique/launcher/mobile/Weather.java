package com.sadique.launcher.mobile;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Live weather from Open-Meteo (free, no key, no account). The request uses
 * the phone's current coarse location when the user allows it; coordinates
 * go only to Open-Meteo for that one request and are never stored or sent
 * anywhere else. When there is no location, Okara (30.81, 73.45) is the
 * fallback and the widget says so. Plain-string JSON parsing, no library.
 * Every field method returns null when its data is missing, so callers can
 * show "—" instead of leaving a blank.
 */
final class Weather {
    static final double OKARA_LATITUDE=30.81,OKARA_LONGITUDE=73.45;
    private Weather() { }

    static String url(double latitude,double longitude) {
        return "https://api.open-meteo.com/v1/forecast?latitude="+latitude+"&longitude="+longitude+"&current=temperature_2m,weather_code&timezone=Asia%2FKarachi";
    }

    /** Same as {@link #url} plus today's high/low (daily max/min, one day). */
    static String dailyUrl(double latitude,double longitude) {
        return "https://api.open-meteo.com/v1/forecast?latitude="+latitude+"&longitude="+longitude+"&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=Asia%2FKarachi";
    }

    /** Full detail feed: current details + hourly (3 fields) + daily (7 fields), 7 days. */
    static String detailUrl(double latitude,double longitude) {
        return "https://api.open-meteo.com/v1/forecast?latitude="+latitude+"&longitude="+longitude
                +"&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m,pressure_msl,cloud_cover"
                +"&hourly=temperature_2m,weather_code,precipitation_probability"
                +"&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max,uv_index_max"
                +"&forecast_days=7&timezone=Asia%2FKarachi";
    }

    /** "24°" or null when the response has no usable reading. */
    static String temperature(String json) {
        String c=currentSection(json); if(c==null) return null;
        double temperature=numberAfter(c,"\"temperature_2m\":");
        if(Double.isNaN(temperature)) return null;
        return Math.round(temperature)+"°";
    }

    /** "Clear" or null when the response has no usable reading. */
    static String condition(String json) {
        String c=currentSection(json); if(c==null) return null;
        int code=numberAfterInt(c,"\"weather_code\":");
        if(code==Integer.MIN_VALUE) return null;
        return describe(code);
    }

    /** "High 26° · Low 18°" or null when the response has no daily high/low. */
    static String highLow(String json) {
        String d=dailySection(json); if(d==null) return null;
        double high=numberAfter(d,"\"temperature_2m_max\":");
        double low=numberAfter(d,"\"temperature_2m_min\":");
        if(Double.isNaN(high) || Double.isNaN(low)) return null;
        return "High "+Math.round(high)+"° · Low "+Math.round(low)+"°";
    }

    /** "26°" today's high, or null. */
    static String high(String json) {
        String d=dailySection(json); if(d==null) return null;
        double high=numberAfter(d,"\"temperature_2m_max\":");
        if(Double.isNaN(high)) return null;
        return Math.round(high)+"°";
    }

    /** "18°" today's low, or null. */
    static String low(String json) {
        String d=dailySection(json); if(d==null) return null;
        double low=numberAfter(d,"\"temperature_2m_min\":");
        if(Double.isNaN(low)) return null;
        return Math.round(low)+"°";
    }

    /** Current WMO weather code, or Integer.MIN_VALUE when unknown. */
    static int code(String json) {
        String c=currentSection(json); if(c==null) return Integer.MIN_VALUE;
        return numberAfterInt(c,"\"weather_code\":");
    }

    /** "<label> · 24° · Clear", or null when the response has no usable reading. */
    static String summary(String json,String label) {
        String c=currentSection(json); if(c==null) return null;
        double temperature=numberAfter(c,"\"temperature_2m\":");
        int code=numberAfterInt(c,"\"weather_code\":");
        if(Double.isNaN(temperature) || code==Integer.MIN_VALUE) return null;
        return label+" · "+Math.round(temperature)+"° · "+describe(code);
    }

    /** "26°" feels-like temperature, or null. */
    static String feelsLike(String json) {
        String c=currentSection(json); if(c==null) return null;
        double v=numberAfter(c,"\"apparent_temperature\":");
        if(Double.isNaN(v)) return null;
        return Math.round(v)+"°";
    }

    /** "62%" humidity, or null. */
    static String humidity(String json) {
        String c=currentSection(json); if(c==null) return null;
        double v=numberAfter(c,"\"relative_humidity_2m\":");
        if(Double.isNaN(v)) return null;
        return Math.round(v)+"%";
    }

    /** Wind speed in km/h, or NaN. */
    static double windSpeedKmh(String json) {
        String c=currentSection(json); if(c==null) return Double.NaN;
        return numberAfter(c,"\"wind_speed_10m\":");
    }

    /** Wind direction in degrees, or NaN. */
    static double windDirectionDeg(String json) {
        String c=currentSection(json); if(c==null) return Double.NaN;
        return numberAfter(c,"\"wind_direction_10m\":");
    }

    /** Compass point for a direction in degrees: N, NE, E, SE, S, SW, W or NW. */
    static String windCompass(double degrees) {
        if(Double.isNaN(degrees)) return null;
        String[] points={"N","NE","E","SE","S","SW","W","NW"};
        int at=(int)Math.floor((((degrees%360)+360)%360)/45.0+0.5)%8;
        return points[at];
    }

    /** "12 km/h NW", or null. */
    static String wind(String json) {
        double speed=windSpeedKmh(json);
        if(Double.isNaN(speed)) return null;
        String compass=windCompass(windDirectionDeg(json));
        return Math.round(speed)+" km/h"+(compass==null?"":" "+compass);
    }

    /** "1013 hPa" pressure, or null. */
    static String pressure(String json) {
        String c=currentSection(json); if(c==null) return null;
        double v=numberAfter(c,"\"pressure_msl\":");
        if(Double.isNaN(v)) return null;
        return Math.round(v)+" hPa";
    }

    /** "20%" cloud cover, or null. */
    static String cloudCover(String json) {
        String c=currentSection(json); if(c==null) return null;
        double v=numberAfter(c,"\"cloud_cover\":");
        if(Double.isNaN(v)) return null;
        return Math.round(v)+"%";
    }

    /** "0.2 mm" precipitation, or null. */
    static String precipitation(String json) {
        String c=currentSection(json); if(c==null) return null;
        double v=numberAfter(c,"\"precipitation\":");
        if(Double.isNaN(v)) return null;
        return (Math.round(v*10)/10.0)+" mm";
    }

    /** "40%" chance of rain today, or null. */
    static String rainChance(String json) {
        String d=dailySection(json); if(d==null) return null;
        double[] v=numberArray(d,"\"precipitation_probability_max\"");
        if(v==null || v.length==0 || Double.isNaN(v[0])) return null;
        return Math.round(v[0])+"%";
    }

    /** "5.2" UV index today, or null. */
    static String uvIndex(String json) {
        String d=dailySection(json); if(d==null) return null;
        double[] v=numberArray(d,"\"uv_index_max\"");
        if(v==null || v.length==0 || Double.isNaN(v[0])) return null;
        return (Math.round(v[0]*10)/10.0)+"";
    }

    /** Current time from the response, e.g. "2026-11-09T15:00", or null. */
    static String currentTime(String json) {
        String c=currentSection(json); if(c==null) return null;
        return stringAfter(c,"\"time\":");
    }

    /** "2026-11-09T06:23" or "06:23" -> "6:23 AM". Returns null when unparseable. */
    static String formatTime12(String iso) {
        return formatMinutes12(minutesOfDay(iso));
    }

    /** Minutes since midnight (0–1439) -> "6:23 AM". Returns null when out of range. */
    static String formatMinutes12(int minutes) {
        if(minutes<0 || minutes>=24*60) return null;
        int hour=minutes/60, minute=minutes%60;
        String ampm=hour<12?"AM":"PM";
        int h12=hour%12; if(h12==0) h12=12;
        return h12+":"+(minute<10?"0"+minute:Integer.toString(minute))+" "+ampm;
    }

    /** "2026-11-09T15:00" -> "3 PM"; "2026-11-09T15:30" -> "3:30 PM". Null when unparseable. */
    static String hourLabel(String iso) {
        int minutes=minutesOfDay(iso); if(minutes<0) return null;
        int hour=minutes/60, minute=minutes%60;
        String ampm=hour<12?"AM":"PM";
        int h12=hour%12; if(h12==0) h12=12;
        return minute==0?h12+" "+ampm:h12+":"+(minute<10?"0"+minute:Integer.toString(minute))+" "+ampm;
    }

    /** Minutes since midnight for an ISO time, or -1 when unparseable. */
    static int minutesOfDay(String iso) {
        if(iso==null) return -1;
        int t=iso.indexOf('T');
        String hm=t>=0?iso.substring(t+1):iso;
        String[] parts=hm.split(":");
        if(parts.length<2) return -1;
        try {
            int h=Integer.parseInt(parts[0]), m=Integer.parseInt(parts[1]);
            if(h<0 || h>23 || m<0 || m>59) return -1;
            return h*60+m;
        } catch(NumberFormatException bad) { return -1; }
    }

    /** {today's sunrise, today's sunset, tomorrow's sunrise} in minutes, or null. */
    static int[] sunMinutes(String json) {
        String d=dailySection(json); if(d==null) return null;
        String[] rise=stringArray(d,"\"sunrise\"");
        String[] set=stringArray(d,"\"sunset\"");
        if(rise==null || set==null || rise.length<2 || set.length<1) return null;
        int r0=minutesOfDay(rise[0]), s0=minutesOfDay(set[0]), r1=minutesOfDay(rise[1]);
        if(r0<0 || s0<0 || r1<0) return null;
        return new int[]{r0,s0,r1};
    }

    /** The next sun event: sunrise before sunrise, sunset during the day, tomorrow's sunrise after sunset. */
    static final class SunEvent {
        final boolean sunrise;
        final boolean tomorrow;
        final String label;
        SunEvent(boolean sunrise,boolean tomorrow,String label) { this.sunrise=sunrise; this.tomorrow=tomorrow; this.label=label; }
    }

    static SunEvent sunEvent(String json) {
        int[] sun=sunMinutes(json); if(sun==null) return null;
        int now=minutesOfDay(currentTime(json)); if(now<0) return null;
        if(now<sun[0]) return new SunEvent(true,false,"Sunrise "+formatMinutes12(sun[0]));
        if(now<sun[1]) return new SunEvent(false,false,"Sunset "+formatMinutes12(sun[1]));
        return new SunEvent(true,true,"Sunrise tomorrow · "+formatMinutes12(sun[2]));
    }

    /** One hour of the hourly forecast. */
    static final class Hour {
        final String label;
        final String temp;
        final int code;
        final String rain;
        Hour(String label,String temp,int code,String rain) { this.label=label; this.temp=temp; this.code=code; this.rain=rain; }
    }

    /** The next `count` hours from the current time. */
    static List<Hour> hourly(String json,int count) {
        List<Hour> out=new ArrayList<>();
        String h=hourlySection(json); if(h==null) return out;
        String[] times=stringArray(h,"\"time\"");
        double[] temps=numberArray(h,"\"temperature_2m\"");
        double[] codes=numberArray(h,"\"weather_code\"");
        double[] rains=numberArray(h,"\"precipitation_probability\"");
        if(times==null || temps==null || codes==null) return out;
        String now=currentTime(json);
        int start=0;
        if(now!=null) { while(start<times.length && times[start].compareTo(now)<0) start++; }
        for(int i=start;i<times.length && out.size()<count;i++) {
            String label=hourLabel(times[i]); if(label==null) continue;
            String temp=Double.isNaN(temps[i])?"—":Math.round(temps[i])+"°";
            int code=Double.isNaN(codes[i])?3:(int)Math.round(codes[i]);
            String rain=rains==null || i>=rains.length || Double.isNaN(rains[i])?"—":Math.round(rains[i])+"%";
            out.add(new Hour(label,temp,code,rain));
        }
        return out;
    }

    /** One day of the 7-day forecast. */
    static final class Day {
        final String name;
        final int code;
        final String high;
        final String low;
        final String rain;
        Day(String name,int code,String high,String low,String rain) { this.name=name; this.code=code; this.high=high; this.low=low; this.rain=rain; }
    }

    /** Up to 7 days, starting today ("Today", then weekday names). */
    static List<Day> daily(String json) {
        List<Day> out=new ArrayList<>();
        String d=dailySection(json); if(d==null) return out;
        String[] dates=stringArray(d,"\"time\"");
        double[] codes=numberArray(d,"\"weather_code\"");
        double[] highs=numberArray(d,"\"temperature_2m_max\"");
        double[] lows=numberArray(d,"\"temperature_2m_min\"");
        double[] rains=numberArray(d,"\"precipitation_probability_max\"");
        if(dates==null) return out;
        for(int i=0;i<dates.length && i<7;i++) {
            String name;
            if(i==0) name="Today";
            else {
                try { name=LocalDate.parse(dates[i]).getDayOfWeek().getDisplayName(TextStyle.SHORT,Locale.ENGLISH); }
                catch(Exception bad) { name=dates[i]; }
            }
            int code=codes!=null && i<codes.length && !Double.isNaN(codes[i])?(int)Math.round(codes[i]):3;
            String high=highs!=null && i<highs.length && !Double.isNaN(highs[i])?Math.round(highs[i])+"°":"—";
            String low=lows!=null && i<lows.length && !Double.isNaN(lows[i])?Math.round(lows[i])+"°":"—";
            String rain=rains!=null && i<rains.length && !Double.isNaN(rains[i])?Math.round(rains[i])+"%":"—";
            out.add(new Day(name,code,high,low,rain));
        }
        return out;
    }

    /** Condition icon (our own drawable) for a WMO weather code. */
    static int conditionIcon(int code) {
        if(code==0) return R.drawable.ic_weather_clear;
        if(code==1 || code==2) return R.drawable.ic_weather_partly;
        if(code==3) return R.drawable.ic_weather_cloudy;
        if(code==45 || code==48) return R.drawable.ic_weather_fog;
        if(code>=51 && code<=57) return R.drawable.ic_weather_rain;
        if(code>=61 && code<=67) return R.drawable.ic_weather_rain;
        if(code>=71 && code<=77) return R.drawable.ic_weather_snow;
        if(code>=80 && code<=82) return R.drawable.ic_weather_rain;
        if(code==85 || code==86) return R.drawable.ic_weather_snow;
        if(code==95) return R.drawable.ic_weather_storm;
        return R.drawable.ic_weather_cloudy;
    }

    /** WMO weather-code description. */
    static String describe(int code) {
        if(code==0) return "Clear";
        if(code==1) return "Mostly clear";
        if(code==2) return "Partly cloudy";
        if(code==3) return "Cloudy";
        if(code==45 || code==48) return "Fog";
        if(code>=51 && code<=55) return "Drizzle";
        if(code>=61 && code<=65) return "Rain";
        if(code>=71 && code<=75) return "Snow";
        if(code>=80 && code<=82) return "Showers";
        if(code==95) return "Thunderstorm";
        return "—";
    }

    /** Object body of "name":{...} (including the braces), or null. */
    static String section(String json,String name) {
        if(json==null) return null;
        int at=json.indexOf("\""+name+"\":{"); if(at<0) return null;
        int start=json.indexOf('{',at);
        int depth=0;
        for(int i=start;i<json.length();i++) {
            char c=json.charAt(i);
            if(c=='{') depth++;
            else if(c=='}') { depth--; if(depth==0) return json.substring(start,i+1); }
        }
        return null;
    }

    private static String currentSection(String json) { return section(json,"current"); }
    private static String dailySection(String json) { return section(json,"daily"); }
    private static String hourlySection(String json) { return section(json,"hourly"); }

    /** String values of "key":[...] (quoted or bare), or null when the array is absent. */
    static String[] stringArray(String json,String key) {
        if(json==null) return null;
        int at=json.indexOf(key+":["); if(at<0) return null;
        int start=json.indexOf('[',at)+1;
        int end=json.indexOf(']',start); if(end<0) return null;
        String body=json.substring(start,end).trim();
        if(body.isEmpty()) return new String[0];
        List<String> out=new ArrayList<>();
        int i=0;
        while(i<body.length()) {
            while(i<body.length() && (body.charAt(i)==' ' || body.charAt(i)==',')) i++;
            if(i>=body.length()) break;
            if(body.charAt(i)=='"') {
                int j=body.indexOf('"',i+1); if(j<0) break;
                out.add(body.substring(i+1,j)); i=j+1;
            } else {
                int j=i; while(j<body.length() && body.charAt(j)!=',') j++;
                out.add(body.substring(i,j).trim()); i=j;
            }
        }
        return out.toArray(new String[0]);
    }

    /** Numbers of "key":[...] (NaN for null entries), or null when the array is absent. */
    static double[] numberArray(String json,String key) {
        String[] parts=stringArray(json,key); if(parts==null) return null;
        double[] out=new double[parts.length];
        for(int i=0;i<parts.length;i++) {
            try { out[i]=Double.parseDouble(parts[i]); }
            catch(NumberFormatException bad) { out[i]=Double.NaN; }
        }
        return out;
    }

    private static String stringAfter(String json,String key) {
        if(json==null) return null;
        int at=json.indexOf(key); if(at<0) return null;
        int start=at+key.length();
        while(start<json.length() && json.charAt(start)==' ') start++;
        if(start>=json.length() || json.charAt(start)!='"') return null;
        int end=json.indexOf('"',start+1); if(end<0) return null;
        return json.substring(start+1,end);
    }

    private static double numberAfter(String json,String key) {
        int at=json.indexOf(key); if(at<0) return Double.NaN;
        int start=at+key.length(); while(start<json.length() && (json.charAt(start)==' ' || json.charAt(start)=='[')) start++;
        int end=start; while(end<json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end)=='.' || json.charAt(end)=='-' || json.charAt(end)=='+')) end++;
        if(end==start) return Double.NaN;
        try { return Double.parseDouble(json.substring(start,end)); } catch(NumberFormatException bad) { return Double.NaN; }
    }

    private static int numberAfterInt(String json,String key) {
        double value=numberAfter(json,key); return Double.isNaN(value)?Integer.MIN_VALUE:(int)Math.round(value);
    }
}
