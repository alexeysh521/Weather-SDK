package com.example.weather.sdk.exceptions;

public class WeatherSdkException extends RuntimeException {
    public WeatherSdkException(String message) {
        super(message);
    }

    public WeatherSdkException(Throwable cause) {
        super(cause);
    }

    public WeatherSdkException(String message, Throwable cause) {
        super(message, cause);
    }
}
