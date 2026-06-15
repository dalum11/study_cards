package com.th3curiosity.studycards.tests.integration.controller;

import com.th3curiosity.studycards.base.BaseApiAssertions;
import com.th3curiosity.studycards.base.BaseApiTest;
import com.th3curiosity.studycards.data.AuthData;
import com.th3curiosity.studycards.data.Endpoints;
import com.th3curiosity.studycards.data.Error;
import com.th3curiosity.studycards.dto.deck.DeckCreateRequest;
import com.th3curiosity.studycards.dto.deck.DeckResponse;
import com.th3curiosity.studycards.utils.AuthUtils;
import com.th3curiosity.studycards.utils.TestDataHelper;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Тесты контроллера для работы с карточками")
public class DeckControllerTest extends BaseApiTest {

    private static final Logger log = LoggerFactory.getLogger(DeckControllerTest.class);
    private final Map<String, String> userAccessTokens = new HashMap<>();

    private String getAccessToken(String username, String password) {
        if (!userAccessTokens.containsKey(username)) {
            Response response = login(username, password);
            userAccessTokens.put(username, response.path("accessToken"));
        }

        return userAccessTokens.get(username);
    }

    private DeckResponse createTestDeck(String token, String title, String description) {
        DeckCreateRequest request = new DeckCreateRequest();
        request.setTitle(title);
        request.setDescription(description);

        return given()
                .spec(requestSpecification)
                .auth().oauth2(token)
                .body(request)
                .log().all()
                .when()
                .post(Endpoints.ADD_DECK)
                .then()
                .log().all()
                .statusCode(200)
                .extract()
                .as(DeckResponse.class);
    }

    @Nested
    @DisplayName("Тесты получения всех колод пользователя")
    class GetAllMyDecks {

        private Response getDecksResponseSuccess(String username, String password) {
            return given()
                    .log().all()
                    .spec(requestSpecification)
                    .auth().oauth2(getAccessToken(username, password))
                    .when()
                    .get(Endpoints.GET_ALL_DECK)
                    .then()
                    .log().all()
                    .statusCode(HttpStatus.OK.value())
                    .extract().response();
        }

        private Response getDecksResponseError(String token) {
            return given()
                    .log().all()
                    .spec(requestSpecification)
                    .auth().oauth2(token)
                    .when()
                    .get(Endpoints.GET_ALL_DECK)
                    .then()
                    .log().all()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .extract().response();
        }

        @Nested
        @DisplayName("Успешное получение колод пользователя")
        class SuccessfulGetAllDecks {

            @ParameterizedTest
            @MethodSource("deckTestDataProvider")
            @DisplayName("Пользователь с разным количеством колод -> возвращает правильное количество")
            void getDecks_SomeDecks_ShouldReturnAllDecks(String username, String password, int decksCount) {
                registerUser(username, password);
                String token = getAccessToken(username, password);

                Response emptyResponse = getDecksResponseSuccess(username, password);
                List<DeckResponse> emptyDecks = emptyResponse.as(new TypeRef<>() {});
                assertThat(emptyDecks).isEmpty();

                for (int i = 1; i <= decksCount; i++) {
                    createTestDeck(token, "Deck " + i, "Description " + i);
                }

                Response response = getDecksResponseSuccess(username, password);
                BaseApiAssertions.assertHeaders(response);

                List<DeckResponse> decks = response.as(new TypeRef<>() {});

                assertThat(decks)
                        .as("Должно быть создано %d колод", decksCount)
                        .hasSize(decksCount);

                assertThat(decks)
                        .allMatch(deck -> deck.getTitle().startsWith("Deck "));
            }

            static Stream<Arguments> deckTestDataProvider() {
                return Stream.of(
                        Arguments.of("testuser1@example.com", "password1", 1),
                        Arguments.of("testuser2@example.com", "password2", 3),
                        Arguments.of("testuser3@example.com", "password3", 5),
                        Arguments.of("testuser4@example.com", "password4", 10)
                );
            }

