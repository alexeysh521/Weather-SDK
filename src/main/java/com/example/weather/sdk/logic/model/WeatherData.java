package com.example.weather.sdk.logic.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherData {
    public List<Weather> weather;

    @JsonIgnore
    public Temperature temperature;

    @JsonIgnore
    public long datetime;

    public int visibility;
    public Wind wind;
    public Sys sys;
    public int timezone;
    public String name;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        public String main;
        public String description;

        @Override
        public String toString() {
            return "Weather{" +
                    "main='" + main + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Temperature {
        @JsonProperty("temp")
        public double temp;

        @JsonProperty("feels_like")
        public double feels_like;

        @Override
        public String toString() {
            return "Temperature{" +
                    "temp=" + temp +
                    ", feels_like=" + feels_like +
                    '}';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        public double speed;

        @Override
        public String toString() {
            return "Wind{" +
                    "speed=" + speed +
                    '}';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sys {
        public long sunrise;
        public long sunset;

        @Override
        public String toString() {
            return "Sys{" +
                    "sunrise=" + sunrise +
                    ", sunset=" + sunset +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "weather=" + weather.toString() +
                ", temperature=" + temperature.toString() +
                ", visibility=" + visibility +
                ", wind=" + wind.toString() +
                ", datetime=" + datetime +
                ", sys=" + sys.toString() +
                ", timezone=" + timezone +
                ", name='" + name + '\'' +
                '}';
    }

    @JsonProperty("datetime")
    public long getDatetime() {
        return datetime;
    }

    @JsonProperty("dt")
    public void setDatetime(long datetime) {
        this.datetime = datetime;
    }

    @JsonProperty("temperature")
    public Temperature getTemperature() {
        return temperature;
    }

    @JsonProperty("main")
    public void setTemperature(Temperature temperature) {
        this.temperature = temperature;
    }
}
