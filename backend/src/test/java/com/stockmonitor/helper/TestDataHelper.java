package com.stockmonitor.helper;

import com.stockmonitor.model.User;
import com.stockmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper class for creating test data in separate transactions.
 * This ensures test data is committed and visible to HTTP requests in integration tests.
 */
@Component
@RequiredArgsConstructor
public class TestDataHelper {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Create or get a test user in a new transaction.
   * This ensures the user is committed and visible to HTTP requests.
   *
   * @param email the user email
   * @return the created or existing user
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public User createTestUser(String email) {
    return userRepository.findByEmail(email)
        .orElseGet(() -> {
          User user = User.builder()
              .email(email)
              .passwordHash(passwordEncoder.encode("password"))
              .firstName("Test")
              .lastName("User")
              .enabled(true)
              .role(User.UserRole.OWNER)
              .build();
          return userRepository.save(user);
        });
  }
}
