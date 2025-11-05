package com.stockmonitor.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.stockmonitor.BaseUnitTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest extends BaseUnitTest {

  @InjectMocks private JwtService jwtService;

  @Mock private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    // Set test values using reflection
    ReflectionTestUtils.setField(
        jwtService, "secretKey", "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tdGhpcy1pcy1vbmx5LWZvci10ZXN0aW5n");
    ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);

    // Mock UserDetails
    when(userDetails.getUsername()).thenReturn("test@example.com");
  }

  @Test
  void testGenerateToken() {
    // When
    String token = jwtService.generateToken(userDetails);

    // Then
    assertThat(token).isNotNull();
    assertThat(token).isNotEmpty();
  }

  @Test
  void testExtractUsername() {
    // Given
    String token = jwtService.generateToken(userDetails);

    // When
    String username = jwtService.extractUsername(token);

    // Then
    assertThat(username).isEqualTo("test@example.com");
  }

  @Test
  void testIsTokenValid() {
    // Given
    String token = jwtService.generateToken(userDetails);

    // When
    boolean isValid = jwtService.isTokenValid(token, userDetails);

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  void testIsTokenValid_InvalidUser() {
    // Given
    String token = jwtService.generateToken(userDetails);
    UserDetails differentUser =
        User.withUsername("different@example.com").password("password").roles("USER").build();

    // When
    boolean isValid = jwtService.isTokenValid(token, differentUser);

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  void testGenerateTokenWithExtraClaims() {
    // Given
    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("role", "OWNER");
    extraClaims.put("userId", "123");

    // When
    String token = jwtService.generateToken(extraClaims, userDetails);

    // Then
    assertThat(token).isNotNull();
    assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
  }
}
