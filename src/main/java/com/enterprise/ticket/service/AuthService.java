package com.enterprise.ticket.service;

import com.enterprise.ticket.request.LoginRequest;
import com.enterprise.ticket.request.RegisterRequest;
import com.enterprise.ticket.response.AuthResponse;

public interface AuthService {

  AuthResponse register(RegisterRequest request);

  AuthResponse login(LoginRequest request);

  AuthResponse getCurrentUser(String username);
}