            @Test
            @DisplayName("У пользователя нет ни одной колоды")
            void getDecks_UserHasNoDeck_ShouldReturnEmptyList() {
                Response response = getDecksResponseSuccess(AuthData.USERNAME_3, AuthData.PASSWORD_3);
                BaseApiAssertions.assertHeaders(response);

                List<DeckResponse> decks = response.as(new TypeRef<>() {});
                BaseApiAssertions.assertHeaders(response);
                assertThat(decks)
                        .as("Не должно быть ни одной колоды")
                        .isEmpty();
            }

            @Test
            @DisplayName("Получение большого количества колод")
            void getDecks_ManyDecks_ShouldReturnAllDecks() {
                String tempUsername = "temp-t.@example.com";
                String tempPassword = "temp-t.password";
                int decksCount = 10;

                registerUser(tempUsername, tempPassword);

                for (int i = 1; i <= decksCount; i++) {
                    createTestDeck(getAccessToken(tempUsername, tempPassword),"Title_" + i, "Description_" + i);
                }

                Response response = getDecksResponseSuccess(tempUsername, tempPassword);
                List<DeckResponse> decks = response.as(new TypeRef<>() {});

                BaseApiAssertions.assertHeaders(response);
                assertThat(decks)
                        .as("Должно быть %d колод", decksCount)
                        .hasSize(decksCount)
                        .extracting(DeckResponse::getTitle)
                        .contains("Title_1", "Title_10");
            }
        }

        @Nested
        @DisplayName("Тесты ошибок получения колод пользователя")
        class ErrorGettingAllDecks {

            @Test
            @DisplayName("Пользователь не авторизован")
            void getDecks_UnauthorizedUser_ShouldReturn401() {
                int statusCode = HttpStatus.UNAUTHORIZED.value();
                Response response = given()
                        .spec(requestSpecification)
                        .when()
                        .get(Endpoints.GET_ALL_DECK)
                        .then()
                        .log().all()
                        .statusCode(statusCode)
                        .extract().response();
                BaseApiAssertions.assertHeaders(response);
                BaseApiAssertions.assertErrorResponse(response, statusCode, Error.Code.UNAUTHORIZED,
                        Error.Message.UNAUTHORIZED, Endpoints.GET_ALL_DECK);
            }

            @Test
            @DisplayName("Токен авторизации пользователя истёк")
            void getDecks_TokenExpired_ShouldReturn401() {
                int statusCode = HttpStatus.UNAUTHORIZED.value();
                String expiredToken = AuthUtils.generateExpiredToken(AuthData.USERNAME_1);
                Response response = getDecksResponseError(expiredToken);

                BaseApiAssertions.assertHeaders(response);
                BaseApiAssertions.assertErrorResponse(response, statusCode, Error.Code.UNAUTHORIZED,
                        Error.Message.UNAUTHORIZED, Endpoints.GET_ALL_DECK);
            }

            @Test
            @DisplayName("Один пользователь не может посмотреть колоду другого пользователя")
            void getDecks_UserCannotSeeDecksOfAnotherUser() {
                String yuriPlisToken = getAccessToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                createTestDeck(yuriPlisToken, "Секретное название", "Секретное описание");

                Response response = getDecksResponseSuccess(AuthData.USERNAME_2, AuthData.PASSWORD_2);
                List<DeckResponse> decks = response.as(new TypeRef<>() {});

                BaseApiAssertions.assertHeaders(response);
                assertThat(decks)
                        .as("Колода другого пользователя не должна отображаться")
                        .extracting(DeckResponse::getTitle)
                        .doesNotContain("Секретное название");
            }

