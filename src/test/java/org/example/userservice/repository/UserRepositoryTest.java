package org.example.userservice.repository;

import org.example.userservice.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("save присваивает ID и сохраняет пользователя")
    void save_persistsUserWithId() {
        User saved = userRepository.save(new User("Alex", "alex@mail.com", 30));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByEmail находит пользователя по email")
    void findByEmail_returnsUser_whenExists() {
        userRepository.save(new User("Alex", "alex2@mail.com", 30));

        Optional<User> found = userRepository.findByEmail("alex2@mail.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alex");
    }

    @Test
    @DisplayName("existsByEmail возвращает true, если email занят")
    void existsByEmail_returnsTrue_whenTaken() {
        userRepository.save(new User("Alex", "alex3@mail.com", 30));

        assertThat(userRepository.existsByEmail("alex3@mail.com")).isTrue();
        assertThat(userRepository.existsByEmail("free@mail.com")).isFalse();
    }
}
