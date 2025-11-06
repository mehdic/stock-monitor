package com.stockmonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmonitor.config.TestSecurityConfig;
import com.stockmonitor.helper.TestDataHelper;
import com.stockmonitor.repository.UserRepository;
import com.stockmonitor.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * Base class for integration tests using H2 in-memory database.
 *
 * <p>Usage: Extend this class for any integration test that requires database access.
 *
 * <p>Features: - H2 in-memory database configured in PostgreSQL compatibility mode - Transaction rollback after each
 * test - MockMvc for testing REST endpoints - Active 'test' profile - Batch configuration excluded
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
// @Transactional removed - contract tests need real transaction behavior
@Import({TestSecurityConfig.class, BaseIntegrationTest.TestRestTemplateConfig.class})
public abstract class BaseIntegrationTest {

  /**
   * Configure TestRestTemplate with buffering to allow request retry on authentication failures.
   * This fixes "cannot retry due to server authentication, in streaming mode" errors.
   *
   * Note: We don't set rootUri here to avoid @LocalServerPort injection issues in @TestConfiguration.
   * Tests using TestRestTemplate should inject @LocalServerPort and build absolute URLs manually,
   * or use MockMvc instead for testing endpoints.
   */
  @TestConfiguration
  static class TestRestTemplateConfig {
    @Bean
    @org.springframework.context.annotation.Primary
    public TestRestTemplate testRestTemplate(RestTemplateBuilder builder) {
      // Configure builder with buffering request factory only
      // Tests must provide absolute URLs when using this TestRestTemplate
      RestTemplateBuilder bufferingBuilder = builder
          .requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
      return new TestRestTemplate(bufferingBuilder);
    }
  }

  @Autowired protected MockMvc mockMvc;
  @Autowired protected JwtService jwtService;
  @Autowired protected UserRepository userRepository;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected TestDataHelper testDataHelper;
  @Autowired protected TestRestTemplate testRestTemplate;
  @Autowired protected ObjectMapper objectMapper;

  @org.springframework.boot.test.web.server.LocalServerPort
  protected int port;

  @BeforeEach
  void setUp() {
    // Security is enabled for tests - use generateTestToken() to create JWT tokens
    // Override in subclasses if needed
  }

  /**
   * Build absolute URL from relative path for TestRestTemplate usage.
   *
   * @param relativePath the relative path (e.g., "/api/users")
   * @return absolute URL (e.g., "http://localhost:8080/api/users")
   */
  protected String url(String relativePath) {
    return "http://localhost:" + port + relativePath;
  }

  /**
   * Generate a valid JWT token for testing.
   * Creates user in a separate transaction to ensure visibility to HTTP requests.
   *
   * @param username the username to include in the token
   * @return a valid JWT token string
   */
  protected String generateTestToken(String username) {
    // Create user in a new transaction so it's visible to HTTP requests
    testDataHelper.createTestUser(username);

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
