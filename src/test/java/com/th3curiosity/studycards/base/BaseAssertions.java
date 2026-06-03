package com.th3curiosity.studycards.base;

import io.restassured.http.Header;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.RequestSpecification;

import java.time.LocalDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class BaseAssertions {

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
}
