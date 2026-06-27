package com.th3curiosity.studycards.utils;

import com.th3curiosity.studycards.entity.User;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Утилитный класс для создания тестовых данных (пользователя).
 * Используется только в тестах
 */
public class UserUtils {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, Month.APRIL, 11, 12, 0);
    private static final AtomicLong userIdGenerator = new AtomicLong(1);

    private UserUtils() {
        throw new UnsupportedOperationException("Утилитный класс, создать объект нельзя");
    }

    /**
     * Метод создаёт пользователя с определённым количеством колод
     *
     * @param username имя пользователя (не может быть пустой или null)
     * @param password пароль пользователя (не может быть пустой или null)
     * @param decksCount количество колод
     * @return пользователь
     */
    public static User createUser(String username, String password, int decksCount) {
        validateUserData(username, password);

        User user = createBaseUser(username, password);
        user.setDecks(DeckUtils.createDecks(decksCount, user));

        return user;
    }

    /**
     * Метод создаёт пользователя, у которого есть колоды с картами
     *
     * @param username почта пользователя (не может быть пустой или null)
     * @param password пароль пользователя (не может быть пустой или null)
     * @param decksCount количество колод
     * @param cardsCount количество карт в одной колоде
     * @return пользователя
     */
    public static User createUser(String username, String password, int decksCount, int cardsCount) {
        validateUserData(username, password);

        User user = createBaseUser(username, password);
        user.setDecks(DeckUtils.createDecksWithCards(decksCount, cardsCount, user));

        return user;
    }

    /**
     * Метод сбрасывает состояние счётчика для генерации id
     */
    public static void resetIdGenerator() {
        userIdGenerator.set(1);
    }

    /**
     * Метод возвращает пользователя без колод и карт
     *
     * @param username почта пользователя (не может быть пустой или null)
     * @param password пароль пользователя (не может быть пустой или null)
     * @return пользователя
     */
    public static User createBaseUser(String username, String password) {
        validateUserData(username, password);

        User user = new User();
        user.setId(userIdGenerator.getAndIncrement());
        user.setUsername(username);
        user.setPassword(password);
        user.setCreatedAt(FIXED_TIME);

        return user;
    }

    /**
     * Приватный метод для валидации данных создания пользователя
     *
     * @param username email пользователя (непустое и не null)
     * @param password пароль (непустой и не null)
     */
    private static void validateUserData(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Почта и пароль пользователя не могут быть пустыми/null");
        }
    }
}
