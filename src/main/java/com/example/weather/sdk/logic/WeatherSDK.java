package com.example.weather.sdk.logic;


import com.example.weather.sdk.enums.Mode;
import com.example.weather.sdk.exceptions.WeatherSdkException;
import com.example.weather.sdk.logic.model.CachedWeather;
import com.example.weather.sdk.logic.model.WeatherData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeatherSDK {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, WeatherSDK> INSTANCES = new HashMap<>();

    private final Map<String, CachedWeather> cache = new LinkedHashMap<>(10, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedWeather> eldest) {
            return size() > 10;
        }
    };

    private ScheduledExecutorService executor;
    private final String apiKey;
    private final Mode mode;

    private WeatherSDK(String apiKey, Mode mode) {
        this.apiKey = apiKey;
        this.mode = mode;

        if(this.mode == Mode.POLLING)
            startPolling();
    }

    private void startPolling() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            synchronized (cache) {
                new ArrayList<>(cache.keySet()).forEach(city -> {
                    try{
                        CachedWeather cached = cache.get(city);
                        long ageMinutes = Duration.between(
                                Instant.ofEpochMilli(cached.getLastUpdated()),
                                Instant.now()
                        ).toMinutes();

                        if (ageMinutes >= 10) {
                            String json = getJsonRequest(city);
                            if (json != null) {
                                cache.put(city, new CachedWeather(json));
                            }
                        }
                    }catch (Exception e){
                        LOGGER.error("Ошибка обновления данных для города: {}", city);
                    }
                });
            }
        }, 10, 1, TimeUnit.MINUTES);
    }

    public WeatherData getCurrentWeatherByCity(String city){
        if (city == null || city.trim().isEmpty())
            throw new WeatherSdkException("Название города не может быть пустым");

        String normalizedCity = city.trim().toLowerCase();

        synchronized (cache) {
            CachedWeather cachedWeather = cache.get(normalizedCity);
            if(cachedWeather != null && cachedWeather.isFresh()){
                try {
                    LOGGER.info("Возврат данных из кэша для города: {}", normalizedCity);
                    return objectMapper.readValue(cachedWeather.getJsonData(), WeatherData.class);
                } catch (JsonProcessingException e) {
                    throw new WeatherSdkException("Ошибка парсинга JSON из кэша", e);
                }
            }
        }

        String json = getJsonRequest(city);

        if(json == null) {
            throw new WeatherSdkException("Не удалось получить данные от API");
        }

        synchronized (cache) {
            CachedWeather cachedWeather = cache.get(normalizedCity);
            if(cachedWeather != null && cachedWeather.isFresh()){
                try {
                    LOGGER.info("Возврат данных из кэша (после повторной проверки): {}", normalizedCity);
                    return objectMapper.readValue(cachedWeather.getJsonData(), WeatherData.class);
                } catch (JsonProcessingException e) {
                    throw new WeatherSdkException("Ошибка парсинга JSON из кэша", e);
                }
            }

            cache.put(normalizedCity, new CachedWeather(json));
        }

        try {
            return objectMapper.readValue(json, WeatherData.class);
        } catch (JsonProcessingException e) {
            throw new WeatherSdkException("Ошибка парсинга JSON от API", e);
        }
    }

    public static synchronized WeatherSDK create(String apiKey, Mode mode){
        if (INSTANCES.containsKey(apiKey))
            throw new WeatherSdkException("SDK для этого API-ключа уже создан");

        WeatherSDK sdk = new WeatherSDK(apiKey, mode);
        INSTANCES.put(apiKey, sdk);
        return sdk;
    }

    public synchronized void remove() {
        if (this.executor != null)
            this.executor.shutdownNow();

        INSTANCES.remove(this.apiKey);
        LOGGER.info("Экземпляр SDK с API: {} успешно удален", this.apiKey);
    }

    public String getJsonRequest(String city){
        String normalizedCity = city.trim().toLowerCase();
        String url = String.format(
                "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s",
                normalizedCity, apiKey
        );

        HttpClient client = getHttpClient();
        HttpRequest request = getHttpRequest(url);

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOGGER.info("Успешный запрос к городу: {}, status запроса: {}", city, response.statusCode());
                LOGGER.info("Тело запроса {}", response.body());
                return response.body();
            } else if (response.statusCode() == 401) {
                LOGGER.error("Неверный API-ключ: {}", apiKey);
                throw new WeatherSdkException("Неверный API-ключ");
            } else if (response.statusCode() == 404) {
                LOGGER.error("Город не найден: {}", city);
                throw new WeatherSdkException("Город не найден");
            } else {
                throw new WeatherSdkException("Ошибка API: " + response.statusCode());
            }

        } catch (IOException e) {
            LOGGER.error("Ошибка при выполнении запроса к API", e);
            throw new WeatherSdkException("Ошибка сети / таймаута при обращении к OpenWeather API", e);
        } catch (InterruptedException e) {
            LOGGER.error("Запрос к API для города {} был прерван", city, e);
            Thread.currentThread().interrupt();
            throw new WeatherSdkException("Запрос к API был прерван", e);
        }
    }

    private HttpRequest getHttpRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
    }

    private HttpClient getHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

}
