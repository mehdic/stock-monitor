package com.stockmonitor;

import com.stockmonitor.config.TestSecurityConfig;
import com.stockmonitor.repository.UserRepository;
import com.stockmonitor.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;

/**
 * Base class for integration tests using TestContainers for PostgreSQL.
 *
 * <p>Usage: Extend this class for any integration test that requires database access.
 *
 * <p>Features: - PostgreSQL container with automatic cleanup - Transaction rollback after each
 * test - MockMvc for testing REST endpoints - Active 'test' profile - Batch configuration excluded
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {BatchAutoConfiguration.class})
@ComponentScan(
    basePackages = "com.stockmonitor",
    excludeFilters = {
      @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.stockmonitor\\.batch\\..*"),
      @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.stockmonitor\\.scheduler\\..*")
    })
@EntityScan(basePackages = "com.stockmonitor.model")
@EnableJpaRepositories(basePackages = "com.stockmonitor.repository")
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
public abstract class BaseIntegrationTest {

  @Container
  protected static final PostgreSQLContainer<?> postgresContainer =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @Autowired protected MockMvc mockMvc;
  @Autowired protected JwtService jwtService;
  @Autowired protected UserRepository userRepository;
  @Autowired protected PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    // Security is enabled for tests - use generateTestToken() to create JWT tokens
    // Override in subclasses if needed
  }

  /**
   * Generate a valid JWT token for testing.
   *
   * @param username the username to include in the token
   * @return a valid JWT token string
   */
  protected String generateTestToken(String username) {
    // Create user in database if not exists
    if (!userRepository.existsByEmail(username)) {
      com.stockmonitor.model.User user =
          com.stockmonitor.model.User.builder()
              .email(username)
              .passwordHash(passwordEncoder.encode("password"))
              .firstName("Test")
              .lastName("User")
              .enabled(true)
              .build();
      userRepository.save(user);
    }

    UserDetails userDetails =
        User.builder().username(username).password("password").authorities(Collections.emptyList()).build();
    return jwtService.generateToken(userDetails);
  }

  /**
   * Create HTTP headers with Bearer token authentication for TestRestTemplate.
   *
   * @param username the username for token generation
   * @return HttpHeaders with Authorization header
   */
  protected org.springframework.http.HttpHeaders createAuthHeaders(String username) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.set("Authorization", "Bearer " + generateTestToken(username));
    return headers;
  }

  /**
   * Create an HttpEntity with authentication headers for TestRestTemplate.
   *
   * @param username the username for token generation
   * @return HttpEntity with auth headers
   */
  protected <T> org.springframework.http.HttpEntity<T> createAuthEntity(String username) {
    return new org.springframework.http.HttpEntity<>(createAuthHeaders(username));
  }

  /**
   * Create an HttpEntity with body and authentication headers for TestRestTemplate.
   *
   * @param body the request body
   * @param username the username for token generation
   * @return HttpEntity with body and auth headers
   */
  protected <T> org.springframework.http.HttpEntity<T> createAuthEntity(T body, String username) {
    return new org.springframework.http.HttpEntity<>(body, createAuthHeaders(username));
  }
}
