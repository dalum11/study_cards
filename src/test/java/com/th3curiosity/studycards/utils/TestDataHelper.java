package com.th3curiosity.studycards.utils;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * Утилитный класс для создания тестовых данных.
 * Используется только в тестах
 */
public class TestDataHelper {

    private TestDataHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Метод создаёт случайную строку заданной длины
     *
     * @param length длина строки
     * @return строку из случайных символов
     */
    public static String generateRandomString(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }
}
