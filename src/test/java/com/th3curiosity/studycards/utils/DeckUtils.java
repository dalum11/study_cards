package com.th3curiosity.studycards.utils;

import com.th3curiosity.studycards.dto.deck.DeckResponse;
import com.th3curiosity.studycards.entity.Card;
import com.th3curiosity.studycards.entity.Deck;
import com.th3curiosity.studycards.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Утилитный класс для создания тестовых данных колод и карточек.
 * Используется только в тестах
 */
public final class DeckUtils {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final String DECK_TITLE_PREFIX = "Колода_";
    private static final String DECK_DESCRIPTION_PREFIX = "Описание_";
    private static final String CARD_FRONT_PREFIX = "Front text: ";
    private static final String CARD_BACK_PREFIX = "Back text: ";

    private DeckUtils() {
        throw new UnsupportedOperationException("Утилитный класс, создать объект нельзя");
    }

    /**
     * Метод создаёт список колод конкретного пользователя.
     *
     * @param count количество колод
     * @param user пользователь - владелец колод
     * @return список колод
     */
    public static List<Deck> createDecks(int count, User user) {
        if (count <= 0) { return Collections.emptyList(); }

        List<Deck> decks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            decks.add(createDeck(i + 1, user));
        }
        return decks;
    }

    /**
     * Метод создаёт список колод с картами конкретного пользователя.
     *
     * @param deckCount количество колод
     * @param cardsCount количество карт
     * @param user пользователь - владелец колод
     * @return список колод
     */
    public static List<Deck> createDecksWithCards(int deckCount, int cardsCount, User user) {
        if (deckCount <= 0) { return Collections.emptyList(); }

        List<Deck> decks = new ArrayList<>();
        for (int i = 0; i < deckCount; i++) {
            Deck deck = createDeck(i + 1, user);
            deck.setCards(createCards(cardsCount, deck));
            decks.add(deck);
        }

        return decks;
    }

    private static Deck createDeck(int index, User user) {
        Deck deck = new Deck();
        deck.setId((long )index);
        deck.setUser(user);
        deck.setTitle(DECK_TITLE_PREFIX + index);
        deck.setDescription(DECK_DESCRIPTION_PREFIX + index);
        deck.setCreatedAt(FIXED_TIME);
        deck.setUpdatedAt(FIXED_TIME);
        deck.setCards(new ArrayList<>());
        return deck;
    }

    /**
     * Метод маппит сущность колоды в ДТО для ответа со списком колод
     *
     * @param decks список колод
     * @return список ДТО колод
     */
    public static List<DeckResponse> mapDecksToDeckResponse(List<Deck> decks) {
        if (decks == null || decks.isEmpty()) { return Collections.emptyList(); }

        return decks.stream().map(deck -> {
            DeckResponse deckResponse = new DeckResponse();
            deckResponse.setId(deck.getId());
            deckResponse.setTitle(deck.getTitle());
            deckResponse.setDescription(deck.getDescription());
            deckResponse.setCreatedAt(deck.getCreatedAt());
            deckResponse.setUpdatedAt(deck.getUpdatedAt());
            return deckResponse;
        }).toList();
    }

    /**
     * Метод создаёт список ДТО для ответа со списком колод
     *
     * @param count количество колод
     * @return список ДТО колод
     */
    public static List<DeckResponse> createDeckResponses(int count) {
        if (count <= 0) { return Collections.emptyList();}

        List<DeckResponse> deckResponses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            deckResponses.add(createDeckResponse(i + 1));
        }
        return deckResponses;
    }

    private static DeckResponse createDeckResponse(int index) {
        DeckResponse deckResponse = new DeckResponse();
        deckResponse.setId((long) index);
        deckResponse.setTitle(DECK_TITLE_PREFIX + index);
        deckResponse.setDescription(DECK_DESCRIPTION_PREFIX + index);
        deckResponse.setCreatedAt(FIXED_TIME);
        deckResponse.setUpdatedAt(FIXED_TIME);
        return deckResponse;
    }

    /**
     * Метод создаёт список карточек
     *
     * @param count количество карточек
     * @param deck колода, для которой создаются карточки
     * @return список ДТО колод
     */
    public static List<Card> createCards(int count, Deck deck) {
        if (count <= 0) { return Collections.emptyList();}

        List<Card> cards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            cards.add(createCard(i + 1, deck));
        }

        return cards;
    }

    /**
     * Метод создаёт одну карточку
     *
     * @param index - местоположение в колоде
     * @param deck - колода, которой принадлежит карточка
     * @return карточку
     */

    public static Card createCard(int index, Deck deck) {
        Card card = new Card();
        card.setDeck(deck);
        card.setFront(CARD_FRONT_PREFIX + index);
        card.setBack(CARD_BACK_PREFIX + index);
        return card;
    }
}
