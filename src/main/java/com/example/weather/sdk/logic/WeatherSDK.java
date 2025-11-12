package com.example.weather.sdk.logic;

import com.example.weather.sdk.enums.Mode;
import com.example.weather.sdk.exceptions.WeatherSdkException;
import com.example.weather.sdk.logic.model.CachedWeather;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeatherSDK {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final String apiKey;
    private final Mode mode;

    public WeatherSDK(String apiKey, Mode mode) {
        this.apiKey = apiKey;
        this.mode = mode;

        if(this.mode == Mode.POLLING)
            startPolling();
    }

    private final Map<String, CachedWeather> cache = new LinkedHashMap<>(10, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedWeather> eldest) {
            return size() > 10;
        }
    };

    public static void main(String[] args) {
        WeatherSDK sdk = new WeatherSDK("23283c7e4ab31b10e93b49043febe4c5", Mode.POLLING);
        String response = sdk.getCurrentWeatherByCity("Samara");
        System.out.println(response);
    }

    public String getCurrentWeatherByCity(String city){
        CachedWeather cachedWeather = cache.get(city.trim().toLowerCase());

        if(cachedWeather != null && cachedWeather.isFresh()){
            LOGGER.info("Возврат данных из хеша: {}", cachedWeather.getJsonData());
            return cachedWeather.getJsonData();
        }

        String json = getJsonRequest(city);

        if(json != null){
            cache.put(city, new CachedWeather(json));
        }

        return json;
    }

    public String getJsonRequest(String city){
        String url = String.format(
                "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s",
                city, apiKey
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

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Ошибка при выполнении запроса к API", e);
            throw new WeatherSdkException("Ошибка сети / таймаута при обращении к OpenWeather API", e);
        }
    }

    private void startPolling() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            synchronized (cache) {
                cache.forEach((city, cached) -> {
                    long ageMinutes = Duration.between(
                            Instant.ofEpochMilli(cached.getLastUpdated()),
                            Instant.now()
                    ).toMinutes();

                    if (ageMinutes >= 10) {
                        LOGGER.info("Обновление данных по городу: {}", city);
                        String json = getJsonRequest(city);
                        if (json != null) {
                            cache.put(city, new CachedWeather(json));
                        }
                    }
                });
            }
        }, 0, 1, TimeUnit.MINUTES);
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
/// -------------------------- исключения --------------------------
/// throw new WeatherSdkException("неверный API ключ");
/// throw new WeatherSdkException("ошибки сети / тайм-ауты");
/// throw new WeatherSdkException("ошибки парсинга ответа");

/// https://api.openweathermap.org/data/2.5/weather?q=Moscow&appid=23283c7e4ab31b10e93b49043febe4c5