package com.th3curiosity.studycards.base;

import com.th3curiosity.studycards.config.TestContainersConfig;
import com.th3curiosity.studycards.data.Endpoints;
import com.th3curiosity.studycards.dto.user.SignupRequest;
import com.th3curiosity.studycards.utils.ApiUtils;
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

    protected Response registerUser(String username, String password) {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(username);
        signupRequest.setPassword(password);

        return given()
                .spec(requestSpecification)
                .body(ApiUtils.toJsonStr(signupRequest))
                .log().all()
                .when()
                .post(Endpoints.SIGNUP)
                .then()
                .log().all()
                .extract().response();
    }

    protected Response logout(String refreshToken, String logoutType, String endpoint) {
        return given()
                .spec(requestSpecification)
                .cookie("refreshToken", refreshToken)
                .queryParam("type", logoutType)
                .log().all()
                .when()
                .post(endpoint)
                .then()
                .contentType(ContentType.TEXT)
                .log().all()
                .extract().response();
    }

    protected String getExpiredToken(String username, String password, String endpoint) {
        return logout(username, password, endpoint).body().asString();
    }
}
