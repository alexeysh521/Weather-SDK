package com.example.weather.sdk.logic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherData {
    public List<Weather> weather;
    public Temperature temperature;
    public int visibility;
    public Wind wind;
    public long datetime;
    public Sys sys;
    public int timezone;
    public String name;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        public String main;
        public String description;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Temperature {
        public double temp;
        public double feels_like;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        public double speed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sys {
        public long sunrise;
        public long sunset;
    }
}
