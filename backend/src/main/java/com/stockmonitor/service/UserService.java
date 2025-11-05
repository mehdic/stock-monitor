package com.stockmonitor.service;

import com.stockmonitor.dto.LoginRequest;
import com.stockmonitor.dto.LoginResponse;
import com.stockmonitor.dto.RegisterRequest;
import com.stockmonitor.dto.UserDTO;
import com.stockmonitor.model.User;
import com.stockmonitor.repository.UserRepository;
import com.stockmonitor.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final ObjectMapper objectMapper;

  @Transactional
  public UserDTO register(RegisterRequest request) {
    log.info("Registering new user with email: {}", request.getEmail());

    // Check if email already exists
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalStateException("Email already registered");
    }

    // Create user entity
    User user =
        User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .baseCurrency(request.getBaseCurrency() != null ? request.getBaseCurrency() : "USD")
            .timezone(request.getTimezone() != null ? request.getTimezone() : "America/New_York")
            .role(User.UserRole.OWNER)
            .emailVerified(false)
            .build();

    User savedUser = userRepository.save(user);
    log.info("User registered successfully with ID: {}", savedUser.getId());

    // TODO: Send email verification
    // emailService.sendVerificationEmail(savedUser);

    return UserDTO.from(savedUser);
  }

  @Transactional
  public LoginResponse login(LoginRequest request) {
    log.info("Login attempt for email: {}", request.getEmail());

    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid email or password");
    }

    // Update last login time
    user.setLastLoginAt(LocalDateTime.now());
    userRepository.save(user);

    // Generate JWT token with user ID as principal for easier access in controllers
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.builder()
            .username(user.getId().toString()) // Use user ID as username for JWT
            .password(user.getPasswordHash())
            .roles(user.getRole().name())
            .build();

    String token = jwtService.generateToken(userDetails);

    log.info("User logged in successfully: {} (ID: {})", user.getEmail(), user.getId());

    return LoginResponse.builder()
        .token(token)
        .email(user.getEmail())
        .role(user.getRole().name())
        .build();
  }

  /**
   * Get notification preferences for user per FR-049.
   *
   * @param userId User ID
   * @return Map of category -> enabled flag
   */
  public Map<String, Boolean> getNotificationPreferences(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    String preferencesJson = user.getNotificationPreferences();

    if (preferencesJson == null || preferencesJson.isEmpty() || preferencesJson.equals("{}")) {
      // Default: all categories enabled
      return getDefaultNotificationPreferences();
    }

    try {
      return objectMapper.readValue(preferencesJson, new TypeReference<Map<String, Boolean>>() {});
    } catch (JsonProcessingException e) {
      log.error("Failed to parse notification preferences for user {}: {}", userId, e.getMessage());
      return getDefaultNotificationPreferences();
    }
  }

  /**
   * Update notification preferences for user per FR-049.
   *
   * Supports per-category opt-out:
   * - T-3_PRECOMPUTE
   * - T-1_STAGED
   * - T_FINALIZED
   * - DATA_STALE
   * - RUN_FAILED
   * - CONSTRAINT_VIOLATED
   *
   * @param userId User ID
   * @param preferences Map of category -> enabled flag
   */
  @Transactional
  public void updateNotificationPreferences(UUID userId, Map<String, Boolean> preferences) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    try {
      String preferencesJson = objectMapper.writeValueAsString(preferences);
      user.setNotificationPreferences(preferencesJson);
      userRepository.save(user);

      log.info("Updated notification preferences for user {}: {}", userId, preferencesJson);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize notification preferences for user {}: {}", userId, e.getMessage());
      throw new IllegalArgumentException("Invalid notification preferences format");
    }
  }

  /**
   * Get default notification preferences (all enabled).
   *
   * @return Default preferences map
   */
  private Map<String, Boolean> getDefaultNotificationPreferences() {
    Map<String, Boolean> defaults = new HashMap<>();
    defaults.put("T-3_PRECOMPUTE", true);
    defaults.put("T-1_STAGED", true);
    defaults.put("T_FINALIZED", true);
    defaults.put("DATA_STALE", true);
    defaults.put("RUN_FAILED", true);
    defaults.put("CONSTRAINT_VIOLATED", true);
    return defaults;
  }
}
