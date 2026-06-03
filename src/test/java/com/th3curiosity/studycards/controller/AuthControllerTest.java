package com.th3curiosity.studycards.controller;

import com.th3curiosity.studycards.base.BaseApiTest;
import com.th3curiosity.studycards.base.BaseAssertions;
import com.th3curiosity.studycards.data.AuthData;
import com.th3curiosity.studycards.data.Endpoints;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты авторизации")
public class AuthControllerTest extends BaseApiTest {

    @Nested
    @DisplayName("Тесты успешной авторизации")
    class SuccessfulLogin {

        @Test
        @DisplayName("Проверка токена при успешной авторизации")
        void login_CheckJwtToken_ReturnsJwtToken() {
            Response response = login(AuthData.USERNAME, AuthData.PASSWORD);

            assertThat(response.statusCode()).as("Код ответа должен быть 200").isEqualTo(200);

            String accessToken = response.path("accessToken");

            assertThat(accessToken)
                    .as("Токен должен соответствовать шаблону")
                    .isNotNull()
                    .isNotBlank()
                    .startsWith("ey")
                    .doesNotContain(" ");

            String[] parts = accessToken.split("\\.");
            assertThat(parts).as("Токен должен быть разбит точками на три части").hasSize(3);
        }

        @Test
        @DisplayName("Проверка куки в заголовках")
        void login_CheckRefreshCookie_CookieShouldPresent() {
            Response response = login(AuthData.USERNAME, AuthData.PASSWORD);

            String cookie = response.getHeader("Set-Cookie");

            assertThat(cookie)
                    .as("Cookie должен содержать определённую информацию")
                    .isNotNull()
                    .isNotBlank()
                    .contains("refreshToken")
                    .contains("HttpOnly")
                    .contains("Path=/api/auth/refresh");
        }

        @Test
        @DisplayName("Проверка заголовок ответа")
        void login_CheckHeaders_AllHeadersShouldPresent() {
            List<Header> expectedHeaders = List.of(
                    new Header("X-Content-Type-Options", "nosniff"),
                    new Header("X-XSS-Protection", "0"),
                    new Header("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"),
                    new Header("Pragma", "no-cache"),
                    new Header("X-Frame-Options", "DENY"),
                    new Header("Expires", "0"),
                    new Header("Content-Type", "application/json"),
                    new Header("Date", ""),
                    new Header("Keep-Alive", "timeout=60"),
                    new Header("Connection", "keep-alive")
            );

            Response response = login(AuthData.USERNAME, AuthData.PASSWORD);

            BaseAssertions.assertHeaders(response, expectedHeaders);
        }

        @Test
        @DisplayName("Проверка работоспособности токена авторизации")
        void login_TokenCanBeUsedForAuthorization() {
            Response response = login(AuthData.USERNAME, AuthData.PASSWORD);
            String accessToken = response.path("accessToken");

            given()
                    .spec(requestSpecification)
                    .auth().oauth2(accessToken)
                    .get("/api/users/me")
                    .then()
                    .statusCode(200);
        }
    }

    @Nested
    @DisplayName("Ошибки авторизации")
    class ErrorLogin {

        @Test
        @DisplayName("Пользователь не существует")
        void login_UserNotExist_ShouldReturn401() {
            String notExistUsername = "not-exists@mail.ru";
            String notExistPassword = "not-exists-pass123";

            Response response = login(notExistUsername, notExistPassword);
            BaseAssertions.assertErrorResponse(response, 401,
                    "INVALID_USERNAME_OR_PASSWORD", "InvalidUsernameOrPassword");
        }

        @ParameterizedTest
        @DisplayName("Несуществующие логин или пароль")
        @CsvSource({
                AuthData.USERNAME + ", invalid-password",
                "invalid-login, " + AuthData.PASSWORD
        })
        void login_InvalidCredentials_ShouldReturn401(String login, String password) {
            Response response = login(login, password);
            BaseAssertions.assertErrorResponse(response, 401,
                    "INVALID_USERNAME_OR_PASSWORD", "InvalidUsernameOrPassword");
        }

