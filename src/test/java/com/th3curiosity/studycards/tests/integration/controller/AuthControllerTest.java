package com.th3curiosity.studycards.tests.integration.controller;

import com.th3curiosity.studycards.base.BaseApiTest;
import com.th3curiosity.studycards.base.BaseApiAssertions;
import com.th3curiosity.studycards.data.AuthData;
import com.th3curiosity.studycards.data.Endpoints;
import com.th3curiosity.studycards.data.Error;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
            Response response = login(AuthData.USERNAME_1, AuthData.PASSWORD_1);

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
            Response response = login(AuthData.USERNAME_1, AuthData.PASSWORD_1);

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
            Response response = login(AuthData.USERNAME_1, AuthData.PASSWORD_1);
            BaseApiAssertions.assertHeaders(response);
        }

        @Test
        @DisplayName("Проверка работоспособности токена авторизации")
        void login_TokenCanBeUsedForAuthorization() {
            Response response = login(AuthData.USERNAME_1, AuthData.PASSWORD_1);
            String accessToken = response.path("accessToken");

            given()
                    .spec(requestSpecification)
                    .auth().oauth2(accessToken)
                    .get(Endpoints.USERS)
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
            BaseApiAssertions.assertErrorResponse(response, 401,
                    Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.Message.INVALID_USERNAME_OR_PASSWORD);
        }

        @ParameterizedTest
        @DisplayName("Несуществующие логин или пароль")
        @CsvSource({
                AuthData.USERNAME_1 + ", invalid-password",
                "invalid-login, " + AuthData.PASSWORD_1
        })
        void login_InvalidCredentials_ShouldReturn401(String login, String password) {
            Response response = login(login, password);
            BaseApiAssertions.assertErrorResponse(response, 401,
                    Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.Message.INVALID_USERNAME_OR_PASSWORD);
        }

        @ParameterizedTest
        @DisplayName("Нет значения логина или пароля")
        @ValueSource(strings = {
                "{\"username\": null, \"password\": \"" + AuthData.PASSWORD_1 + "\"}",
                "{\"username\": \"" + AuthData.USERNAME_1 + "\", \"password\": null}"
        })
        void login_CredentialsIsNull_ShouldReturn401(String authData) {
            Response response = login(authData);
            BaseApiAssertions.assertErrorResponse(response, 401,
                    Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.Message.INVALID_USERNAME_OR_PASSWORD);
        }

        @ParameterizedTest
        @DisplayName("Передан только логин или только пароль")
        @ValueSource(strings =
                {
                        "{\"username\": \"" + AuthData.USERNAME_1 + "\"}",
                        "{\"password\": \"" + AuthData.PASSWORD_1 + "\"}"
                })
        void login_WithoutKey_ShouldReturn401(String authData) {
            Response response = login(authData);
            BaseApiAssertions.assertErrorResponse(response, 401,
                    Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.Message.INVALID_USERNAME_OR_PASSWORD);
        }

        @ParameterizedTest
        @DisplayName("Пустой логин или пароль")
        @ValueSource(strings = {
                "{\"username\": \"\", \"password\": \"" + AuthData.PASSWORD_1 + "\"}",
                "{\"username\": \"" + AuthData.USERNAME_1 + "\", \"password\": \"\"}"
        })
        void login_EmptyCredentials_ShouldReturn401(String authData) {
            Response response = login(authData);
            BaseApiAssertions.assertErrorResponse(response, 401,
                    Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.Message.INVALID_USERNAME_OR_PASSWORD);
        }

        @Test
        @DisplayName("Проверка заголовков ответа")
        void login_CheckHeaders_AllHeadersShouldPresent() {
            Response response = login(AuthData.USERNAME_1, "invalid-password");
            BaseApiAssertions.assertErrorResponse(response, 401,
                    Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.Message.INVALID_USERNAME_OR_PASSWORD);
            BaseApiAssertions.assertHeaders(response);
        }

        @Disabled("Неучтённость - больше подойдёт статус-код 405")
        @ParameterizedTest
        @DisplayName("Проверка неподдерживаемых методов")
        @ValueSource(strings = {"GET", "DELETE", "PUT", "PATCH"})
        void login_MethodNotAllowed_ShouldReturn405(String wrongMethod) {
            BaseApiAssertions.assertMethodNotAllowed(requestSpecification, Endpoints.LOGIN, wrongMethod, "POST");
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

            BaseApiAssertions.assertErrorResponse(response, 400,
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

            BaseApiAssertions.assertErrorResponse(response, 400,
                    "BAD_REQUEST", "Тело запроса отсутствует");
        }
    }
}
