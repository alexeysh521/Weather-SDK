package com.example.weather.sdk.logic.model;

public class CachedWeather {

    private final String jsonData;
    private final long timestamp;

    public CachedWeather(String jsonData) {
        this.jsonData = jsonData;
        this.timestamp = System.currentTimeMillis();
    }

    public String getJsonData() {
        return jsonData;
    }

    public boolean isFresh() {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        return diff < 10 * 60 * 1000; // 10 минут
    }
}
