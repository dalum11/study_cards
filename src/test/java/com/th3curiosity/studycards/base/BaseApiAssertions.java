package com.th3curiosity.studycards.base;

import com.th3curiosity.studycards.data.Error;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.RequestSpecification;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class BaseApiAssertions {

    public static void assertErrorResponse(Response response,
                                           int expectedStatus,
                                           String expectedError,
                                           String expectedMessage,
                                           String expectedPath) {
        ResponseBody body = response.getBody();
        String error = body.path("error");
        String message = body.path("message");
        String path = body.path("path");
        Long timestamp = body.path("timestamp");

        assertThat(response.getStatusCode())
                .as("Статус-код должен быть %d", expectedStatus)
                .isEqualTo(expectedStatus);

        assertThat(error)
                .as("Код ошибки должен быть %s", expectedError)
                .isEqualTo(expectedError);

        assertThat(message)
                .as("Текст ошибки должен быть %s", expectedMessage)
                .isEqualTo(expectedMessage);

        assertThat(path)
                .as("Путь должен быть %s", expectedMessage)
                .isEqualTo(expectedPath);

        assertThat(timestamp)
                .as("Время ответа должно быть")
                .isNotNull()
                .isInstanceOf(Long.class);
    }

    public static void assertErrorResponse(Response response,
                                           int expectedStatusCode,
                                           String expectedErrorCode,
                                           String expectedErrorMessage) {
        ResponseBody body = response.getBody();
        String code = body.path("code");
        String message = body.path("message");
        LocalDateTime timestamp = LocalDateTime.parse(body.path("timestamp"));

        assertThat(response.getStatusCode())
                .as("Статус-код должен быть %d", expectedStatusCode)
                .isEqualTo(expectedStatusCode);

        assertThat(code)
                .as("Код ошибки должен быть %s", expectedErrorCode)
                .isEqualTo(expectedErrorCode);

        assertThat(message)
                .as("Сообщение об ошибке должно быть %s", expectedErrorMessage)
                .isEqualTo(expectedErrorMessage);

        assertThat(timestamp)
                .as("Время ответа должно быть в прошедшем времени")
                .isBeforeOrEqualTo(LocalDateTime.now());
    }

    public static void assertHeaders(Response response, List<Header> expectedHeaders) {
        List<Header> actualHeaders = response.getHeaders().asList();

        for (Header expected : expectedHeaders) {
            assertThat(actualHeaders)
                    .as("Заголовок '%s' должен присутствовать", expected.getName())
                    .anyMatch(actual -> actual.getName().equals(expected.getName()));

            if (expected.getValue() != null && !expected.getValue().isEmpty()) {
                String actualValue = response.getHeader(expected.getName());
                assertThat(actualValue)
                        .as("Заголовок '%s' должен иметь значение '%s'",
                                expected.getName(), expected.getValue())
                        .isEqualTo(expected.getValue());
            }
        }
    }

    public static void assertHeaders(Response response) {
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

        assertHeaders(response, expectedHeaders);
    }

    public static void assertMethodNotAllowed(RequestSpecification spec, String endpoint, String wrongMethod, String... allowedMethods) {
        Response response = given()
                .spec(spec)
                .log().all()
                .when()
                .request(wrongMethod, endpoint)
                .then()
                .log().all()
                .extract().response();

        assertThat(response.getStatusCode())
                .as("Статус-код должен иметь значение 405")
                .isEqualTo(405);

        if (allowedMethods.length > 0) {
            String allowHeader = response.getHeader("Allow");
            assertThat(allowHeader).contains(allowedMethods);
        }
    }

    public static void assertMethodNotAllowed(RequestSpecification spec, String token, String endpoint, String wrongMethod, String allowedMethod) {
        Response response = given()
                .spec(spec)
                .auth().oauth2(token)
                .log().all()
                .when()
                .request(wrongMethod, endpoint)
                .then()
                .log().all()
                .extract().response();

        assertThat(response.getStatusCode())
                .as("Статус-код должен иметь значение 405")
                .isEqualTo(405);

        String allowHeader = response.getHeader("Allow");
        assertThat(allowHeader).isEqualTo(allowedMethod);
    }

    public static void assertTextError(Response response, int statusCode, String expectedMessage) {
        assertThat(response.statusCode())
                .as("Должен вернуться ответ %d", statusCode)
                .isEqualTo(statusCode);

        String message = response.body().asString();
        assertThat(message)
                .as("Сообщение должно быть %s", expectedMessage)
                .isNotBlank()
                .isEqualTo(expectedMessage);
    }
}
