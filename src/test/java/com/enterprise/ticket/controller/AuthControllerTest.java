package com.enterprise.ticket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.request.LoginRequest;
import com.enterprise.ticket.request.RegisterRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.AuthResponse;
import com.enterprise.ticket.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthControllerTest {

  @Test
  @DisplayName("register should return 201 Created and registered AuthResponse")
  void testRegisterSuccess() {
    AuthService dummyAuthService =
        new AuthService(null, null, null, null, null, null, null) {
          @Override
          public AuthResponse register(RegisterRequest request) {
            return AuthResponse.builder()
                .token("dummy-jwt-register-token")
                .username(request.getUsername())
                .userType(UserType.INTERNAL)
                .build();
          }
        };

    AuthController controller = new AuthController(dummyAuthService);
    RegisterRequest request =
        RegisterRequest.builder()
            .username("john_doe")
            .password("Secret@123")
            .email("john@example.com")
            .build();

    ResponseEntity<ApiResponse<AuthResponse>> response = controller.register(request);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isSuccess());
    assertEquals("dummy-jwt-register-token", response.getBody().getData().getToken());
    assertEquals("john_doe", response.getBody().getData().getUsername());
  }

  @Test
  @DisplayName("login should return 200 OK and authenticated AuthResponse")
  void testLoginSuccess() {
    AuthService dummyAuthService =
        new AuthService(null, null, null, null, null, null, null) {
          @Override
          public AuthResponse login(LoginRequest request) {
            return AuthResponse.builder()
                .token("dummy-jwt-login-token")
                .username(request.getUsername())
                .userType(UserType.INTERNAL)
                .build();
          }
        };

    AuthController controller = new AuthController(dummyAuthService);
    LoginRequest request =
        LoginRequest.builder().username("agent_user").password("Password@123").build();

    ResponseEntity<ApiResponse<AuthResponse>> response = controller.login(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isSuccess());
    assertEquals("dummy-jwt-login-token", response.getBody().getData().getToken());
  }
}
