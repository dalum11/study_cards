package com.th3curiosity.studycards.tests.unit.service;

import com.th3curiosity.studycards.data.AuthData;
import com.th3curiosity.studycards.dto.card.CardCreateRequest;
import com.th3curiosity.studycards.dto.card.CardResponse;
import com.th3curiosity.studycards.dto.deck.DeckResponse;
import com.th3curiosity.studycards.entity.Card;
import com.th3curiosity.studycards.entity.Deck;
import com.th3curiosity.studycards.entity.User;
import com.th3curiosity.studycards.exceptions.DeckNotFoundException;
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
import java.util.Optional;

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

                verifyGetUserDecksResponse(user.getUsername(), user, Collections.emptyList());
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

    @Nested
    @DisplayName("Тесты для метода добавления карты в колоду")
    class AddCardToDeck {

        private CardCreateRequest setupCardCreateRequest(String front, String back) {
            CardCreateRequest cardCreateRequest = new CardCreateRequest();
            cardCreateRequest.setFront(front);
            cardCreateRequest.setBack(back);

            return cardCreateRequest;
        }

        private CardResponse mapCardToCardResponse(Card card) {
            CardResponse cardResponse = new CardResponse();
            cardResponse.setId(card.getId());
            cardResponse.setFront(card.getFront());
            cardResponse.setBack(card.getBack());
            return cardResponse;
        }

        @Nested
        @DisplayName("Успешное добавление карты в колоду")
        class AddCardToDeckSuccessful {

            private final int cardsCountBeforeAddCard = 1;
            private final int cardsCountAfterAddCard = 2;

            @Test
            @DisplayName("Добавление новой карты в колоду")
            void addCardToDeck_AddNewCard_ShouldReturnCardResponse() {
                int cardId = 1;
                long deckId = 1;
                CardCreateRequest cardCreateRequest = setupCardCreateRequest("front", "back");
                User user = UserUtils.createUser(AuthData.USERNAME, AuthData.PASSWORD, 1);
                Deck deck = user.getDecks().get(0);
                Card card = DeckUtils.createCard(cardId, deck);
                CardResponse expectedCardResponse = mapCardToCardResponse(card);
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, deck, expectedCardResponse);

                mockAddCardToDeckResponse(user, deck, card, cardCreateRequest, expectedCardResponse);
                log.info("Данные успешно замоканы");

                CardResponse actualCardResponse = deckService.addCardToDeck(user.getUsername(), deckId, cardCreateRequest);
                log.info("Вернулся ответ: {}", actualCardResponse);

                assertResponseSuccess(expectedCardResponse, actualCardResponse, deck, card, 1);
                verifyAddCardToDeck(user, deck, card, cardCreateRequest);
            }

            @Test
            @DisplayName("Добавление карты в колоду, если в ней уже есть другие карты)")
            void addCardToDeck_AddNewCardIfAnotherCardExists_ShouldReturnCardResponse() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME, AuthData.PASSWORD);
                Deck deck = DeckUtils.createDecksWithCards(1, cardsCountBeforeAddCard, user ).get(0);
                CardCreateRequest cardCreateRequest = setupCardCreateRequest("front", "back");
                Card newCard = DeckUtils.createCard(1, deck);
                CardResponse expectedCardResponse = mapCardToCardResponse(newCard);
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, deck, expectedCardResponse);

                mockAddCardToDeckResponse(user, deck, newCard, cardCreateRequest, expectedCardResponse);
                log.info("Данные успешно замоканы");

                CardResponse actualResponse = deckService.addCardToDeck(user.getUsername(), deck.getId(), cardCreateRequest);
                log.info("Вернулся ответ: {}", actualResponse);

                assertResponseSuccess(expectedCardResponse, actualResponse, deck, newCard, cardsCountAfterAddCard);
                verifyAddCardToDeck(user, deck, newCard, cardCreateRequest);
            }

            @Test
            @DisplayName("Добавление карты с дублирующимися данными в колоду")
            void addCardToDeck_AddCardWithSameData_ShouldAddCard() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME, AuthData.PASSWORD);
                Deck deck = DeckUtils.createDecksWithCards(1, cardsCountBeforeAddCard, user ).get(0);
                Card existingCard  = deck.getCards().get(0);
                CardCreateRequest cardCreateRequest = setupCardCreateRequest(existingCard.getFront(),
                        existingCard.getBack());
                CardResponse expectedCardResponse = mapCardToCardResponse(existingCard);
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, deck, expectedCardResponse);

                mockAddCardToDeckResponse(user, deck, existingCard, cardCreateRequest, expectedCardResponse);
                log.info("Данные успешно замоканы");

                CardResponse actualResponse = deckService.addCardToDeck(user.getUsername(), deck.getId(), cardCreateRequest);
                log.info("Вернулся ответ: {}", actualResponse);

                assertResponseSuccess(expectedCardResponse, actualResponse, deck, existingCard, cardsCountAfterAddCard);
                verifyAddCardToDeck(user, deck, existingCard, cardCreateRequest);
            }

            private void assertResponseSuccess(CardResponse expectedResponse,
                                               CardResponse actualResponse,
                                               Deck deck,
                                               Card card,
                                               int cardsCountAfterAddCard) {
                assertThat(actualResponse)
                        .as("Ответ должен  быть: {}", actualResponse)
                        .isNotNull()
                        .isEqualTo(expectedResponse);

                assertThat(deck.getCards())
                        .as("Количество  карт должно быть: {}, должна быть добавлена карта: {}",
                                cardsCountAfterAddCard,  card)
                        .isNotEmpty()
                        .hasSize(cardsCountAfterAddCard)
                        .contains(card);
            }
        }

        @Nested
        @DisplayName("Ошибки при добавлении карты")
        class ErrorAddCardToDeck {

            @Test
            @DisplayName("Пользователь не существует")
            @Disabled("Неучтённость -  нет ошибки, если пользователь не найден")
            void addCardToDeck_OwnerNotExist_ShouldThrowException() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME, AuthData.PASSWORD);
                Deck deck = DeckUtils.createDecksWithCards(1, 1, user ).get(0);
                Card card = DeckUtils.createCard(1, deck);
                CardCreateRequest  cardCreateRequest = setupCardCreateRequest(card.getFront(), card.getBack());
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, deck,
                        IllegalArgumentException.class);

                when(userService.findByUsername(user.getUsername())).thenReturn(null);
                log.info("Данные успешно замоканы");

                assertThatThrownBy(() ->
                        deckService.addCardToDeck(user.getUsername(),1L, cardCreateRequest))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(userService).findByUsername(user.getUsername());
                verify(deckRepository, never()).findByUserAndId(user, deck.getId());
                verifyNoInteractions(cardMapper, cardRepository);
            }

            @Test
            @DisplayName("Хозяин колоды - другой пользователь")
            void  addCardToDeck_AnotherDeckOwner_ShouldThrowDeckNotFoundException() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME, AuthData.PASSWORD);
                Deck deck = DeckUtils.createDecksWithCards(1, 1, user ).get(0);
                CardCreateRequest cardCreateRequest = setupCardCreateRequest("front", "back");
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, deck,
                        DeckNotFoundException.class);

                when(userService.findByUsername(user.getUsername())).thenReturn(user);
                log.info("Данные успешно замоканы");

                assertException(DeckNotFoundException.class, deck.getId(), user.getUsername(), cardCreateRequest);

                verify(userService).findByUsername(user.getUsername());
                verify(deckRepository).findByUserAndId(user, deck.getId());
                verifyNoInteractions(cardMapper, cardRepository);
            }

            @Test
            @DisplayName("Колода не существует")
            void addCardToDeck_DeckNotExist_ShouldThrowDeckNotFoundException() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME, AuthData.PASSWORD);
                long notExistDeckId = 9999L;
                CardCreateRequest cardCreateRequest = setupCardCreateRequest("front", "back");
                log.info("Данные пользователя: {}, ожидаемый ответ: {}", user,
                        DeckNotFoundException.class);

                when(userService.findByUsername(user.getUsername())).thenReturn(user);
                when(deckRepository.findByUserAndId(user, notExistDeckId)).thenReturn(Optional.empty());
                log.info("Данные успешно замоканы");

                assertException(DeckNotFoundException.class, notExistDeckId, user.getUsername(), cardCreateRequest);

                verify(userService).findByUsername(user.getUsername());
                verify(deckRepository).findByUserAndId(user, notExistDeckId);
                verifyNoInteractions(cardMapper, cardRepository);
            }

            @Test
            @DisplayName("Имя пользователя - null")
            @Disabled("Неучтённость - нет обработки невалидного имени пользователя")
            void addCardToDeck_NullUsername_ShouldThrowDeckNotFoundException() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME, AuthData.PASSWORD);
                user.setUsername(null);
                Deck deck = DeckUtils.createDecksWithCards(1, 1, user ).get(0);
                CardCreateRequest cardCreateRequest = setupCardCreateRequest("front", "back");
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, deck,
                        IllegalArgumentException.class);

                assertThatThrownBy(() -> deckService.addCardToDeck(user.getUsername(), deck.getId(), cardCreateRequest))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(userService).findByUsername(user.getUsername());
                verify(deckRepository, never()).findByUserAndId(user, deck.getId());
                verifyNoInteractions(cardMapper, cardRepository);
            }

            @ParameterizedTest
            @ValueSource(strings = {"", " ", "  "})
            @Disabled("Неучтённость - нет обработки невалидного имени пользователя")
            @DisplayName("Имя пользователя - пустая строка и пробелы")
            void addCardToDeck_EmptyOrBlankUsername_ShouldThrowDeckNotFoundException(String username) {
                User user = UserUtils.createBaseUser(AuthData.USERNAME, AuthData.PASSWORD);
                user.setUsername(username);
                Deck deck = DeckUtils.createDecksWithCards(1, 1, user ).get(0);
                CardCreateRequest cardCreateRequest = setupCardCreateRequest("front", "back");
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, deck,
                        IllegalArgumentException.class);

                assertThatThrownBy(() -> deckService.addCardToDeck(user.getUsername(), deck.getId(), cardCreateRequest))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(userService).findByUsername(user.getUsername());
                verify(deckRepository, never()).findByUserAndId(user, deck.getId());
                verifyNoInteractions(cardMapper, cardRepository);
            }

            @Test
            @DisplayName("Колода с id: null")
            void addCardToDeck_NullDeckId_ShouldThrowDeckNotFoundException() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME, AuthData.PASSWORD);
                Deck deck = DeckUtils.createDecksWithCards(1, 1, user ).get(0);
                deck.setId(null);
                CardCreateRequest cardCreateRequest = setupCardCreateRequest("front", "back");
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, deck,
                        DeckNotFoundException.class);

                when(userService.findByUsername(user.getUsername())).thenReturn(user);
                when(deckRepository.findByUserAndId(user, deck.getId())).thenReturn(Optional.empty());
                log.info("Данные успешно замоканы");

                assertException(DeckNotFoundException.class, deck.getId(), user.getUsername(), cardCreateRequest);

                verify(userService).findByUsername(user.getUsername());
                verify(deckRepository).findByUserAndId(user, deck.getId());
                verifyNoInteractions(cardMapper, cardRepository);
            }

            @ParameterizedTest
            @ValueSource(longs = {0L, -1L, -2L})
            @DisplayName("Id колоды - 0 или отрицательное число")
            void addCardToDeck_UnexpectedDeckId_ShouldThrowDeckNotFoundException(long deckId) {
                User user = UserUtils.createBaseUser(AuthData.USERNAME, AuthData.PASSWORD);
                Deck deck = DeckUtils.createDecksWithCards(1, 1, user ).get(0);
                deck.setId(deckId);
                CardCreateRequest cardCreateRequest = setupCardCreateRequest("front", "back");
                log.info("Данные пользователя: {}, данные колоды: {}, ожидаемый ответ: {}", user, deck,
                        DeckNotFoundException.class);

                when(userService.findByUsername(user.getUsername())).thenReturn(user);
                log.info("Данные успешно замоканы");

                assertException(DeckNotFoundException.class, deckId, user.getUsername(), cardCreateRequest);

                verify(userService).findByUsername(user.getUsername());
                verify(deckRepository).findByUserAndId(user, deck.getId());
                verifyNoInteractions(cardMapper, cardRepository);
            }

            void verifyNoInteractions(CardMapper cardMapper, CardRepository cardRepository) {
                verify(cardMapper, never()).toCard(any(), any());
                verify(cardRepository, never()).save(any());
                verify(cardMapper, never()).toCardResponseDTO(any());
                verifyNoMoreInteractions(userService, deckRepository, cardMapper, cardRepository);
            }

            void assertException(Class clazz, Long deckId, String username, CardCreateRequest cardCreateRequest) {
                assertThatThrownBy(() -> deckService.addCardToDeck(username, deckId, cardCreateRequest))
                        .isInstanceOf(clazz)
                        .hasMessageContaining(String.valueOf(deckId))
                        .hasMessageContaining(username);
            }
        }

        private void verifyAddCardToDeck(User user, Deck deck, Card card, CardCreateRequest cardCreateRequest)  {
            verify(userService).findByUsername(user.getUsername());
            verify(deckRepository).findByUserAndId(user, deck.getId());
            verify(cardMapper).toCard(deck, cardCreateRequest);
            verify(cardRepository).save(card);
            verify(cardMapper).toCardResponseDTO(card);
            verifyNoMoreInteractions(userService, deckRepository, cardRepository, cardMapper);
        }

        private void mockAddCardToDeckResponse(User user, Deck deck, Card card,
                                               CardCreateRequest cardCreateRequest, CardResponse expectedCardResponse) {
            when(userService.findByUsername(user.getUsername())).thenReturn(user);
            when(deckRepository.findByUserAndId(user, deck.getId())).thenReturn(Optional.of(deck));
            when(cardMapper.toCard(deck, cardCreateRequest)).thenReturn(card);
            when(cardRepository.save(card)).thenAnswer(invocationOnMock -> {
                deck.getCards().add(card);
                return card;
            });
            when(cardMapper.toCardResponseDTO(card)).thenReturn(expectedCardResponse);
        }
    }
}
