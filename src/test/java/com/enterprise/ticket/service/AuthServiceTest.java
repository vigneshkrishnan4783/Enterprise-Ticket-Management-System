package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

  @Test
  @DisplayName("register should validate empty payload")
  void testRegisterValidation() {
    AuthService service = new AuthService(null, null, null, null, null, null, null);
    RestException ex = assertThrows(RestException.class, () -> service.register(null));
    assertEquals("Registration request payload cannot be empty", ex.getMessage());
  }

  @Test
  @DisplayName("register should validate blank username")
  void testRegisterBlankUsername() {
    AuthService service = new AuthService(null, null, null, null, null, null, null);
    RegisterRequest request = RegisterRequest.builder().username("  ").build();
    RestException ex = assertThrows(RestException.class, () -> service.register(request));
    assertEquals("Username is required and must be between 3 and 50 characters", ex.getMessage());
  }

  @Test
  @DisplayName("login should validate empty login request")
  void testLoginValidation() {
    AuthService service = new AuthService(null, null, null, null, null, null, null);
    RestException ex = assertThrows(RestException.class, () -> service.login(null));
    assertEquals("Login request payload cannot be empty", ex.getMessage());
  }
}
