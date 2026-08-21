package org.example.userservice.service;

import org.example.userservice.dto.UserRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.entity.User;
import org.example.userservice.exception.EmailAlreadyExistsException;
import org.example.userservice.exception.UserNotFoundException;
import org.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
    }

    private User userWithId(Long id, String name, String email, Integer age) throws Exception {
        User user = new User(name, email, age);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }

    @Test
    @DisplayName("createUser сохраняет нового пользователя, если email свободен")
    void createUser_savesUser_whenEmailIsFree() throws Exception {
        UserRequest request = new UserRequest("Alex", "alex@mail.com", 30);
        User saved = userWithId(1L, "Alex", "alex@mail.com", 30);

        when(userRepository.existsByEmail("alex@mail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse result = userService.createUser(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Alex");
        assertThat(result.getEmail()).isEqualTo("alex@mail.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser выбрасывает исключение, если email уже занят")
    void createUser_throwsWhenEmailAlreadyExists() {
        UserRequest request = new UserRequest("Alex", "dup@mail.com", 30);
        when(userRepository.existsByEmail("dup@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUserById возвращает пользователя, если он найден")
    void getUserById_returnsUser_whenFound() throws Exception {
        User user = userWithId(1L, "Alex", "alex@mail.com", 30);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserById(1L);

        assertThat(result.getName()).isEqualTo("Alex");
    }

    @Test
    @DisplayName("getUserById выбрасывает исключение, если пользователь не найден")
    void getUserById_throwsWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("getAllUsers возвращает список из репозитория")
    void getAllUsers_returnsListFromRepository() throws Exception {
        List<User> users = List.of(
                userWithId(1L, "Alex", "alex@mail.com", 30),
                userWithId(2L, "Petr", "petr@mail.com", 25)
        );
        when(userRepository.findAll()).thenReturn(users);

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("updateUser обновляет существующего пользователя")
    void updateUser_updatesExistingUser() throws Exception {
        User existing = userWithId(1L, "Alex", "alex@mail.com", 30);
        UserRequest request = new UserRequest("Alex Updated", "alex@mail.com", 35);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("alex@mail.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.updateUser(1L, request);

        assertThat(result.getName()).isEqualTo("Alex Updated");
        assertThat(result.getAge()).isEqualTo(35);
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("updateUser выбрасывает исключение, если пользователь не найден")
    void updateUser_throwsWhenUserNotFound() {
        UserRequest request = new UserRequest("Alex", "alex@mail.com", 30);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser выбрасывает исключение при смене email на уже занятый")
    void updateUser_throwsWhenEmailTakenByAnotherUser() throws Exception {
        User existing = userWithId(1L, "Alex", "alex@mail.com", 30);
        User other = userWithId(2L, "Petr", "taken@mail.com", 25);
        UserRequest request = new UserRequest("Alex", "taken@mail.com", 30);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("taken@mail.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteUser удаляет существующего пользователя")
    void deleteUser_deletesExistingUser() throws Exception {
        User existing = userWithId(1L, "Alex", "alex@mail.com", 30);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        userService.deleteUser(1L);

        verify(userRepository).delete(existing);
    }

    @Test
    @DisplayName("deleteUser выбрасывает исключение, если пользователь не найден")
    void deleteUser_throwsWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(any(User.class));
        verify(userRepository, never()).deleteById(anyLong());
    }
}
