package com.th3curiosity.studycards.data;

public class Error {

    public static class Code {

        public static final String UNAUTHORIZED = "Unauthorized";
        public static final String INVALID_USERNAME_OR_PASSWORD = "INVALID_USERNAME_OR_PASSWORD";
        public static final String BAD_REQUEST = "BAD_REQUEST";
        public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
        public static final String DECK_NOT_FOUND = "DECK_NOT_FOUND";
        public static final String NOT_FOUND = "NOT_FOUND";
    }

    public static class ResponseMessage {

        public static final String UNAUTHORIZED = "Требуется аутентификация для доступа к ресурсу";
        public static final String INVALID_USERNAME_OR_PASSWORD = "InvalidUsernameOrPassword";
        public static final String BAD_REQUEST = "BAD_REQUEST";
        public static final String INTERNAL_SERVER_ERROR = "Внутренняя ошибка сервера";
        public static final String DECK_NOT_FOUND = "Колода не найдена";
        public static final String DECK_NOT_FOUND_BY_USER = "Deck with id %d not found for user %s";
    }

    public static class ServiceMessage {
        public static final String USER_NOT_FOUND = "User not found with username: ";
        public static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";
    }
}
