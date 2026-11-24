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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user registration, authentication, and profile retrieval.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final JwtProperties jwtProperties;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new IllegalArgumentException("Username is already taken");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
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
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .userType(request.getUserType())
            .organizationName(request.getOrganizationName())
            .isActive(true)
            .roles(roles)
            .build();

    user = userRepository.save(user);

    var userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    String token = jwtService.generateToken(userDetails);

    return toAuthResponse(user, token);
  }

  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    User user =
        userRepository
            .findByUsername(request.getUsername())
            .or(() -> userRepository.findByEmail(request.getUsername()))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    var userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    String token = jwtService.generateToken(userDetails);

    return toAuthResponse(user, token);
  }

  @Transactional(readOnly = true)
  public AuthResponse getCurrentUser(String username) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    return toAuthResponse(user, null);
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