            @Test
            @DisplayName("Невалидный токен авторизации пользователя")
            void getDecks_InvalidToken_ShouldReturn401() {
                int statusCode = HttpStatus.UNAUTHORIZED.value();
                String invalidToken = "invalidToken";

                Response response = getDecksResponseError(invalidToken);

                BaseApiAssertions.assertHeaders(response);
                BaseApiAssertions.assertErrorResponse(response, statusCode, Error.Code.UNAUTHORIZED,
                        Error.Message.UNAUTHORIZED, Endpoints.GET_ALL_DECK);
            }
        }
    }

    @Nested
    @DisplayName("Тесты для создания новой колоды")
    class CreateNewDeck {

        private Response sendCreateDeckRequest(String accessToken, DeckCreateRequest deckCreateRequest) {
            return given()
                    .spec(requestSpecification)
                    .auth().oauth2(accessToken)
                    .body(deckCreateRequest)
                    .log().all()
                    .when()
                    .post(Endpoints.ADD_DECK)
                    .then()
                    .log().all()
                    .extract().response();
        }

        private DeckCreateRequest createDeckRequest(String title, String description) {
            DeckCreateRequest deckCreateRequest = new DeckCreateRequest();
            deckCreateRequest.setTitle(title);
            deckCreateRequest.setDescription(description);
            return deckCreateRequest;
        }

        @Nested
        @DisplayName("Тесты для успешного создания новой колоды")
        class CreateNewDeckSuccess {

            private void assertDeck(DeckResponse createdDeck, String expectedTitle, String expectedDescription) {
                assertThat(createdDeck)
                        .as("Колода должна существовать")
                        .isNotNull();

                assertThat(createdDeck.getId())
                        .as("Id колоды должен быть больше нуля")
                        .isPositive();

                assertThat(createdDeck.getTitle())
                        .as("Название колоды должно быть %s", expectedTitle)
                        .isNotBlank()
                        .isEqualTo(expectedTitle);

                assertThat(createdDeck.getDescription())
                        .as("Описание колоды должно быть %s", expectedDescription)
                        .isEqualTo(expectedDescription);

                assertThat(createdDeck.getCreatedAt())
                        .as("Время создание колоды должно быть раньше текущего времени")
                        .isBeforeOrEqualTo(LocalDateTime.now());
                assertThat(createdDeck.getUpdatedAt())
                        .as("Время обновления колоды должно быть раньше текущего времени")
                        .isBeforeOrEqualTo(LocalDateTime.now());
            }

            @Test
            @DisplayName("Успешное создание новой колоды")
            void createNewDeck_DeckAddedSuccessful() {
                String title = "Сложная колода";
                String description = "Колода для уровня миддл+";
                String accessToken = getAccessToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                DeckCreateRequest deckCreateRequest = createDeckRequest(title, description);

                Response response = sendCreateDeckRequest(accessToken, deckCreateRequest);

                BaseApiAssertions.assertHeaders(response);

                DeckResponse createdDeck = response.as(DeckResponse.class);

                assertDeck(createdDeck, title, description);
            }

            @Test
            @DisplayName("Создание колоды без описания")
            void createNewDeck_WithoutDescription_DeckAddedSuccessful() {
                String title = "Сложная колода";
                String accessToken = getAccessToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                DeckCreateRequest deckCreateRequest = createDeckRequest(title, null);

                Response response = sendCreateDeckRequest(accessToken, deckCreateRequest);

                BaseApiAssertions.assertHeaders(response);

                DeckResponse createdDeck = response.as(DeckResponse.class);

                assertDeck(createdDeck, title, null);
            }

            @Test
            @DisplayName("Добавление колоды с одинаковыми названиями и описсаниями дважды")
            void createNewDeck_AddDeckTwice_DeckAddedSuccessful() {
                String title = "Сложная колода";
                String description = "Колода для уровня миддл+";
                String accessToken = getAccessToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                DeckCreateRequest deckCreateRequest = createDeckRequest(title, description);

                Response response1 = sendCreateDeckRequest(accessToken, deckCreateRequest);
                BaseApiAssertions.assertHeaders(response1);
                DeckResponse createdDeck1 = response1.as(DeckResponse.class);
                assertDeck(createdDeck1, title, description);

                Response response2 = sendCreateDeckRequest(accessToken, deckCreateRequest);
                BaseApiAssertions.assertHeaders(response2);
                DeckResponse createdDeck2 = response2.as(DeckResponse.class);
                assertDeck(createdDeck2, title, description);

                assertThat(createdDeck1.getId())
                        .as("Id созданных колод должны отличаться")
                        .isNotEqualTo(createdDeck2.getId());
            }

