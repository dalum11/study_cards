package com.th3curiosity.studycards.tests.unit.service;

import com.th3curiosity.studycards.data.AuthData;
import com.th3curiosity.studycards.data.Error;
import com.th3curiosity.studycards.entity.User;
import com.th3curiosity.studycards.repository.UserRepository;
import com.th3curiosity.studycards.service.CustomUserDetailsService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты загрузки данных пользователя")
public class CustomUserDetailsServiceTest {

    private static final Logger log = LoggerFactory.getLogger(DeckServiceTest.class);

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private void verifyUserRepositoryInteractions(String username) {
        verify(userRepository).findByUsername(username);
        verifyNoMoreInteractions(userRepository);
    }

    @Nested
    @DisplayName("Тесты успешной загрузки данных пользователя")
    class LoadUserByUsernameSuccess {

        @Test
        @DisplayName("Пользователь существует")
        void loadUserByUsername_ExistingUser_ReturnsUser() {
            User expectedUser = UserUtils.createBaseUser(AuthData.USERNAME_3, AuthData.PASSWORD_3);

            when(userRepository.findByUsername(expectedUser.getUsername())).thenReturn(Optional.of(expectedUser));

            UserDetails actualUser = customUserDetailsService.loadUserByUsername(AuthData.USERNAME_3);

            assertThat(actualUser).isNotNull();
            assertThat(actualUser.getUsername()).isEqualTo(expectedUser.getUsername());
            assertThat(actualUser.getPassword()).isEqualTo(expectedUser.getPassword());

            assertThat(actualUser.getAuthorities())
                    .hasSize(1)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_USER");

            assertThat(actualUser.isAccountNonExpired()).isTrue();
            assertThat(actualUser.isAccountNonLocked()).isTrue();
            assertThat(actualUser.isCredentialsNonExpired()).isTrue();
            assertThat(actualUser.isEnabled()).isTrue();

            verifyUserRepositoryInteractions(actualUser.getUsername());
        }

        @Test
        @DisplayName("Проверка шифрования пароля")
        @Disabled("Баг - пароль не зашифрован")
        void loadUserByUsername_IsPasswordEncoded() {
            User expectedUser = UserUtils.createBaseUser(AuthData.USERNAME_3, AuthData.PASSWORD_3);

            when(userRepository.findByUsername(expectedUser.getUsername())).thenReturn(Optional.of(expectedUser));

            UserDetails actualUser = customUserDetailsService.loadUserByUsername(AuthData.USERNAME_3);

            assertThat(actualUser.getPassword())
                    .startsWith("{bcrypt}$2a")
                    .isNotEqualTo(AuthData.PASSWORD_3);

            verifyUserRepositoryInteractions(actualUser.getUsername());
        }
    }

    @Nested
    @DisplayName("Тесты ошибок при загрузке данных пользователя")
    class LoadUserByUsernameError {

        private void assertGettingUsernameNotFoundException(String username) {
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(username))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining(Error.ServiceMessage.USER_NOT_FOUND)
                    .hasMessageContaining(username);
        }

        @Test
        @DisplayName("Пользователь не существует")
        void loadUserByUsername_NotExistsUser_ThrowsException() {
            String username = "not_existing_username";
            assertGettingUsernameNotFoundException(username);
            verifyUserRepositoryInteractions(username);
        }

        @Test
        @DisplayName("Имя пользователя - null")
        @Disabled("Баг - не учитывается ситуация, где имя пользователя - null")
        void loadUserByUsername_NullUser_ThrowsException() {
            assertGettingUsernameNotFoundException(null);
            verify(userRepository, never()).findByUsername(any());
            verifyNoMoreInteractions(userRepository);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("Имя пользователя - пустое или с пробелами")
        void loadUserByUsername_EmptyOrBlankUsername_ThrowsException(String username) {
            assertGettingUsernameNotFoundException(username);
            verifyUserRepositoryInteractions(username);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "user@example",
                "user@@.example.com",
                "user@.com",
                "user@gmail.com.com",
                "@gmail.com",
                "user@gmail."
        })
        @DisplayName("Невалидное имя пользователя")
        void loadUserByUsername_IncorrectUsername_ThrowsException(String username) {
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
            assertGettingUsernameNotFoundException(username);
            verifyUserRepositoryInteractions(username);
        }

        @Test
        @DisplayName("Слишком длинное имя - граничное значение")
        void loadUserByUsername_TooLongUsername_ThrowsException() {
            String username = TestDataHelper.generateRandomString(300);

            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
            assertGettingUsernameNotFoundException(username);
        }
    }
}
