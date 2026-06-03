package com.th3curiosity.studycards.base;

import com.th3curiosity.studycards.config.TestContainersConfig;
import com.th3curiosity.studycards.data.Endpoints;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
public class BaseApiTest {

    @LocalServerPort
    protected int port;

    protected RequestSpecification requestSpecification;

    @BeforeEach
    void setupRestAssured() {
        requestSpecification = new RequestSpecBuilder()
                .setPort(port)
                .setContentType(ContentType.JSON)
                .build();
    }

    protected Response login(String username, String password) {
        Map<String, String> credentials = Map.of("username", username, "password", password);
        return given()
                .spec(requestSpecification)
                .body(credentials)
                .log().all()
            .when()
                .post(Endpoints.LOGIN)
            .then()
                .log().all()
                .extract().response();
    }

    protected Response login(String credentialsJson) {
        return given()
                .spec(requestSpecification)
                .body(credentialsJson)
                .log().all()
                .when()
                .post(Endpoints.LOGIN)
                .then()
                .log().all()
                .extract().response();
    }
}
