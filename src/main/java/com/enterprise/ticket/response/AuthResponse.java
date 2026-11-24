package com.enterprise.ticket.response;

import com.enterprise.ticket.constant.UserType;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

  private String token;
  private String tokenType;
  private Long expiresIn;
  private Long userId;
  private String username;
  private String email;
  private UserType userType;
  private String organizationName;
  private Set<String> roles;
}
