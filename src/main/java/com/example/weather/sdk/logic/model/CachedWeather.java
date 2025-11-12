package com.example.weather.sdk.logic.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedWeather {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

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
        LOGGER.info("Данные о погоде устарели: {}", jsonData);

        return diff < 600_000;
    }
}
