package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.LoginRequest;
import com.enterprise.ticket.request.RegisterRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.AuthResponse;
import com.enterprise.ticket.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller managing authentication operations (registration and login).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and JWT login")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register user", description = "Registers internal employee or external customer")
  public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
    log.info("Processing registration request for username: {}", request.getUsername());
    return ApiResponse.success("User registered successfully", authService.register(request));
  }

  @PostMapping("/login")
  @Operation(summary = "User login", description = "Authenticates user and returns JWT bearer token")
  public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
    log.info("Processing login request for username: {}", request.getUsername());
    return ApiResponse.success("Login successful", authService.login(request));
  }
}
