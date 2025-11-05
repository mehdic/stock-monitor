package com.stockmonitor.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmonitor.BaseIntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Contract tests for Authentication API endpoints.
 *
 * <p>Tests verify API contracts per specs/001-month-end-analyst/contracts/rest-api.yaml
 *
 * <p>T036: POST /api/auth/register T037: POST /api/auth/login
 */
class AuthContractTest extends BaseIntegrationTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  void testRegisterEndpoint_Success() throws Exception {
    // Given
    Map<String, Object> registerRequest = new HashMap<>();
    registerRequest.put("email", "newuser@example.com");
    registerRequest.put("password", "SecurePassword123!");
    registerRequest.put("firstName", "John");
    registerRequest.put("lastName", "Doe");

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.email").value("newuser@example.com"))
        .andExpect(jsonPath("$.emailVerified").value(false))
        .andExpect(jsonPath("$.role").value("OWNER"));
  }

  @Test
  void testRegisterEndpoint_InvalidEmail() throws Exception {
    // Given
    Map<String, Object> registerRequest = new HashMap<>();
    registerRequest.put("email", "invalid-email");
    registerRequest.put("password", "SecurePassword123!");

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.email").exists());
  }

  @Test
  void testRegisterEndpoint_DuplicateEmail() throws Exception {
    // Given - register first user
    Map<String, Object> registerRequest = new HashMap<>();
    registerRequest.put("email", "duplicate@example.com");
    registerRequest.put("password", "SecurePassword123!");
    registerRequest.put("firstName", "First");
    registerRequest.put("lastName", "User");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isCreated());

    // When & Then - try to register with same email
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email already registered"));
  }

  @Test
  void testRegisterEndpoint_WeakPassword() throws Exception {
    // Given
    Map<String, Object> registerRequest = new HashMap<>();
    registerRequest.put("email", "user@example.com");
    registerRequest.put("password", "weak");

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.password").exists());
  }

  @Test
  void testLoginEndpoint_Success() throws Exception {
    // Given - register user first
    Map<String, Object> registerRequest = new HashMap<>();
    registerRequest.put("email", "login@example.com");
    registerRequest.put("password", "SecurePassword123!");
    registerRequest.put("firstName", "Login");
    registerRequest.put("lastName", "Test");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isCreated());

    // When - login with correct credentials
    Map<String, Object> loginRequest = new HashMap<>();
    loginRequest.put("email", "login@example.com");
    loginRequest.put("password", "SecurePassword123!");

    // Then
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists())
        .andExpect(jsonPath("$.email").value("login@example.com"))
        .andExpect(jsonPath("$.role").value("OWNER"));
  }

  @Test
  void testLoginEndpoint_InvalidCredentials() throws Exception {
    // Given
    Map<String, Object> loginRequest = new HashMap<>();
    loginRequest.put("email", "nonexistent@example.com");
    loginRequest.put("password", "WrongPassword123!");

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void testLoginEndpoint_MissingFields() throws Exception {
    // Given
    Map<String, Object> loginRequest = new HashMap<>();
    loginRequest.put("email", "user@example.com");
    // password missing

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors").exists());
  }
}
