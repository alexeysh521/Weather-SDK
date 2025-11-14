package com.example.weather.sdk;

import com.example.weather.sdk.enums.Mode;
import com.example.weather.sdk.exceptions.WeatherSdkException;
import com.example.weather.sdk.logic.WeatherSDK;
import com.example.weather.sdk.logic.model.CachedWeather;
import com.example.weather.sdk.logic.model.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class WeatherSDKTests {

    @BeforeEach
    void clearInstances() throws Exception {
        Field field = WeatherSDK.class.getDeclaredField("INSTANCES");
        field.setAccessible(true);
        ((Map<String, WeatherSDK>) field.get(null)).clear();
    }

    @Test
    void create_ifSdkAlreadyExists_throwsException(){
        String apiKey = "test-api";
        WeatherSDK.create(apiKey, Mode.POLLING);

        WeatherSdkException exception = assertThrows(
                WeatherSdkException.class,
                () -> WeatherSDK.create(apiKey, Mode.POLLING)
        );

        assertEquals("An SDK instance for this API-key has already been created", exception.getMessage());
    }

    @Test
    void create_inPollingMode_shouldSchedulePolling() throws Exception {
        String apiKey = "test-api";
        WeatherSDK weatherSDK = WeatherSDK.create(apiKey, Mode.POLLING);

        Field executorField  = WeatherSDK.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        ScheduledExecutorService executor = (ScheduledExecutorService) executorField.get(weatherSDK);

        assertNotNull(executor, "The executor must be created");
        assertFalse(executor.isShutdown(), "The executor must not be terminated");
    }

    @Test
    void getCurrentWeatherByCity_fromApi_returnWeatherData() throws Exception {
        WeatherSDK spySdk = Mockito.spy(WeatherSDK.create("test-api", Mode.POLLING));

        String json = """
                {
                  "main": { "temp": 25, "feels_like": 23 },
                  "name": "Samara",
                  "visibility": 10000,
                  "wind": { "speed": 5.5 },
                  "sys": { "sunrise": 1690000000, "sunset": 1690040000 },
                  "timezone": 14400
                }""";


        doReturn(json).when(spySdk).getJsonRequest("Samara");

        WeatherData result = spySdk.getCurrentWeatherByCity("Samara");

        assertNotNull(result, "The method must return a WeatherData object");
        assertEquals(25, result.getTemperature().temp, "The temperature must match the JSON");
        assertEquals("Samara", result.name, "The city must match the JSON");

        verify(spySdk, times(1)).getJsonRequest("Samara");
    }

    @Test
    void getCurrentWeatherByCity_fromCache_returnWeatherData() throws Exception {
        WeatherSDK spySdk = Mockito.spy(WeatherSDK.create("test-api", Mode.POLLING));

        Field cacheField  = WeatherSDK.class.getDeclaredField("cache");
        cacheField.setAccessible(true);

        Map<String, CachedWeather> cache = (Map<String, CachedWeather>) cacheField.get(spySdk);

        String json = """
                {
                  "main": { "temp": 25, "feels_like": 23 },
                  "name": "Samara",
                  "visibility": 10000,
                  "wind": { "speed": 5.5 },
                  "sys": { "sunrise": 1690000000, "sunset": 1690040000 },
                  "timezone": 14400
                }""";

        CachedWeather cachedWeather = new CachedWeather(json);

        cache.put("Samara", cachedWeather);

        WeatherData result = spySdk.getCurrentWeatherByCity("Samara");

        assertNotNull(result, "The method must return a WeatherData object");
        assertEquals(25, result.getTemperature().temp, "The temperature must match the cache");
        assertEquals("Samara", result.name, "The city must match the cache");

        verify(spySdk, never()).getJsonRequest(anyString());
    }

}
