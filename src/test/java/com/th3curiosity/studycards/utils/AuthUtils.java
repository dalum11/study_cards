package com.th3curiosity.studycards.utils;

import com.th3curiosity.studycards.dto.user.LoginRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Утилитный класс для создания тестовых данных авторизации.
 * Используется только в тестах
 */
public class AuthUtils {

    private static final String SECRET = "testSecretKeyThatIsAtLeast32BytesLong123456";
    private static final long ACCESS_EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24 часа
    private static final long REFRESH_EXPIRATION_MS = 1000 * 60 * 60 * 24 * 7; // 7 дней

    private AuthUtils() {
        throw new UnsupportedOperationException("Утилитный класс, создать объект нельзя");
    }

    /**
     * Метод позволяет закодировать пароль при создани/обновлении пароля пользователя
     *
     * @param password пароль в человекочитаемом виде
     * @return закодированный пароль в том виде, в котором он есть в БД
     */
    public static String encodePassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    /**
     * Метод позволяет сгенерировать DTO для запроса авторизации пользователя
     *
     * @param usernameLength длина email пользователя
     * @param passwordLength длина пароля пользователя
     * @return ДТО для запроса авторизации
     */
    public static LoginRequest generateLoginRequest(int usernameLength, int passwordLength) {
        LoginRequest request =  new LoginRequest();
        request.setUsername(generateUsername(usernameLength));
        request.setPassword(generatePassword(passwordLength));
        return request;
    }

    /**
     * Метод позволяет сгенерировать DTO запроса авторизации для конкретных данных пользователя
     *
     * @param username email пользователя
     * @param password пароль пользователя
     * @return ДТО для запроса авторизации
     */
    public static LoginRequest createLoginRequest(String username, String password) {
        LoginRequest request =  new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    /**
     * Метод генерирует email пользователя заданной длины
     *
     * @param length длина email
     * @return email
     */
    public static String generateUsername(int length) {
        if (length <= 8) {return "user_N" + "@mail.ru";}
        String rawUsername = "user_" + System.currentTimeMillis() + ThreadLocalRandom.current();
        return rawUsername.substring(0, length - 8) + "@mail.ru";
    }

    /**
     * Метод возвращает сгенерированный пароль заданной длины в человекочитаемом виде
     *
     * @param length длина пароля
     * @return пароль
     */
    public static String generatePassword(int length) {
        if (length <= 0) {return "";}
        return ("pass_" + System.currentTimeMillis() + ThreadLocalRandom.current()).substring(0, length);
    }

    /**
     * Метод возвращает истекший токен авторизации для конкретного пользователя
     *
     * @param username email пользователя
     * @return истекший токен авторизации
     */
    public static String generateExpiredToken(String username) {
        Date expiredAt = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiredAt)
                .signWith(SignatureAlgorithm.HS256, SECRET.getBytes())
                .compact();
    }

    /**
     * Метод возвращает валидный токен авторизации для конкретного пользователя
     *
     * @param username email пользователя
     * @return токен авторизации
     */
    public static String generateAccessToken(String username) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + ACCESS_EXPIRATION_MS);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiredAt)
                .signWith(SignatureAlgorithm.HS256, SECRET.getBytes())
                .compact();
    }

    /**
     * Метод возвращает валидный токен обновления для конкретного пользователя
     *
     * @param username email пользователя
     * @return токен обновления
     */
    public static String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + REFRESH_EXPIRATION_MS);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiredAt)
                .setId(UUID.randomUUID().toString())
                .signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256), SignatureAlgorithm.HS256)
                .compact();
    }
}
