package com.th3curiosity.studycards.tests.integration.repository;

import com.th3curiosity.studycards.data.AuthData;
import com.th3curiosity.studycards.data.TokenData;
import com.th3curiosity.studycards.entity.RefreshToken;
import com.th3curiosity.studycards.entity.User;
import com.th3curiosity.studycards.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@DisplayName("Тесты репозитория для работы с refreshToken")
public class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User createUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        entityManager.persistAndFlush(user);
        return user;
    }

    private RefreshToken createRefreshToken(User user, String token, Date expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setRefreshToken(token);
        refreshToken.setExpiresAt(expiresAt);
        entityManager.persistAndFlush(refreshToken);
        return refreshToken;
    }

    private void assertRefreshTokenExists(boolean result, RefreshToken refreshToken, Date expiredAt, User user) {
        assertThat(result)
                .as("Метод должен вернуть true")
                .isTrue();
        assertThat(refreshToken)
                .as("Токен должен существовать")
                .isNotNull();
        assertThat(refreshToken.getRefreshToken())
                .as("Имя токена должно быть %s", TokenData.SUCCESS_REFRESH_TOKEN)
                .isEqualTo(TokenData.SUCCESS_REFRESH_TOKEN);
        assertThat(refreshToken.getExpiresAt())
                .as("Срок истечения токена должен быть {}", expiredAt)
                .isEqualTo(expiredAt);
        assertThat(refreshToken.getUser())
                .as("Токен должен принадлежать пользователю с логином %s", user.getUsername())
                .extracting(User::getUsername)
                .isEqualTo(user.getUsername());
    }

    @Nested
    @DisplayName("Тесты для метода поиска refreshToken в базе")
    class ExistsByRefreshTokenTest {

        @Nested
        @DisplayName("refreshToken существует в базе")
        class ExistsByRefreshToken {

            @Test
            @DisplayName("Существуют и пользователь, и refreshToken")
            void userExists_ShouldReturnTrue() {
                Date expiredAt = new Date(System.currentTimeMillis() + 86400000);
                User yuri = createUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                RefreshToken refreshToken = createRefreshToken(yuri, TokenData.SUCCESS_REFRESH_TOKEN, expiredAt);

                boolean result = refreshTokenRepository.existsByRefreshToken(refreshToken.getRefreshToken());

                assertRefreshTokenExists(result, refreshToken, expiredAt, yuri);
            }

            @Test
            @DisplayName("refreshToken истёк, но находится в БД")
            void tokenExpiredButExists_ShouldReturnTrue() {
                Date expiredAt = new Date(System.currentTimeMillis() - 86400000);
                User yuri = createUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                RefreshToken refreshToken = createRefreshToken(yuri, TokenData.SUCCESS_REFRESH_TOKEN, expiredAt);

                boolean result = refreshTokenRepository.existsByRefreshToken(refreshToken.getRefreshToken());

                assertRefreshTokenExists(result, refreshToken, expiredAt, yuri);
            }

            @Test
            @DisplayName("Токен принадлежит другому пользователю -> всё равно true")
            void tokenBelongsToAnotherUser_ShouldReturnTrue() {
                Date expiredAt = new Date(System.currentTimeMillis() + 86400000);
                User alice = createUser("alice@example.com", "password");
                RefreshToken aliceToken = createRefreshToken(alice, TokenData.SUCCESS_REFRESH_TOKEN, expiredAt);

                boolean result = refreshTokenRepository.existsByRefreshToken(aliceToken.getRefreshToken());

                assertThat(result)
                        .as("Токен должен быть найден в базе")
                        .isTrue();
            }
        }

        @Nested
        @DisplayName("refreshToken не найден в БД")
        class NotExistsByRefreshToken {

            @Test
            @DisplayName("В БД нет refreshToken -> false")
            void tokenNotExists_ShouldReturnFalse() {
                boolean result = refreshTokenRepository.existsByRefreshToken("non-existent-token");

                assertThat(result)
                        .as("Токена не должно быть в базе")
                        .isFalse();
            }

            @Test
            @DisplayName("refreshToken - null")
            void tokenNull_ShouldReturnFalse() {
                boolean result = refreshTokenRepository.existsByRefreshToken(null);

                assertThat(result)
                        .as("Токена не должно быть в базе")
                        .isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Тесты удаления всех токенов пользователя")
    class DeleteAllByUserTest {

        @Nested
        @DisplayName("Успешное удаление всех токенов пользователя")
        class DeleteAllByUserSuccess {

            @ParameterizedTest
            @ValueSource(ints = {1, 5, 10})
            @DisplayName("Удаление одного или нескольких токенов")
            void deleteTokensByUser_ShouldDeleteAllTokens(int tokensCount) {
                User yuri = createUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                List<RefreshToken> tokens = new ArrayList<>();
                Date expiredAt = new Date(System.currentTimeMillis() + 86400000);

                for (int i = 1; i <= tokensCount; i++) {
                    RefreshToken token = createRefreshToken(yuri, TokenData.SUCCESS_REFRESH_TOKEN, expiredAt);
                    tokens.add(token);
                }

                assertThat(refreshTokenRepository.findByUser(yuri))
                        .as("Количество токенов пользователя в базе должно быть %d", tokensCount)
                        .hasSize(tokensCount);

                refreshTokenRepository.deleteAllByUser(yuri);

                assertThat(refreshTokenRepository.findByUser(yuri))
                        .as("У пользователя не должно остаться токенов")
                        .isEmpty();
            }
        }

        @Nested
        @DisplayName("Неуспешное удаление всех токенов пользователя")
        class DeleteAllByUserError {

            @Test
            @DisplayName("У пользователя нет ни одного токена")
            void noTokens_ShouldDeleteNothing() {
                User yuri = createUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);

                assertThat(refreshTokenRepository.findByUser(yuri))
                        .as("У пользователя не должно быть токенов")
                        .isEmpty();

                refreshTokenRepository.deleteAllByUser(yuri);

                assertThat(refreshTokenRepository.findByUser(yuri))
                        .as("У пользователя не должно быть токенов")
                        .isEmpty();
            }

            @Test
            @DisplayName("Удаление токенов от другого пользователя")
            void deleteTokensByAnotherUser_ShouldDeleteNoTokens() {
                User yuri = createUser(AuthData.USERNAME_1, AuthData.PASSWORD_1);
                User victor = createUser("vicknick@gmail.com", AuthData.PASSWORD_1);
                Date expiredAt = new Date(System.currentTimeMillis() + 86400000);
                RefreshToken token = createRefreshToken(yuri, TokenData.SUCCESS_REFRESH_TOKEN, expiredAt);
                entityManager.persistAndFlush(token);

                assertThat(refreshTokenRepository.findByUser(victor))
                        .as("У пользователя %s не должно быть токенов", victor.getUsername())
                        .isEmpty();
                assertThat(refreshTokenRepository.findByUser(yuri))
                        .as("У пользователя $s должны быть токены", yuri.getUsername())
                        .isNotEmpty();

                refreshTokenRepository.deleteAllByUser(victor);

                assertThat(refreshTokenRepository.findByUser(yuri))
                        .as("У пользователя %s должен быть 1 токен", yuri.getUsername())
                        .isNotEmpty()
                        .hasSize(1);
                assertThat(refreshTokenRepository.findByUser(yuri))
                        .as("У пользователя %s должен быть токен %s", yuri.getUsername(), TokenData.SUCCESS_REFRESH_TOKEN)
                        .first()
                        .extracting(RefreshToken::getRefreshToken)
                        .isEqualTo(TokenData.SUCCESS_REFRESH_TOKEN);
            }

            @Test
            @DisplayName("Удаление токенов пользователя null не бросает исключение")
            void deleteTokensByNullUser_DoesNotThrowsException() {
                assertThatCode(() -> refreshTokenRepository.deleteAllByUser(null))
                        .as("Не должно быть никакого исключения")
                        .doesNotThrowAnyException();
            }
        }
    }
}