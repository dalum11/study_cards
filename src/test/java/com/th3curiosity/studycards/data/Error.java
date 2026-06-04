package com.th3curiosity.studycards.data;

public class Error {

    public static class Code {

        public static final String UNAUTHORIZED = "Unauthorized";
        public static final String INVALID_USERNAME_OR_PASSWORD = "INVALID_USERNAME_OR_PASSWORD";
    }

    public static class Message {

        public static final String UNAUTHORIZED = "Требуется аутентификация для доступа к ресурсу";
        public static final String INVALID_USERNAME_OR_PASSWORD = "InvalidUsernameOrPassword";
    }
}
