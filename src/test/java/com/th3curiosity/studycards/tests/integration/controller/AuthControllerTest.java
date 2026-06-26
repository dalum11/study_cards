package com.th3curiosity.studycards.tests.integration.controller;

import com.th3curiosity.studycards.base.BaseApiTest;
import com.th3curiosity.studycards.base.BaseApiAssertions;
import com.th3curiosity.studycards.data.AuthData;
import com.th3curiosity.studycards.data.Endpoints;
import com.th3curiosity.studycards.data.Error;
import com.th3curiosity.studycards.data.SuccessData;
import com.th3curiosity.studycards.utils.AuthUtils;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.util.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты авторизации")
public class AuthControllerTest extends BaseApiTest {

    @Nested
    @DisplayName("Тесты эндпоинта login")
    class Login {

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
                        Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.ResponseMessage.INVALID_USERNAME_OR_PASSWORD);
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
                        Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.ResponseMessage.INVALID_USERNAME_OR_PASSWORD);
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
                        Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.ResponseMessage.INVALID_USERNAME_OR_PASSWORD);
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
                        Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.ResponseMessage.INVALID_USERNAME_OR_PASSWORD);
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
                        Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.ResponseMessage.INVALID_USERNAME_OR_PASSWORD);
            }

            @Test
            @DisplayName("Проверка заголовков ответа")
            void login_CheckHeaders_AllHeadersShouldPresent() {
                Response response = login(AuthData.USERNAME_1, "invalid-password");
                BaseApiAssertions.assertErrorResponse(response, 401,
                        Error.Code.INVALID_USERNAME_OR_PASSWORD, Error.ResponseMessage.INVALID_USERNAME_OR_PASSWORD);
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


    private void assertHeaders(Response response) {
        List<Header> expectedHeaders = List.of(
                new Header("X-Content-Type-Options", "nosniff"),
                new Header("X-XSS-Protection", "0"),
                new Header("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"),
                new Header("Pragma", "no-cache"),
                new Header("X-Frame-Options", "DENY"),
                new Header("Expires", "0"),
                new Header("Content-Type", "text/plain;charset=UTF-8"),
                new Header("Date", ""),
                new Header("Keep-Alive", "timeout=60"),
                new Header("Connection", "keep-alive")
        );
        BaseApiAssertions.assertHeaders(response, expectedHeaders);
    }

    private String loginAndGetRefreshToken(String username, String password) {
        return login(username, password).getCookie("refreshToken");
    }

    private void tryRefreshSessionWithRefreshToken(String refreshToken, int statusCode) {
        given()
                .spec(requestSpecification)
                .cookie("refreshToken", refreshToken)
                .when()
                .post(Endpoints.REFRESH)
                .then()
                .statusCode(statusCode);
    }

    @Nested
    @DisplayName("Тесты для разлогина со всех устройств")
    class LogoutAll {

        @Nested
        @DisplayName("Успешный разлогин со всех устройств")
        class LogoutAllSuccessful {

            @Test
            @DisplayName("Разлогин с валидным токеном")
            void logout_ValidToken_ShouldReturn200() {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                Response logoutResult = logout(refreshToken, SuccessData.LogoutType.LOGOUT_ALL, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResult);
                BaseApiAssertions
                        .assertTextResponse(logoutResult, HttpStatus.OK.value(), SuccessData.AuthMessage.SUCCESS_LOGOUT);
            }

            @ParameterizedTest
            @DisplayName("Разлогин с нескольких устройств сразу")
            @ValueSource(ints = {1, 5, 10})
            @Disabled("Баг - создаются неуникальные токены")
            void logout_MoreThanOneTokens_ShouldReturn200(int tokensCount) {
                List<String> refreshTokens = new ArrayList<>();
                for (int i = 0; i < tokensCount; i++) {
                    String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_3, AuthData.PASSWORD_3);
                    refreshTokens.add(refreshToken);
                }

                assertThat(refreshTokens)
                        .as("Количество токенов должно быть %d", tokensCount)
                        .isNotEmpty()
                        .hasSize(tokensCount);

                assertThat(refreshTokens)
                        .as("Не должно быть дублей")
                        .doesNotHaveDuplicates();

                Response logoutResult = logout(refreshTokens.get(0), SuccessData.LogoutType.LOGOUT_ALL, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResult);

                for (String refreshToken : refreshTokens) {
                    tryRefreshSessionWithRefreshToken(refreshToken, HttpStatus.UNAUTHORIZED.value());
                }
            }
        }

        @Nested
        @DisplayName("Неуспешный разлогин со всех устройств")
        class LogoutAllError {

            @Test
            @DisplayName("Токен невалиден")
            void logout_InvalidToken_ShouldReturn401() {
                String refreshToken = "invalid-token";
                Response logoutResult = logout(refreshToken, SuccessData.LogoutType.LOGOUT_ALL, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.UNAUTHORIZED.value(),
                        Error.ServiceMessage.INVALID_REFRESH_TOKEN);
            }

            @Test
            @DisplayName("Токен истёк")
            void logout_ExpiredToken_ShouldReturn401() {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                assertThat(refreshToken).isNotBlank();

                String expiredRefreshToken = AuthUtils.generateExpiredToken(AuthData.USERNAME_1);

                Response logoutResponse = logout(expiredRefreshToken, SuccessData.LogoutType.LOGOUT_ALL, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResponse);
                BaseApiAssertions.assertTextResponse(logoutResponse, HttpStatus.UNAUTHORIZED.value(),
                        Error.ServiceMessage.INVALID_REFRESH_TOKEN);

                tryRefreshSessionWithRefreshToken(expiredRefreshToken, HttpStatus.UNAUTHORIZED.value());
            }

            @Test
            @DisplayName("Токен неизвестен (null)")
            @Disabled("Неучтённость - больше подошла бы ошибка 400")
            void logout_NullRefreshToken_ShouldReturn400() {
                Response logoutResult = logout(null, SuccessData.LogoutType.LOGOUT_ALL, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @Test
            @DisplayName("Передача access token в куки вместо refresh token")
            void logout_AccessTokenInCookie_ShouldReturn401() {
                String accessToken = AuthUtils.generateAccessToken(AuthData.USERNAME_1);
                login(AuthData.USERNAME_1, AuthData.PASSWORD_1);

                Response logoutResult = logout(accessToken, SuccessData.LogoutType.LOGOUT_ALL, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.UNAUTHORIZED.value(),
                        Error.ServiceMessage.INVALID_REFRESH_TOKEN);
            }

            @ParameterizedTest
            @DisplayName("Токен - пустая строка или пробелы")
            @ValueSource(strings = {"", " ", "  "})
            @Disabled("Неучтённость - возвращается 401")
            void logout_EmptyOrBlankRefreshToken(String refreshToken) {
                Response logoutResult = logout(refreshToken, SuccessData.LogoutType.LOGOUT_ALL, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @ParameterizedTest
            @Disabled("Баг - ответ 200 на неподдерживаемый тип")
            @DisplayName("Необрабатываемой системой тип разлогина")
            @ValueSource(strings = {"alll", "al", "1234567890", "!@#$"})
            void logout_UnexpectedLogoutType_ShouldReturn400(String logoutType) {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                Response logoutResult = logout(refreshToken, logoutType, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @ParameterizedTest
            @Disabled("Баг - ответ 200 на тип в другом регистре")
            @DisplayName("Разный регистр типов разлогина")
            @ValueSource(strings = {"AlL", "aLl", "ALl", "aLL"})
            void logout_LogoutTypeDifferentCases_ShouldReturn400(String logoutType) {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                Response logoutResult = logout(refreshToken, logoutType, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @Test
            @Disabled("Баг - ответ 200 на null тип")
            @DisplayName("Тип разлогина - null")
            void logout_NullLogoutType_ShouldReturn400() {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                Response logoutResult = logout(refreshToken, null, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @ParameterizedTest
            @Disabled("Баг - ответ 200 на пустой или пробельный тип")
            @DisplayName("Тип разлогина - пустая строка или пробелы")
            @ValueSource(strings = {"", " ", "  "})
            void logout_EmptyOrBlankLogoutType_ShouldReturn400(String logoutType) {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                Response logoutResult = logout(refreshToken, logoutType, Endpoints.LOGOUT_ALL);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @ParameterizedTest
            @Disabled("Неучтённость - больше подойдёт код 405")
            @DisplayName("Неподдерживаемые методы")
            @ValueSource(strings = {"GET", "PATCH", "DELETE", "PUT"})
            void logout_UnexpectedMethod_ShouldReturn405(String wrongMethod) {
                BaseApiAssertions.assertMethodNotAllowed(requestSpecification, Endpoints.LOGOUT_ALL,
                        wrongMethod, "POST");
            }

            @Test
            @DisplayName("Logout без куки")
            @Disabled("Неучтённость - больше подойдёт ответ 400")
            void logout_withoutCookie_shouldReturn400() {
                given()
                        .spec(requestSpecification)
                        .queryParam("type", SuccessData.LogoutType.LOGOUT_ALL)
                        .when()
                        .post(Endpoints.LOGOUT_ALL)
                        .then()
                        .statusCode(HttpStatus.BAD_REQUEST.value());
            }

            @Test
            @DisplayName("Logout с тем же токеном")
            void logout_sameTokenTwice_shouldReturn401() {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);

                Response firstLogout = logout(refreshToken, SuccessData.LogoutType.LOGOUT_ALL, Endpoints.LOGOUT_ALL);
                assertThat(firstLogout.statusCode()).isEqualTo(HttpStatus.OK.value());

                Response secondLogout = logout(refreshToken, SuccessData.LogoutType.LOGOUT_ALL, Endpoints.LOGOUT_ALL);
                assertThat(secondLogout.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            }
        }
    }

    @Nested
    @DisplayName("Тесты разлогина на одном устройстве")
    class LogoutCurrent {

        @Nested
        @DisplayName("Успешный разлогин на одном устройстве")
        class LogoutCurrentSuccessful {

            @Test
            @DisplayName("Успешный разлогин на одном устройстве")
            void logoutCurrent_SuccessfulLogout() {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                Response logoutResult = logout(refreshToken, SuccessData.LogoutType.LOGOUT_CURRENT, Endpoints.LOGOUT_CURRENT);

                assertHeaders(logoutResult);
                BaseApiAssertions
                        .assertTextResponse(logoutResult, HttpStatus.OK.value(), SuccessData.AuthMessage.SUCCESS_LOGOUT);
            }

            @Test
            @DisplayName("Разлогин на одном устройстве не влияет на другие устройства")
            void logoutCurrent_SingleLogout_ShouldLogoutOnlyOneToken() {
                String refreshToken1 = loginAndGetRefreshToken(AuthData.USERNAME_3, AuthData.PASSWORD_3);
                String refreshToken2 = loginAndGetRefreshToken(AuthData.USERNAME_2, AuthData.PASSWORD_2);

                Response logoutResult = logout(refreshToken1, SuccessData.LogoutType.LOGOUT_CURRENT, Endpoints.LOGOUT_CURRENT);

                BaseApiAssertions
                        .assertTextResponse(logoutResult, HttpStatus.OK.value(), SuccessData.AuthMessage.SUCCESS_LOGOUT);

                tryRefreshSessionWithRefreshToken(refreshToken1, HttpStatus.UNAUTHORIZED.value());
                tryRefreshSessionWithRefreshToken(refreshToken2, HttpStatus.OK.value());
            }
        }

        @Nested
        @DisplayName("Тесты неуспешного разлогина с одного устройства")
        class LogoutCurrentError {

            @Test
            @DisplayName("Токен невалиден")
            void logoutCurrent_InvalidToken_ShouldReturn401() {
                String refreshToken = "invalid-token";
                Response logoutResult = logout(refreshToken, SuccessData.LogoutType.LOGOUT_CURRENT, Endpoints.LOGOUT_CURRENT);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.UNAUTHORIZED.value(),
                        Error.ServiceMessage.INVALID_REFRESH_TOKEN);
            }

            @Test
            @DisplayName("Токен истёк")
            void logoutCurrent_ExpiredToken_ShouldReturn401() {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                assertThat(refreshToken).isNotBlank();

                String expiredRefreshToken = AuthUtils.generateExpiredToken(AuthData.USERNAME_1);

                Response logoutResponse = logout(expiredRefreshToken, SuccessData.LogoutType.LOGOUT_CURRENT, Endpoints.LOGOUT_CURRENT);

                assertHeaders(logoutResponse);
                BaseApiAssertions.assertTextResponse(logoutResponse, HttpStatus.UNAUTHORIZED.value(),
                        Error.ServiceMessage.INVALID_REFRESH_TOKEN);

                tryRefreshSessionWithRefreshToken(expiredRefreshToken, HttpStatus.UNAUTHORIZED.value());
            }

            @Test
            @DisplayName("Токен неизвестен (null)")
            @Disabled("Неучтённость - больше подошла бы ошибка 400")
            void logoutCurrent_NullRefreshToken_ShouldReturn400() {
                Response logoutResult = logout(null, SuccessData.LogoutType.LOGOUT_CURRENT, Endpoints.LOGOUT_CURRENT);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @Test
            @DisplayName("Передача access token в куки вместо refresh token")
            void logoutCurrent_AccessTokenInCookie_ShouldReturn401() {
                String accessToken = AuthUtils.generateAccessToken(AuthData.USERNAME_1);
                login(AuthData.USERNAME_1, AuthData.PASSWORD_1);

                Response logoutResult = logout(accessToken, SuccessData.LogoutType.LOGOUT_CURRENT, Endpoints.LOGOUT_CURRENT);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.UNAUTHORIZED.value(),
                        Error.ServiceMessage.INVALID_REFRESH_TOKEN);
            }

            @ParameterizedTest
            @DisplayName("Токен - пустая строка или пробелы")
            @ValueSource(strings = {"", " ", "  "})
            @Disabled("Неучтённость - возвращается 401")
            void logoutCurrent_EmptyOrBlankRefreshToken(String refreshToken) {
                Response logoutResult = logout(refreshToken, SuccessData.LogoutType.LOGOUT_CURRENT, Endpoints.LOGOUT_CURRENT);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @ParameterizedTest
            @Disabled("Баг - ответ 200 на неподдерживаемый тип")
            @DisplayName("Необрабатываемой системой тип разлогина")
            @ValueSource(strings = {"currentt", "curren", "1234567890", "!@#$"})
            void logoutCurrent_UnexpectedLogoutType_ShouldReturn400(String logoutType) {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                Response logoutResult = logout(refreshToken, logoutType, Endpoints.LOGOUT_CURRENT);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @ParameterizedTest
            @Disabled("Баг - ответ 200 на тип в другом регистре")
            @DisplayName("Разный регистр типов разлогина")
            @ValueSource(strings = {"CurrenT", "cURRENt", "CuRrEnT", "cuRRent"})
            void logoutCurrent_LogoutTypeDifferentCases_ShouldReturn400(String logoutType) {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                Response logoutResult = logout(refreshToken, logoutType, Endpoints.LOGOUT_CURRENT);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @Test
            @Disabled("Баг - ответ 200 на null тип")
            @DisplayName("Тип разлогина - null")
            void logoutCurrent_NullLogoutType_ShouldReturn400() {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                Response logoutResult = logout(refreshToken, null, Endpoints.LOGOUT_CURRENT);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @ParameterizedTest
            @Disabled("Баг - ответ 200 на пустой или пробельный тип")
            @DisplayName("Тип разлогина - пустая строка или пробелы")
            @ValueSource(strings = {"", " ", "  "})
            void logoutCurrent_EmptyOrBlankLogoutType_ShouldReturn400(String logoutType) {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                Response logoutResult = logout(refreshToken, logoutType, Endpoints.LOGOUT_CURRENT);

                assertHeaders(logoutResult);
                BaseApiAssertions.assertTextResponse(logoutResult, HttpStatus.BAD_REQUEST.value(),
                        Error.ResponseMessage.BAD_REQUEST);
            }

            @ParameterizedTest
            @Disabled("Неучтённость - больше подойдёт код 405")
            @DisplayName("Неподдерживаемые методы")
            @ValueSource(strings = {"GET", "PATCH", "DELETE", "PUT"})
            void logoutCurrent_UnexpectedMethod_ShouldReturn405(String wrongMethod) {
                BaseApiAssertions.assertMethodNotAllowed(requestSpecification, Endpoints.LOGOUT_CURRENT,
                        wrongMethod, "POST");
            }

            @Test
            @DisplayName("Logout без куки")
            @Disabled("Неучтённость - больше подойдёт ответ 400")
            void logoutCurrent_withoutCookie_shouldReturn400() {
                given()
                        .spec(requestSpecification)
                        .queryParam("type", SuccessData.LogoutType.LOGOUT_CURRENT)
                        .when()
                        .post(Endpoints.LOGOUT_CURRENT)
                        .then()
                        .statusCode(HttpStatus.BAD_REQUEST.value());
            }

            @Test
            @DisplayName("Logout с тем же токеном")
            void logoutCurrent_sameTokenTwice_shouldReturn401() {
                String refreshToken = loginAndGetRefreshToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);

                Response firstLogout = logout(refreshToken, SuccessData.LogoutType.LOGOUT_CURRENT, Endpoints.LOGOUT_CURRENT);
                assertThat(firstLogout.statusCode()).isEqualTo(HttpStatus.OK.value());

                Response secondLogout = logout(refreshToken, SuccessData.LogoutType.LOGOUT_CURRENT, Endpoints.LOGOUT_CURRENT);
                assertThat(secondLogout.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            }
        }
    }
}
