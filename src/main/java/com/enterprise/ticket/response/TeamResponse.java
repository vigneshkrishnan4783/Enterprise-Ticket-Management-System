package com.enterprise.ticket.response;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
  private Long id;
  private String name;
  private String code;
  private String description;
  private Boolean isActive;
  private List<String> memberUsernames;
  private Instant createdAt;
}
