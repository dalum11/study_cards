package com.th3curiosity.studycards.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Утилитный класс для работы с API и объектами.
 * Используется только в тестах
 */
public class ApiUtils {

    private ApiUtils() {
        throw new UnsupportedOperationException("Утилитный класс, создать объект нельзя");
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Метод превращает переданный объект в строковый JSON
     *
     * @param obj объект (любой) для сериализации
     * @return строковый JSON
     */
    public static String toJsonStr(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Не удалось привести DTO к строковому JSON: {}", e);
        }
    }
}
