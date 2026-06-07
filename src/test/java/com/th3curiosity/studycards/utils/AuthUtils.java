package com.th3curiosity.studycards.utils;

import com.th3curiosity.studycards.dto.user.LoginRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.ThreadLocalRandom;

public class AuthUtils {

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

    public static String generateUsername(int length) {
        if (length <= 8) {return "user_N" + "@mail.ru";}
        String rawUsername = "user_" + System.currentTimeMillis() + ThreadLocalRandom.current();
        return rawUsername.substring(0, length - 8) + "@mail.ru";
    }

    public static String generatePassword(int length) {
        if (length <= 0) {return "";}
        return ("pass_" + System.currentTimeMillis() + ThreadLocalRandom.current()).substring(0, length);
    }
}