        @ParameterizedTest
        @DisplayName("Нет значения логина или пароля")
        @ValueSource(strings = {
                "{\"username\": null, \"password\": \"" + AuthData.PASSWORD + "\"}",
                "{\"username\": \"" + AuthData.USERNAME + "\", \"password\": null}"
        })
        void login_CredentialsIsNull_ShouldReturn401(String authData) {
            Response response = login(authData);
            BaseAssertions.assertErrorResponse(response, 401,
                    "INVALID_USERNAME_OR_PASSWORD", "InvalidUsernameOrPassword");
        }

        @ParameterizedTest
        @DisplayName("Передан только логин или только пароль")
        @ValueSource(strings =
                {
                        "{\"username\": \"" + AuthData.USERNAME + "\"}",
                        "{\"password\": \"" + AuthData.PASSWORD + "\"}"
                })
        void login_WithoutKey_ShouldReturn401(String authData) {
            Response response = login(authData);
            BaseAssertions.assertErrorResponse(response, 401,
                    "INVALID_USERNAME_OR_PASSWORD", "InvalidUsernameOrPassword");
        }

        @ParameterizedTest
        @DisplayName("Пустой логин или пароль")
        @ValueSource(strings = {
                "{\"username\": \"\", \"password\": \"" + AuthData.PASSWORD + "\"}",
                "{\"username\": \"" + AuthData.USERNAME + "\", \"password\": \"\"}"
        })
        void login_EmptyCredentials_ShouldReturn401(String authData) {
            Response response = login(authData);
            BaseAssertions.assertErrorResponse(response, 401,
                    "INVALID_USERNAME_OR_PASSWORD", "InvalidUsernameOrPassword");
        }

        @Test
        @DisplayName("Проверка заголовков ответа")
        void login_CheckHeaders_AllHeadersShouldPresent() {
            List<Header> expectedHeaders = List.of(
                    new Header("X-Content-Type-Options", "nosniff"),
                    new Header("X-XSS-Protection", "0"),
                    new Header("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"),
                    new Header("Pragma", "no-cache"),
                    new Header("X-Frame-Options", "DENY"),
                    new Header("Expires", "0"),
                    new Header("Content-Type", "application/json"),
                    new Header("Date", ""),
                    new Header("Keep-Alive", "timeout=60"),
                    new Header("Connection", "keep-alive")
            );

            Response response = login(AuthData.USERNAME, "invalid-password");
            BaseAssertions.assertErrorResponse(response, 401,
                    "INVALID_USERNAME_OR_PASSWORD", "InvalidUsernameOrPassword");
            BaseAssertions.assertHeaders(response, expectedHeaders);
        }

        @Disabled("Неучтённость - больше подойдёт статус-код 405")
        @ParameterizedTest
        @DisplayName("Проверка неподдерживаемых методов")
        @ValueSource(strings = {"GET", "DELETE", "PUT", "PATCH"})
        void login_MethodNotAllowed_ShouldReturn405(String wrongMethod) {
            BaseAssertions.assertMethodNotAllowed(requestSpecification, Endpoints.LOGIN, wrongMethod, "POST");
        }

        @Disabled("Неучтённость - ошибка 400 подходит больше")
        @Test
        @DisplayName("Проверка запроса без тела")
        void login_RequestWithoutBody_ShouldReturn400() {
            Response response = given()
                    .spec(requestSpecification)
                    .log().all()
                    .when()
                    .post(Endpoints.LOGIN)
                    .then()
                    .log().all()
                    .extract().response();

            BaseAssertions.assertErrorResponse(response, 400,
                    "BAD_REQUEST", "Тело запроса отсутствует");
        }

        @Disabled("Неучтённость - ошибка 400 подходит больше")
        @Test
        @DisplayName("Проверка запроса с пустым телом")
        void login_RequestWithEmptyBody_ShouldReturn400() {
            Response response = given()
                    .spec(requestSpecification)
                    .body("{}")
                    .log().all()
                    .when()
                    .post(Endpoints.LOGIN)
                    .then()
                    .log().all()
                    .extract().response();

            BaseAssertions.assertErrorResponse(response, 400,
                    "BAD_REQUEST", "Тело запроса отсутствует");
        }
    }
}