            @ParameterizedTest
            @ValueSource(ints = {254, 255})
            @DisplayName("Создание колоды с названием максимальной длины")
            void createNewDeck_TitleMaxLength_ShouldReturn200(int length) {
                String title = TestDataHelper.generateRandomString(length);
                String description = "Колода для уровня миддл+";
                String accessToken = getAccessToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                DeckCreateRequest deckCreateRequest = createDeckRequest(title, description);

                Response response = sendCreateDeckRequest(accessToken, deckCreateRequest);
                DeckResponse createdDeck = response.as(DeckResponse.class);

                BaseApiAssertions.assertHeaders(response);
                assertDeck(createdDeck, title, description);
            }

            @ParameterizedTest
            @CsvSource({
                    "Название с пробелами, Описание с пробелами",
                    "Название со спецсимволами №;%:?*()_+!, Описание со спецсимволами !@#$%^&*()_+",
                    "Title in english, description in english",
                    "中文標題, 中文標題",
                    "çin dilində başlıq, çin dilində başlıq",
            })
            @DisplayName("Создание колоды сс разными названиями и описаниями")
            void createNewDeck_SpecialCharacters_Return200(String title, String description) {
                String accessToken = getAccessToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                DeckCreateRequest deckCreateRequest = createDeckRequest(title, description);

                Response response = sendCreateDeckRequest(accessToken, deckCreateRequest);
                DeckResponse createdDeck = response.as(DeckResponse.class);

                BaseApiAssertions.assertHeaders(response);
                assertDeck(createdDeck, title, description);
            }
        }

        @Nested
        @DisplayName("Тесты для проверки ошибок при создании новой колоды")
        class ErrorCreateNewDeck {

            @Test
            @DisplayName("У колоды нет названия")
            @Disabled("Баг - возвращается 401 вместо 400")
            void createNewDeck_NullTitle_ShouldReturn400() {
                String description = "Колода для уровня миддл+";
                String accessToken = getAccessToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                DeckCreateRequest deckCreateRequest = createDeckRequest(null, description);

                Response response = sendCreateDeckRequest(accessToken, deckCreateRequest);
                BaseApiAssertions.assertHeaders(response);
                BaseApiAssertions.assertErrorResponse(response, HttpStatus.BAD_REQUEST.value(), Error.Code.BAD_REQUEST,
                        Error.Message.BAD_REQUEST, Endpoints.ADD_DECK);
            }

            @Test
            @DisplayName("Запрос создания колоды не содержит тело")
            @Disabled("Баг - возвращается 401 вместо 400")
            void createNewDeck_RequestWithoutBody_ShouldReturn400() {
                String accessToken = getAccessToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);

                Response response = given()
                        .spec(requestSpecification)
                        .auth().oauth2(accessToken)
                        .log().all()
                        .when()
                        .post(Endpoints.ADD_DECK)
                        .then()
                        .log().all()
                        .extract().response();

                BaseApiAssertions.assertHeaders(response);
                BaseApiAssertions.assertErrorResponse(response, HttpStatus.BAD_REQUEST.value(), Error.Code.BAD_REQUEST,
                        Error.Message.BAD_REQUEST, Endpoints.ADD_DECK);
            }

