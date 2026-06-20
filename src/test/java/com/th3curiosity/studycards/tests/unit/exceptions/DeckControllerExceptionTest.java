package com.th3curiosity.studycards.tests.unit.exceptions;

import com.th3curiosity.studycards.data.Endpoints;
import com.th3curiosity.studycards.data.Error;
import com.th3curiosity.studycards.dto.card.CardCreateRequest;
import com.th3curiosity.studycards.exceptions.DeckNotFoundException;
import com.th3curiosity.studycards.service.DeckService;
import com.th3curiosity.studycards.utils.ApiUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Тесты обработчика исключений работы с карточками")
public class DeckControllerExceptionTest extends BaseExceptionTest {

    @MockitoBean
    private DeckService deckService;

    @Test
    @DisplayName("Проверка обработки  ошибки DeckNotFoundException с пользовательским сообщением")
    @WithMockUser(username = "testuser")
    void handleDeckNotFound_OnlyMessage_ShouldReturn404() throws Exception {
        Mockito.doThrow(new DeckNotFoundException(Error.ResponseMessage.DECK_NOT_FOUND))
                .when(deckService).getUserDecks(any(String.class));

        mockMvc.perform(get(Endpoints.GET_ALL_DECK)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(Error.Code.DECK_NOT_FOUND))
                .andExpect(jsonPath("$.message").value(Error.ResponseMessage.DECK_NOT_FOUND));
    }

    @Test
    @DisplayName("Проверка обработки  ошибки DeckNotFoundException с пользовательским сообщением и id колоды")
    @WithMockUser(username = "testuser")
    void handleDeckNotFound_MessageWithId_ShouldReturn404() throws Exception {
        long deckId = 1111L;
        String username = "testuser";
        doThrow(new DeckNotFoundException(deckId, username))
                .when(deckService).addCardToDeck(anyString(), anyLong(), any(CardCreateRequest.class));

        CardCreateRequest cardCreateRequest = new CardCreateRequest();
        cardCreateRequest.setFront("Front text");
        cardCreateRequest.setBack("Back text");
        String body = ApiUtils.toJsonStr(cardCreateRequest);

        mockMvc.perform(post(String.format("/api/decks/%d/add-card", deckId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(Error.Code.DECK_NOT_FOUND))
                .andExpect(jsonPath("$.message")
                        .value("Deck with id " + deckId +" not found for user " + username));
    }

    @Test
    @DisplayName("Проверка обработки неожиданной ошибки RuntimeException")
    @Disabled("Не обрабатываетсяя неожиданнаяя  ошибка")
    @WithMockUser(username = "testuser")
    void handleUnexpectedError_ShouldReturn500() throws Exception {
        doThrow(new RuntimeException(Error.ResponseMessage.INTERNAL_SERVER_ERROR))
                .when(deckService).getUserDecks(anyString());

        mockMvc.perform(get(Endpoints.GET_ALL_DECK)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(Error.Code.INTERNAL_SERVER_ERROR))
                .andExpect(jsonPath("$.message").value(Error.ResponseMessage.INTERNAL_SERVER_ERROR));
    }
}
