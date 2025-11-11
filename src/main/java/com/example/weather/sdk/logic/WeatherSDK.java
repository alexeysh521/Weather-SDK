package com.example.weather.sdk.logic;

import com.example.weather.sdk.enums.Mode;
import com.example.weather.sdk.exceptions.WeatherSdkException;
import com.example.weather.sdk.logic.model.CachedWeather;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class WeatherSDK {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private String city;
    private final String apiKey;
    private final Mode mode;

    private final Map<String, CachedWeather> cache = new LinkedHashMap<>(10, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedWeather> eldest) {
            return size() > 10; // максимум 10 городов
        }
    };

    // https://api.openweathermap.org/data/2.5/weather?q=Moscow&appid=23283c7e4ab31b10e93b49043febe4c5

/// -------------------------- исключения --------------------------
/// throw new WeatherSdkException("Город не найден");
/// throw new WeatherSdkException("неверный API ключ");
/// throw new WeatherSdkException("ошибки сети / тайм-ауты");
/// throw new WeatherSdkException("ошибки парсинга ответа");

    public WeatherSDK(String apiKey, String mode) {
        this.apiKey = apiKey;
        this.mode = Mode.toMode(mode);

        if(this.mode == Mode.POLLING){
            // запуск фонового метода
        }
    }

    public static void main(String[] args) {
        WeatherSDK sdk = new WeatherSDK("23283c7e4ab31b10e93b49043febe4c5", "POLLING");
        String response = sdk.getCurrentWeatherByCity("Samara");
        System.out.println(response);
    }

    public String getCurrentWeatherByCity(String city){
        CachedWeather cachedWeather = cache.get(city);

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

        // Создаем HTTP клиент
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Создаем запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        try {
            // Отправляем запрос и получаем ответ
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOGGER.info("Успешный запрос к городу: {}, status запроса: {}", city, response.statusCode());
                LOGGER.info("Тело запроса {}", response.body());
                return response.body();
            }
            else {
                LOGGER.error("Ошибка запроса, проверьте правильность написания города {}", city);
                LOGGER.error("Тело запроса {}", response.body());
                throw new WeatherSdkException("Город не найден");
            }

        } catch (Exception e) {
            LOGGER.error("Ошибка при выполнении запроса к API", e);
            throw new WeatherSdkException("Ошибка при обращении к OpenWeather API");
        }
    }

}
