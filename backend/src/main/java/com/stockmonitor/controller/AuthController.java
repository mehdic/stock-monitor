package com.stockmonitor.controller;

import com.stockmonitor.dto.LoginRequest;
import com.stockmonitor.dto.LoginResponse;
import com.stockmonitor.dto.RegisterRequest;
import com.stockmonitor.dto.UserDTO;
import com.stockmonitor.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller handling user registration and login.
 *
 * <p>Endpoints: - POST /api/auth/register - Register new user - POST /api/auth/login - Login with
 * email/password
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
    log.info("Registration request received for email: {}", request.getEmail());
    UserDTO user = userService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    log.info("Login request received for email: {}", request.getEmail());
    LoginResponse response = userService.login(request);
    return ResponseEntity.ok(response);
  }
}
