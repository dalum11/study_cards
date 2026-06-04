package com.th3curiosity.studycards.exceptions;

import com.th3curiosity.studycards.StudyCardsApplication;
import com.th3curiosity.studycards.config.TestContainersConfig;
import com.th3curiosity.studycards.controller.AuthController;
import com.th3curiosity.studycards.data.Endpoints;
import com.th3curiosity.studycards.data.Error;
import com.th3curiosity.studycards.dto.user.LoginRequest;
import com.th3curiosity.studycards.service.AuthService;
import com.th3curiosity.studycards.utils.ApiUtils;
import com.th3curiosity.studycards.utils.AuthUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Тесты обработчика исключений авторизации")
public class AuthControllerExceptionTest extends BaseExceptionTest {

    @MockitoBean
    private AuthService authService;

    private String convertLoginRequestToStringJson(int usernameLength, int passwordLength) {
        LoginRequest body = AuthUtils.generateLoginRequest(usernameLength, passwordLength);
        return ApiUtils.toJsonStr(body);
    }

    @Test
    @DisplayName("Проверка обработки исключения InvalidUsernameOrPasswordException")
    void handleWrongLoginDataException_ShouldReturn401() throws Exception {
        doThrow(new InvalidUsernameOrPasswordException())
                .when(authService).login(any(LoginRequest.class));

        String bodyStr = convertLoginRequestToStringJson(5, 5);

        mockMvc.perform(post(Endpoints.LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyStr))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(Error.Code.INVALID_USERNAME_OR_PASSWORD))
                .andExpect(jsonPath("$.message").value(Error.Message.INVALID_USERNAME_OR_PASSWORD));
    }

    @Test
    @Disabled("Неучтённость - не обрабатывает неожиданные исключения")
    @DisplayName("Проверка обработки неожиданного исключения (RuntimeException)")
    void handleUnexpectedException_ShouldReturn500() throws Exception {
        doThrow(new RuntimeException("Неожиданная ошибка"))
                .when(authService).login(any(LoginRequest.class));

        String bodyStr = convertLoginRequestToStringJson(5, 5);

        mockMvc.perform(post(Endpoints.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyStr))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(Error.Code.INTERNAL_SERVER_ERROR))
                .andExpect(jsonPath("$.message").value(Error.Message.INTERNAL_SERVER_ERROR));
    }
}
