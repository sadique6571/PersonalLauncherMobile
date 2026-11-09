package com.sadique.launcher.mobile;

/**
 * Live weather for the Home widget, from Open-Meteo (free, no key, no account).
 * The request uses the phone's current coarse location when the user allows
 * it; coordinates go only to Open-Meteo for that one request and are never
 * stored or sent anywhere else. When there is no location, Okara (30.81,
 * 73.45) is the fallback and the widget says so. Plain-string JSON parsing,
 * no library: the response is small and only two fields are read.
 */
final class Weather {
    static final double OKARA_LATITUDE=30.81,OKARA_LONGITUDE=73.45;
    private Weather() { }

    static String url(double latitude,double longitude) {
        return "https://api.open-meteo.com/v1/forecast?latitude="+latitude+"&longitude="+longitude+"&current=temperature_2m,weather_code&timezone=Asia%2FKarachi";
    }

    /** "<label> · 24° · Clear", or null when the response has no usable reading. */
    static String summary(String json,String label) {
        if(json==null) return null;
        double temperature=numberAfter(json,"\"temperature_2m\":");
        int code=numberAfterInt(json,"\"weather_code\":");
        if(Double.isNaN(temperature) || code==Integer.MIN_VALUE) return null;
        return label+" · "+Math.round(temperature)+"° · "+describe(code);
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

    private static double numberAfter(String json,String key) {
        int at=json.indexOf(key); if(at<0) return Double.NaN;
        int start=at+key.length(); while(start<json.length() && (json.charAt(start)==' ')) start++;
        int end=start; while(end<json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end)=='.' || json.charAt(end)=='-' || json.charAt(end)=='+')) end++;
        if(end==start) return Double.NaN;
        try { return Double.parseDouble(json.substring(start,end)); } catch(NumberFormatException bad) { return Double.NaN; }
    }

    private static int numberAfterInt(String json,String key) {
        double value=numberAfter(json,key); return Double.isNaN(value)?Integer.MIN_VALUE:(int)Math.round(value);
    }
}
