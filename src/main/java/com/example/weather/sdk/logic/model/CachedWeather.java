package com.example.weather.sdk.logic.model;

public class CachedWeather {

    private final String jsonData;
    private final long lastUpdated;

    public CachedWeather(String jsonData) {
        this.jsonData = jsonData;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getJsonData() {
        return jsonData;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public boolean isFresh() {
        long now = System.currentTimeMillis();
        long diff = now - lastUpdated;
        return diff < 600_000;
    }
}
