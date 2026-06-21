package com.th3curiosity.studycards.tests.unit.service;

import com.th3curiosity.studycards.data.AuthData;
import com.th3curiosity.studycards.data.Endpoints;
import com.th3curiosity.studycards.data.Error;
import com.th3curiosity.studycards.data.SuccessData;
import com.th3curiosity.studycards.dto.other.AuthResult;
import com.th3curiosity.studycards.dto.user.LoginRequest;
import com.th3curiosity.studycards.entity.User;
import com.th3curiosity.studycards.exceptions.InvalidUsernameOrPasswordException;
import com.th3curiosity.studycards.security.JwtUtils;
import com.th3curiosity.studycards.service.AuthService;
import com.th3curiosity.studycards.service.RefreshTokensService;
import com.th3curiosity.studycards.service.UserService;
import com.th3curiosity.studycards.utils.AuthUtils;
import com.th3curiosity.studycards.utils.TestDataHelper;
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
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для сервиса авторизации")
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokensService refreshTokensService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("Тесты авторизации - метод login")
    class Login {

        @Nested
        @DisplayName("Успешная авторизация пользователя")
        class LoginSuccessful {

            private static final String AUTH_SUCCESS_MESSAGE = "Login successful";

            @Test
            @DisplayName("Успешная авторизация существующего пользователя")
            void login_UserExists_ShouldReturnAuthResult() {
                LoginRequest loginRequest = AuthUtils.generateLoginRequest(10, 10);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                );

                User user = UserUtils.createBaseUser(loginRequest.getUsername(), loginRequest.getPassword());

                String accessToken = AuthUtils.generateAccessToken(loginRequest.getUsername());
                String refreshToken = AuthUtils.generateRefreshToken(loginRequest.getUsername());
                ResponseCookie responseCookie = ResponseCookie.from(
                        "refreshToken", refreshToken
                ).httpOnly(true)
                        .path(Endpoints.REFRESH)
                        .build();

                when(authenticationManager.authenticate(authentication)).thenReturn(authentication);
                when(userService.findByUsername(user.getUsername())).thenReturn(user);

                when(jwtUtils.generateAccessToken(user.getUsername())).thenReturn(accessToken);
                when(jwtUtils.generateRefreshToken(user.getUsername())).thenReturn(refreshToken);

                doNothing().when(refreshTokensService).saveRefreshToken(user, refreshToken);
                when(jwtUtils.putRefreshTokenInCookie(refreshToken)).thenReturn(responseCookie);

                AuthResult actualAuthResult = authService.login(loginRequest);

                assertThat(actualAuthResult)
                        .as("Результаты авторизации должны быть заполнены")
                        .isNotNull();

                assertThat(actualAuthResult.isSuccess())
                        .as("Должна быть успешная авторизация")
                        .isTrue();
                assertThat(actualAuthResult.getAccessToken())
                        .as("Токен доступа должен быть %s", accessToken)
                        .isEqualTo(accessToken);
                assertThat(actualAuthResult.getMessage())
                        .as("Сообщение должно содержать %s", AUTH_SUCCESS_MESSAGE)
                        .isNotBlank()
                        .contains(AUTH_SUCCESS_MESSAGE);
            }
        }

        @Nested
        @DisplayName("Авторизация пользователя  с ошибкой")
        class LoginError {

            private void assertInvalidUsernameOrPasswordException(LoginRequest loginRequest) {
                assertThatThrownBy(() -> authService.login(loginRequest))
                        .as("Должно быть исключение: " + InvalidUsernameOrPasswordException.class)
                        .isInstanceOf(InvalidUsernameOrPasswordException.class)
                        .hasMessageContaining(Error.ServiceMessage.USER_NOT_FOUND)
                        .hasMessageContaining(loginRequest.getUsername());
            }

            private void verifyUserErrorsInteractions(Authentication authentication, String username) {
                verify(authenticationManager).authenticate(authentication);
                verify(userService).findByUsername(username);
                verifyNoMoreInteractions(userService, refreshTokensService, jwtUtils);
            }

            @Test
            @DisplayName("Пользователь не существует")
            void login_UserNotExists_ShouldThrowException() {
                LoginRequest loginRequest = AuthUtils.generateLoginRequest(10, 10);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                );

                User user = UserUtils.createBaseUser(loginRequest.getUsername(), loginRequest.getPassword());

                when(authenticationManager.authenticate(authentication)).thenReturn(authentication);
                when(userService.findByUsername(user.getUsername()))
                        .thenThrow(new InvalidUsernameOrPasswordException(
                                Error.ServiceMessage.USER_NOT_FOUND + user.getUsername()
                        ));

                assertInvalidUsernameOrPasswordException(loginRequest);
                verifyUserErrorsInteractions(authentication, user.getUsername());
            }

            @Test
            @DisplayName("Имя пользователя - null")
            @Disabled("Неучтённость - нет валидации loginRequest")
            void login_UsernameIsNull_ShouldThrowException() {
                LoginRequest loginRequest = AuthUtils.createLoginRequest(null, AuthData.PASSWORD_3);

                assertInvalidUsernameOrPasswordException(loginRequest);
                verify(authenticationManager, never()).authenticate(any());
                verifyNoInteractions(userService,  refreshTokensService, jwtUtils);
            }

            @Test
            @DisplayName("Пароль пользователя - null")
            @Disabled("Неучтённость - нет валидации loginRequest")
            void login_PasswordIsNull_ShouldThrowException() {
                LoginRequest loginRequest = AuthUtils.createLoginRequest(AuthData.USERNAME_1, null);

                assertInvalidUsernameOrPasswordException(loginRequest);
                verify(authenticationManager, never()).authenticate(any());
                verifyNoInteractions(userService,  refreshTokensService, jwtUtils);
            }

            @ParameterizedTest
            @DisplayName("Имя пользователя пустое или состоит из пробелов")
            @ValueSource(strings = {"", " ", "   "})
            @Disabled("Неучтённость - нет валидации loginRequest")
            void login_EmptyOrBlankUsername_ShouldThrowException(String username) {
                LoginRequest loginRequest = AuthUtils.createLoginRequest(username, AuthData.PASSWORD_3);

                assertInvalidUsernameOrPasswordException(loginRequest);
                verify(authenticationManager, never()).authenticate(any());
                verifyNoInteractions(userService,  refreshTokensService, jwtUtils);
            }

            @ParameterizedTest
            @DisplayName("Пароль пользователя пустой или состоит из пробелов")
            @ValueSource(strings = {"", " ", "   "})
            @Disabled("Неучтённость - нет валидации loginRequest")
            void login_EmptyOrBlankPassword_ShouldThrowException(String password) {
                LoginRequest loginRequest = AuthUtils.createLoginRequest(AuthData.USERNAME_3, password);

                assertInvalidUsernameOrPasswordException(loginRequest);
                verify(authenticationManager, never()).authenticate(any());
                verifyNoInteractions(userService,  refreshTokensService, jwtUtils);
            }

            @Test
            @DisplayName("Неверный пароль")
            void login_InvalidPassword_ShouldThrowException() {
                LoginRequest loginRequest = AuthUtils.generateLoginRequest(10, 10);

                when(authenticationManager.authenticate(any())).thenThrow(new InvalidUsernameOrPasswordException(
                        Error.ServiceMessage.USER_NOT_FOUND + loginRequest.getUsername()
                ));

                assertInvalidUsernameOrPasswordException(loginRequest);
                verifyNoInteractions(userService, refreshTokensService, jwtUtils);
            }

            @Test
            @DisplayName("Неверный логин")
            void login_InvalidLogin_ShouldThrowException() {
                LoginRequest loginRequest = AuthUtils.generateLoginRequest(10, 10);

                when(authenticationManager.authenticate(any())).thenThrow(new InvalidUsernameOrPasswordException(
                        Error.ServiceMessage.USER_NOT_FOUND + loginRequest.getUsername()
                ));

                assertInvalidUsernameOrPasswordException(loginRequest);
                verifyNoInteractions(userService, refreshTokensService, jwtUtils);
            }

            @Test
            @DisplayName("loginRequest не заполнен никакими данными")
            @Disabled("Неучтённость - нет валидации loginRequest")
            void login_NullLoginRequest_ShouldThrowException() {
                LoginRequest loginRequest = new LoginRequest();

                assertInvalidUsernameOrPasswordException(loginRequest);
                verifyNoInteractions(userService, refreshTokensService, jwtUtils);
            }
        }
    }

    @Nested
    @DisplayName("Тесты для разлогина пользователя")
    class Logout {

        private void assertAuthResult(AuthResult authResult, String message, boolean isSuccessful) {
            assertThat(authResult.isSuccess())
                    .as("Должен быть успешный разлогин")
                    .isEqualTo(isSuccessful);
            assertThat(authResult.getAccessToken())
                    .as("AccessToken должен быть null")
                    .isNull();
            assertThat(authResult.getRefreshTokenCookie())
                    .as("RefreshToken должен быть null")
                    .isNull();
            assertThat(authResult.getMessage())
                    .as("Сообщение должно быть %s", message)
                    .isNotBlank().isEqualTo(message);
        }

        private void mockLogoutResponse(User user, String refreshToken) {
            when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
            when(jwtUtils.getUsernameFromToken(refreshToken)).thenReturn(user.getUsername());
            when(userService.findByUsername(user.getUsername())).thenReturn(user);
            when(refreshTokensService.isRefreshTokenInWhiteList(user,  refreshToken)).thenReturn(true);
        }

        @Nested
        @DisplayName("Успешный разлогин пользователя")
        class LogoutSuccessful {

            @Test
            @DisplayName("Пользователь успешно разлогинен на всех устройствах")
            void logout_logoutAll_ShouldLogoutSuccessful() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                String username = user.getUsername();
                String refreshToken = AuthUtils.generateRefreshToken(username);

                mockLogoutResponse(user, refreshToken);
                doNothing().when(refreshTokensService).deleteAllRefreshTokens(user);

                AuthResult actualResult = authService.logout(refreshToken, SuccessData.LogoutType.LOGOUT_ALL);

                assertAuthResult(actualResult, SuccessData.AuthMessage.SUCCESS_LOGOUT, true);
            }

            @Test
            @DisplayName("Пользователь успешно разлогинен на одном устройстве")
            void logout_logoutCurrent_ShouldLogoutSuccessful() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                String username = user.getUsername();
                String refreshToken = AuthUtils.generateRefreshToken(username);

                mockLogoutResponse(user, refreshToken);
                doNothing().when(refreshTokensService).deleteRefreshToken(user, refreshToken);

                AuthResult actualResult = authService.logout(refreshToken, SuccessData.LogoutType.LOGOUT_CURRENT);

                assertAuthResult(actualResult, SuccessData.AuthMessage.SUCCESS_LOGOUT, true);
            }

            @ParameterizedTest
            @ValueSource(ints = {1, 254, 255})
            @DisplayName("Валидное имя пользователя")
            void logout_UsernameValidLength_ShouldThrowException(int usernameLength) {
                String username = TestDataHelper.generateRandomString(usernameLength);
                User user = UserUtils.createBaseUser(username, AuthData.PASSWORD_1);
                String refreshToken = AuthUtils.generateRefreshToken(username);

                mockLogoutResponse(user, refreshToken);
                doNothing().when(refreshTokensService).deleteRefreshToken(user, refreshToken);

                AuthResult actualResult = authService.logout(refreshToken, SuccessData.LogoutType.LOGOUT_CURRENT);

                assertAuthResult(actualResult, SuccessData.AuthMessage.SUCCESS_LOGOUT, true);
            }
        }

        @Nested
        @DisplayName("Неуспешный разлогин пользователя")
        class LogoutError {

            private void verifyNoInteractionsDeleteRefreshToken(User user, String refreshToken) {
                verify(refreshTokensService, never()).deleteRefreshToken(user, refreshToken);
                verify(refreshTokensService, never()).deleteAllRefreshTokens(user);
            }

            private void verifyNoInteractionsAfterValidateToken() {
                verify(jwtUtils, never()).getUsernameFromToken(any());
                verify(userService, never()).findByUsername(anyString());
                verify(refreshTokensService, never()).isRefreshTokenInWhiteList(any(), anyString());
                verify(refreshTokensService, never()).deleteRefreshToken(any(), any());
                verify(refreshTokensService, never()).deleteAllRefreshTokens(any());
            }

            @ParameterizedTest
            @ValueSource(strings = {"alll", "currentT", "unexpected"})
            @DisplayName("Несуществующий тип разлогина")
            @Disabled("Баг - пропускает невалидные типы разлогина (неизвестные системе)")
            void logout_UnexpectedLogoutType_ShouldReturnAuthResultFalse(String logoutType) {
                User user = UserUtils.createBaseUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                String username = user.getUsername();
                String refreshToken = AuthUtils.generateRefreshToken(username);

                mockLogoutResponse(user, refreshToken);

                AuthResult actualResult = authService.logout(refreshToken, logoutType);

                assertAuthResult(actualResult, Error.ServiceMessage.INVALID_REFRESH_TOKEN, false);
                verifyNoInteractionsDeleteRefreshToken(user, refreshToken);
            }

            @ParameterizedTest
            @ValueSource(strings = {"ALL", "CURRENT", "AlL", "cURRENt", "aLl", "cUrReNt"})
            @DisplayName("Другой регистр типа разлогина")
            @Disabled("Баг - пропускает невалидные типы разлогина (другой регистр)")
            void logout_logoutTypeToDifferentCases_ShouldReturnAuthResultFalse(String logoutType) {
                User user = UserUtils.createBaseUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                String username = user.getUsername();
                String refreshToken = AuthUtils.generateRefreshToken(username);

                mockLogoutResponse(user, refreshToken);

                AuthResult actualResult = authService.logout(refreshToken, logoutType);

                assertAuthResult(actualResult, Error.ServiceMessage.INVALID_REFRESH_TOKEN, false);
                verifyNoInteractionsDeleteRefreshToken(user, refreshToken);
            }

            @ParameterizedTest
            @ValueSource(strings = {"", " ", "   "})
            @DisplayName("Пустой или из пробелов тип разлогина")
            @Disabled("Баг - пропускает невалидные типы разлогина (пустые и пробельные)")
            void logout_EmptyOrBlankLogoutTypes_ShouldReturnAuthResultFalse(String logoutType) {
                User user = UserUtils.createBaseUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                String username = user.getUsername();
                String refreshToken = AuthUtils.generateRefreshToken(username);

                mockLogoutResponse(user, refreshToken);

                AuthResult actualResult = authService.logout(refreshToken, logoutType);

                assertAuthResult(actualResult, Error.ServiceMessage.INVALID_REFRESH_TOKEN, false);
                verifyNoInteractionsDeleteRefreshToken(user, refreshToken);
            }

            @Test
            @DisplayName("Тип разлогина - null")
            void logout_NullLogoutTypes_ShouldThrowException() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                String username = user.getUsername();
                String refreshToken = AuthUtils.generateRefreshToken(username);

                mockLogoutResponse(user, refreshToken);

                assertThatThrownBy(() -> authService.logout(refreshToken, null))
                        .as("Должно быть брошено " + NullPointerException.class.getName())
                        .isInstanceOf(NullPointerException.class);
                verifyNoInteractionsDeleteRefreshToken(user, refreshToken);
            }

            @Test
            @DisplayName("Невалидный RefreshToken")
            void logout_InvalidRefreshToken_ShouldReturnAuthResultFalse() {
                String invalidToken = "invalidToken";

                when(jwtUtils.validateToken(invalidToken)).thenReturn(false);

                AuthResult actualResult = authService.logout(invalidToken, SuccessData.LogoutType.LOGOUT_ALL);

                assertAuthResult(actualResult, Error.ServiceMessage.INVALID_REFRESH_TOKEN, false);
                verifyNoInteractionsAfterValidateToken();
            }

            @ParameterizedTest
            @ValueSource(strings = {"", " ", "  "})
            @DisplayName("RefreshToken пустой или из пробелов")
            void logout_EmptyOrBlankRefreshToken_ShouldReturnAuthResultFalse(String refreshToken) {
                when(jwtUtils.validateToken(refreshToken)).thenReturn(false);

                AuthResult actualResult = authService.logout(refreshToken, SuccessData.LogoutType.LOGOUT_ALL);

                assertAuthResult(actualResult, Error.ServiceMessage.INVALID_REFRESH_TOKEN, false);
                verifyNoInteractionsAfterValidateToken();
            }

            @Test
            @DisplayName("RefreshToken - null")
            void logout_NullRefreshToken_ShouldReturnAuthResultFalse() {
                AuthResult actualResult = authService.logout(null, SuccessData.LogoutType.LOGOUT_ALL);

                assertAuthResult(actualResult, Error.ServiceMessage.INVALID_REFRESH_TOKEN, false);
                verifyNoInteractionsAfterValidateToken();
            }

            @Test
            @DisplayName("Неcуществующий пользователь")
            void logout_UsernameNotFound_ShouldReturnAuthResultFalse() {
                String refreshToken = AuthUtils.generateRefreshToken("username");

                when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
                when(jwtUtils.getUsernameFromToken(refreshToken)).thenReturn(null);
                when(refreshTokensService.isRefreshTokenInWhiteList(null, refreshToken)).thenReturn(false);

                AuthResult actualResult = authService.logout(refreshToken, SuccessData.LogoutType.LOGOUT_ALL);

                assertAuthResult(actualResult, Error.ServiceMessage.INVALID_REFRESH_TOKEN, false);
                verify(jwtUtils).getUsernameFromToken(refreshToken);
                verify(userService, never()).findByUsername(anyString());
                verify(refreshTokensService).isRefreshTokenInWhiteList(null, refreshToken);
                verify(refreshTokensService, never()).deleteRefreshToken(any(), any());
                verify(refreshTokensService, never()).deleteAllRefreshTokens(any());
            }

            @ParameterizedTest
            @ValueSource(ints = {256, 257})
            @DisplayName("Имя пользователя невадидной длины")
            @Disabled("Неучтённость - нет ограничения на длину имени пользователя (как в БД)")
             void logout_UsernameInvalidLength_ShouldThrowException(int usernameLength) {
                String username = TestDataHelper.generateRandomString(usernameLength);
                String refreshToken = AuthUtils.generateRefreshToken(username);

                when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
                assertThatThrownBy(() -> authService.logout(refreshToken, SuccessData.LogoutType.LOGOUT_ALL))
                        .as("Должно быть брошено " + IllegalArgumentException.class.getName())
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            @DisplayName("RefreshToken неактивен")
            void logout_RefreshTokenNotInWhiteList_ShouldReturnAuthResultFalse() {
                User user = UserUtils.createBaseUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                String username = user.getUsername();
                String refreshToken = AuthUtils.generateRefreshToken(username);

                when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
                when(jwtUtils.getUsernameFromToken(refreshToken)).thenReturn(username);
                when(userService.findByUsername(username)).thenReturn(user);
                when(refreshTokensService.isRefreshTokenInWhiteList(any(), any())).thenReturn(false);

                AuthResult actualResult = authService.logout(refreshToken, SuccessData.LogoutType.LOGOUT_ALL);

                assertAuthResult(actualResult, Error.ServiceMessage.INVALID_REFRESH_TOKEN, false);

                verify(refreshTokensService).isRefreshTokenInWhiteList(user, refreshToken);
                verify(refreshTokensService, never()).deleteRefreshToken(any(), any());
                verify(refreshTokensService, never()).deleteAllRefreshTokens(any());
            }
        }
    }
}
