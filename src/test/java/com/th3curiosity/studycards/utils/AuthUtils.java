package com.th3curiosity.studycards.utils;

import com.th3curiosity.studycards.dto.other.AuthResult;
import com.th3curiosity.studycards.dto.user.LoginRequest;
import com.th3curiosity.studycards.security.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class AuthUtils {

    private static final String SECRET = "testSecretKeyThatIsAtLeast32BytesLong123456";
    private static final long ACCESS_EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24 часа
    private static final long REFRESH_EXPIRATION_MS = 1000 * 60 * 60 * 24 * 7; // 7 дней

    private AuthUtils() {
        throw new UnsupportedOperationException("Утилитный класс, создать объект нельзя");
    }

    public static String encodePassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    public static LoginRequest generateLoginRequest(int usernameLength, int passwordLength) {
        LoginRequest request =  new LoginRequest();
        request.setUsername(generateUsername(usernameLength));
        request.setPassword(generatePassword(passwordLength));
        return request;
    }

    public static LoginRequest createLoginRequest(String username, String password) {
        LoginRequest request =  new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    public static String generateUsername(int length) {
        if (length <= 8) {return "user_N" + "@mail.ru";}
        String rawUsername = "user_" + System.currentTimeMillis() + ThreadLocalRandom.current();
        return rawUsername.substring(0, length - 8) + "@mail.ru";
    }

    public static String generatePassword(int length) {
        if (length <= 0) {return "";}
        return ("pass_" + System.currentTimeMillis() + ThreadLocalRandom.current()).substring(0, length);
    }

    public static String generateExpiredToken(String username) {
        Date expiredAt = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiredAt)
                .signWith(SignatureAlgorithm.HS256, SECRET.getBytes())
                .compact();
    }

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
