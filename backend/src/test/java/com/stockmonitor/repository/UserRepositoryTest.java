package com.stockmonitor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.model.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryTest extends BaseIntegrationTest {

  @Autowired private UserRepository userRepository;

  @Test
  void testSaveAndFindByEmail() {
    // Given
    User user =
        User.builder()
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .passwordHash("hashedpassword")
            .role(User.UserRole.OWNER)
            .emailVerified(true)
            .baseCurrency("USD")
            .timezone("America/New_York")
            .build();

    // When
    User savedUser = userRepository.save(user);
    Optional<User> foundUser = userRepository.findByEmail("test@example.com");

    // Then
    assertThat(savedUser.getId()).isNotNull();
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    assertThat(foundUser.get().getRole()).isEqualTo(User.UserRole.OWNER);
  }

  @Test
  void testExistsByEmail() {
    // Given
    User user =
        User.builder()
            .email("exists@example.com")
            .firstName("Exists")
            .lastName("User")
            .passwordHash("hashedpassword")
            .role(User.UserRole.VIEWER)
            .emailVerified(false)
            .baseCurrency("USD")
            .timezone("America/New_York")
            .build();

    userRepository.save(user);

    // When
    boolean exists = userRepository.existsByEmail("exists@example.com");
    boolean notExists = userRepository.existsByEmail("notexists@example.com");

    // Then
    assertThat(exists).isTrue();
    assertThat(notExists).isFalse();
  }

  @Test
  void testFindByEmail_NotFound() {
    // When
    Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

    // Then
    assertThat(foundUser).isEmpty();
  }
}
