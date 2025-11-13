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

        assertEquals("SDK для этого API-ключа уже создан", exception.getMessage());
    }

    @Test
    void create_inPollingMode_shouldSchedulePolling() throws Exception {
        String apiKey = "test-api";
        WeatherSDK weatherSDK = WeatherSDK.create(apiKey, Mode.POLLING);

        Field executorField  = WeatherSDK.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        ScheduledExecutorService executor = (ScheduledExecutorService) executorField.get(weatherSDK);

        assertNotNull(executor, "Executor должен быть создан");
        assertFalse(executor.isShutdown(), "Executor не должен быть завершён");
    }

    @Test
    void getCurrentWeatherByCity_fromApi_returnWeatherData(){
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

        assertNotNull(result, "Метод должен вернуть объект WeatherData");
        assertEquals(25, result.getTemperature().temp, "Температура должна соответствовать JSON");
        assertEquals("Samara", result.name, "Город должен соответствовать JSON");

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

        cache.put("samara", cachedWeather);

        WeatherData result = spySdk.getCurrentWeatherByCity("Samara");

        assertNotNull(result, "Метод должен вернуть объект WeatherData");
        assertEquals(25, result.getTemperature().temp, "Температура должна соответствовать кэшу");
        assertEquals("Samara", result.name, "Город должен соответсвовать кешу");

        verify(spySdk, never()).getJsonRequest(anyString());
    }

}
