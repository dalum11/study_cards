package com.th3curiosity.studycards.tests.unit.service;

import com.th3curiosity.studycards.data.AuthData;
import com.th3curiosity.studycards.dto.deck.DeckResponse;
import com.th3curiosity.studycards.entity.Deck;
import com.th3curiosity.studycards.entity.User;
import com.th3curiosity.studycards.mapper.CardMapper;
import com.th3curiosity.studycards.mapper.DeckMapper;
import com.th3curiosity.studycards.repository.CardRepository;
import com.th3curiosity.studycards.repository.DeckRepository;
import com.th3curiosity.studycards.service.DeckService;
import com.th3curiosity.studycards.service.UserService;
import com.th3curiosity.studycards.utils.DeckUtils;
import com.th3curiosity.studycards.utils.UserUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для игрового сервиса")
public class DeckServiceTest {

    private static final Logger log = LoggerFactory.getLogger(DeckServiceTest.class);

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private UserService userService;

    @Mock
    private DeckMapper deckMapper;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private DeckService deckService;

    @Nested
    @DisplayName("Тесты получения карточек пользователя")
    class GetUserDecks {

        @Nested
        @DisplayName("Тесты успешного получения карточек пользователя")
        class SuccessfullyGetUserDecks {

            @ParameterizedTest
            @ValueSource(ints = {1, 5, 10})
            @DisplayName("У пользователя несколько колод")
            void getUserDecks_UserAndSomeDecksFound_ShouldReturnDecks(int expectedDecksCount) {
                User user = UserUtils.createUser(AuthData.USERNAME, AuthData.PASSWORD, expectedDecksCount);
                List<Deck> decks = user.getDecks();
                List<DeckResponse> expectedResponse = DeckUtils.mapDecksToDeckResponse(decks);
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, decks, expectedResponse);

                mockResponse(user, decks, expectedResponse);
                log.info("Запросы успешно замоканы");

                List<DeckResponse> actualDecks = deckService.getUserDecks(user.getUsername());
                log.info("Вернулись карточки: {}", actualDecks);

                assertThat(actualDecks)
                        .as("Должно быть %d колоды карт", expectedDecksCount)
                        .isNotNull()
                        .hasSize(expectedDecksCount)
                        .usingRecursiveComparison()
                        .ignoringFields("createdAt", "updatedAt")
                        .isEqualTo(expectedResponse);

                verifyGetUserDecksResponse(user.getUsername(), user, decks);
            }
        }

        @Nested
        @DisplayName("Ошибки при получении карточек пользователя")
        class ErrorOnGetUserDecks {

            @Test
            @DisplayName("У пользователя нет ни одной колоды")
            void getUserDecks_UserWithoutDeck_ShouldReturnEmptyList() {
                User user = UserUtils.createUser(AuthData.USERNAME, AuthData.PASSWORD, 0);
                List<Deck> decks = user.getDecks();
                List<DeckResponse> expectedResponse = Collections.emptyList();
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, decks, expectedResponse);

                mockResponse(user, decks, expectedResponse);
                log.info("Запросы успешно замоканы");

                List<DeckResponse> actualDecks = deckService.getUserDecks(user.getUsername());
                log.info("Вернулись карточки: {}", actualDecks);

                assertThat(actualDecks)
                        .as("Не должно быть ни одной колоды")
                        .hasSize(0)
                        .isEqualTo(expectedResponse);

                verifyGetUserDecksResponse(AuthData.USERNAME, user, Collections.emptyList());
            }

            @Test
            @DisplayName("Пользователь не найден в системе")
            @Disabled("Баг - пользователь не найден, но другие методыы вызываютсяя")
            void getUserDecks_UserNotFound_ShouldReturnEmptyList() {
                when(userService.findByUsername(AuthData.USERNAME)).thenReturn(null);
                log.info("Запросы успешно замоканы");

                List<DeckResponse> actualDecks = deckService.getUserDecks(AuthData.USERNAME);
                log.info("Вернулись карточки: {}", actualDecks);

                assertThat(actualDecks)
                        .as("У неизвестного пользователя не должно быть колод")
                        .isEmpty();

                verify(userService).findByUsername(AuthData.USERNAME);
                verify(deckRepository, never()).findByUser(any());
                verify(deckMapper, never()).toDeckResponseList(any());
                verifyNoMoreInteractions(userService, deckRepository, deckMapper);
            }

            @Test
            @DisplayName("Имя пользователя - null")
            @Disabled("Баг - можно передать null в имени  пользователя")
            void getUerDecks_NullUsername_ThrowsException()  {
                assertThatThrownBy(() -> deckService.getUserDecks(null))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            @DisplayName("Имя пользователя - пустая строка")
            @Disabled("Баг - можно передать пустую строку в имени  пользователя")
            void getUserDecks_EmptyUsername_ThrowsException() {
                assertThatThrownBy(() -> deckService.getUserDecks(""))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }

        private void verifyGetUserDecksResponse(String username, User user, List<Deck> decks) {
            verify(userService).findByUsername(username);
            verify(deckRepository).findByUser(user);
            verify(deckMapper).toDeckResponseList(decks);
            verifyNoMoreInteractions(userService, deckRepository, deckMapper);
        }

        private void mockResponse(User user, List<Deck> decks, List<DeckResponse> expectedResponse) {
            when(userService.findByUsername(user.getUsername())).thenReturn(user);
            when(deckRepository.findByUser(user)).thenReturn(decks);
            when(deckMapper.toDeckResponseList(decks))
                    .thenReturn(expectedResponse);
        }
    }
}