            @ParameterizedTest
            @DisplayName("Неподдерживаемые методы запроса создания колоды")
            @ValueSource(strings = {"GET", "DELETE", "PUT", "PATCH"})
            @Disabled("Неучтённость - возвращает 401 вместо 405")
            void createNewDeck_InvalidMethod_ShouldReturn405(String wrongMethod) {
                String accessToken = getAccessToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                BaseApiAssertions.assertMethodNotAllowed(requestSpecification, accessToken, Endpoints.ADD_DECK, wrongMethod, "POST");
            }

            @Test
            @DisplayName("Попытка создать колоду без токена авторизации")
            void createNewDeck_WithoutToken_ShouldReturn401() {
                String title = "Сложная колода";
                String description = "Колода для уровня миддл+";
                DeckCreateRequest deckCreateRequest = createDeckRequest(title, description);

                Response response = sendCreateDeckRequest("", deckCreateRequest);

                BaseApiAssertions.assertHeaders(response);
                BaseApiAssertions.assertErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), Error.Code.UNAUTHORIZED,
                        Error.Message.UNAUTHORIZED, Endpoints.ADD_DECK);
            }

            @ParameterizedTest
            @DisplayName("Попытка создать колоду с токеном авторизации из пробелов")
            @ValueSource(strings = {" ", "  ", "                                       "})
            void createNewDeck_BlankToken_ShouldReturn401(String accessToken) {
                String title = "Сложная колода";
                String description = "Колода для уровня миддл+";
                DeckCreateRequest deckCreateRequest = createDeckRequest(title, description);

                Response response = sendCreateDeckRequest(accessToken, deckCreateRequest);

                BaseApiAssertions.assertHeaders(response);
                BaseApiAssertions.assertErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), Error.Code.UNAUTHORIZED,
                        Error.Message.UNAUTHORIZED, Endpoints.ADD_DECK);
            }

            @Test
            @DisplayName("Попытка создать колоду с истекшим токеном")
            void createNewDeck_TokenExpired_ShouldReturn401() {
                String expiredToken = AuthUtils.generateExpiredToken(AuthData.USERNAME_1);
                String title = "Сложная колода";
                String description = "Колода для уровня миддл+";
                DeckCreateRequest deckCreateRequest = createDeckRequest(title, description);

                Response response = sendCreateDeckRequest(expiredToken, deckCreateRequest);
                BaseApiAssertions.assertHeaders(response);
                BaseApiAssertions.assertErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), Error.Code.UNAUTHORIZED,
                        Error.Message.UNAUTHORIZED, Endpoints.ADD_DECK);
            }

            @Test
            @DisplayName("Попытка создать колоду с невалидным токеном")
            void createNewDeck_InvalidToken_ShouldReturn401() {
                String invalidToken = "invalidToken";
                String title = "Сложная колода";
                String description = "Колода для уровня миддл+";
                DeckCreateRequest deckCreateRequest = createDeckRequest(title, description);

                Response response = sendCreateDeckRequest(invalidToken, deckCreateRequest);
                BaseApiAssertions.assertHeaders(response);
                BaseApiAssertions.assertErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), Error.Code.UNAUTHORIZED,
                        Error.Message.UNAUTHORIZED, Endpoints.ADD_DECK);
            }

            @ParameterizedTest
            @DisplayName("Попытка создать колоду с названием, превышающим ограничение в БД")
            @ValueSource(ints = {256, 257})
            @Disabled("Неучтённость - возвращает 401 вместо 400")
            void createNewDeck_TitleMaxLength_ShouldReturn400(int length) {
                String title = TestDataHelper.generateRandomString(length);
                String description = "Колода для уровня миддл+";
                String accessToken = getAccessToken(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                DeckCreateRequest deckCreateRequest = createDeckRequest(title, description);

                Response response = sendCreateDeckRequest(accessToken, deckCreateRequest);
                BaseApiAssertions.assertHeaders(response);
                BaseApiAssertions.assertErrorResponse(response, HttpStatus.BAD_REQUEST.value(), Error.Code.BAD_REQUEST,
                        Error.Message.BAD_REQUEST, Endpoints.ADD_DECK);
            }
        }
    }
}
