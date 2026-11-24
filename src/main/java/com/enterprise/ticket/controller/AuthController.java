package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.LoginRequest;
import com.enterprise.ticket.request.RegisterRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.AuthResponse;
import com.enterprise.ticket.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and JWT login")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @Operation(summary = "Register user", description = "Registers internal employee or external customer")
  public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("User registered successfully", response));
  }

  @PostMapping("/login")
  @Operation(summary = "User login", description = "Authenticates user and returns JWT bearer token")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success("Login successful", response));
  }
}
