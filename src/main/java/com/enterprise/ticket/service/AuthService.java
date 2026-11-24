package com.enterprise.ticket.service;

import com.enterprise.ticket.config.JwtProperties;
import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.model.Role;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.RoleRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.LoginRequest;
import com.enterprise.ticket.request.RegisterRequest;
import com.enterprise.ticket.response.AuthResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user registration and authentication with business validation.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final JwtProperties jwtProperties;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    validateRegisterRequest(request);

    if (userRepository.existsByUsername(request.getUsername().trim())) {
      throw new IllegalArgumentException("Username is already taken");
    }
    if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
      throw new IllegalArgumentException("Email is already in use");
    }

    RoleType targetRoleType =
        (request.getRole() != null)
            ? request.getRole()
            : (request.getUserType() == UserType.EXTERNAL_CUSTOMER
                ? RoleType.ROLE_CUSTOMER
                : RoleType.ROLE_EMPLOYEE);

    Role role =
        roleRepository
            .findByName(targetRoleType)
            .orElseGet(
                () ->
                    roleRepository.save(
                        Role.builder()
                            .name(targetRoleType)
                            .description(targetRoleType.getDescription())
                            .build()));

    Set<Role> roles = new HashSet<>();
    roles.add(role);

    User user =
        User.builder()
            .username(request.getUsername().trim())
            .email(request.getEmail().trim().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName().trim())
            .lastName(request.getLastName().trim())
            .userType(request.getUserType())
            .organizationName(
                request.getOrganizationName() != null ? request.getOrganizationName().trim() : null)
            .isActive(true)
            .roles(roles)
            .build();

    user = userRepository.save(user);

    var userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    String token = jwtService.generateToken(userDetails);

    return toAuthResponse(user, token);
  }

  public AuthResponse login(LoginRequest request) {
    validateLoginRequest(request);

    String loginIdentifier = request.getUsername().trim();
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginIdentifier, request.getPassword()));

    User user =
        userRepository
            .findByUsername(loginIdentifier)
            .or(() -> userRepository.findByEmail(loginIdentifier.toLowerCase()))
            .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

    var userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    String token = jwtService.generateToken(userDetails);

    return toAuthResponse(user, token);
  }

  private void validateRegisterRequest(RegisterRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Registration request payload cannot be empty");
    }
    if (request.getUsername() == null || request.getUsername().trim().length() < 3 || request.getUsername().trim().length() > 50) {
      throw new IllegalArgumentException("Username is required and must be between 3 and 50 characters");
    }
    if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
      throw new IllegalArgumentException("A valid email address is required");
    }
    if (request.getPassword() == null || request.getPassword().length() < 6) {
      throw new IllegalArgumentException("Password is required and must be at least 6 characters");
    }
    if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
      throw new IllegalArgumentException("First name is required");
    }
    if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
      throw new IllegalArgumentException("Last name is required");
    }
    if (request.getUserType() == null) {
      throw new IllegalArgumentException("User type is required (INTERNAL or EXTERNAL_CUSTOMER)");
    }
  }

  private void validateLoginRequest(LoginRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Login request payload cannot be empty");
    }
    if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
      throw new IllegalArgumentException("Username or email is required");
    }
    if (request.getPassword() == null || request.getPassword().isEmpty()) {
      throw new IllegalArgumentException("Password is required");
    }
  }

  private AuthResponse toAuthResponse(User user, String token) {
    Set<String> roleNames =
        user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet());

    return AuthResponse.builder()
        .token(token)
        .tokenType(token != null ? "Bearer" : null)
        .expiresIn(token != null ? jwtProperties.getExpirationMs() : null)
        .userId(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .userType(user.getUserType())
        .organizationName(user.getOrganizationName())
        .roles(roleNames)
        .build();
  }
}
