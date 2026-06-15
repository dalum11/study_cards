package com.th3curiosity.studycards.tests.integration.controller;

import com.th3curiosity.studycards.base.BaseApiTest;
import com.th3curiosity.studycards.base.BaseApiAssertions;
import com.th3curiosity.studycards.data.AuthData;
import com.th3curiosity.studycards.data.Endpoints;
import com.th3curiosity.studycards.data.Error;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Тесты для работы с профилем")
public class UserControllerTest extends BaseApiTest {

    private String accessToken;

    private ValidatableResponse getCurrentUser() {
        return given()
                .spec(requestSpecification)
                .auth().oauth2(accessToken)
                .log().all()
                .get(Endpoints.USERS)
                .then()
                .log().all();
    }

    @Nested
    @DisplayName("Получение профиля текущего пользователя")
    class GetCurrentUserSuccess {

        @BeforeEach
        void setUp() {
            Response response = login(AuthData.USERNAME_1, AuthData.PASSWORD_1);
            accessToken = response.path("accessToken");
        }

        @Test
        @DisplayName("Получение профиля существующего пользователя")
        void currentUser_GetExistingUser_ShouldReturnUser() {
            getCurrentUser()
                    .statusCode(200)
                    .body("id", notNullValue())
                    .body("username", equalTo(AuthData.USERNAME_1));
        }

        @Test
        @DisplayName("Проверка типов данных")
        void currentUser_ShouldReturnCorrectDataTypes() {
            getCurrentUser()
                    .statusCode(200)
                    .body("id", instanceOf(Integer.class))
                    .body("username", instanceOf(String.class));
        }

        @Test
        @DisplayName("Проверка заголовков ответа")
        void currentUser_CheckHeaders_AllHeadersShouldBePresent() {
            ValidatableResponse validatableResponse = getCurrentUser();
            BaseApiAssertions.assertHeaders(validatableResponse.extract().response());
        }
    }

    @Nested
    @DisplayName("Ошибка при получении профиля пользователя")
    class GetCurrentUserError {

        @Test
        @DisplayName("Нет токена доступа к профилю")
        void currentUser_NoToken_ShouldReturn401() {
            Response response = given()
                    .spec(requestSpecification)
                    .log().all()
                    .get(Endpoints.USERS)
                    .then()
                    .log().all()
                    .extract().response();

            BaseApiAssertions.assertErrorResponse(response, 401,
                    Error.Code.UNAUTHORIZED, Error.Message.UNAUTHORIZED, Endpoints.USERS);
        }

        @Test
        @DisplayName("Невалидный токен")
        void currentUser_InvalidToken_ShouldReturn401() {
            String invalidAccessToken = "eyJhbGciOiJIUzI1NiJ9eyJzdWIiOiJ5dXJpLXBsaXNAZXhhbXBsZS5jb20iLCJpYXQiOjE3ODA1N" +
                    "zY3ODEsImV4cCI6MTc4MDU3NzY4MX0uJla94yuUgkHTX6bohpQUSskNG0WwhcEDOzho7GhM30";

            Response response = given()
                    .spec(requestSpecification)
                    .auth().oauth2(invalidAccessToken)
                    .log().all()
                    .get(Endpoints.USERS)
                    .then()
                    .log().all()
                    .extract().response();

            BaseApiAssertions.assertErrorResponse(response, 401,
                    Error.Code.UNAUTHORIZED, Error.Message.UNAUTHORIZED, Endpoints.USERS);
        }

        @Test
        @DisplayName("Истекший токен")
        void currentUser_TokenExpired_ShouldReturn401() {
            String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ5dXJpLXBsaXNAZXhhbXBsZS5jb20iLCJpYXQiOjE3ODA1Nzgz" +
                    "MzEsImV4cCI6MTc4MDU3OTIzMX0.z2oF3tJ9hq1jM3o9ZQz70NaFD_Z--qWRPqXa011-bTw";

            Response response = given()
                    .spec(requestSpecification)
                    .auth().oauth2(expiredToken)
                    .log().all()
                    .get(Endpoints.USERS)
                    .then()
                    .log().all()
                    .extract().response();

            BaseApiAssertions.assertErrorResponse(response, 401,
                    Error.Code.UNAUTHORIZED, Error.Message.UNAUTHORIZED, Endpoints.USERS);
        }
    }
}
