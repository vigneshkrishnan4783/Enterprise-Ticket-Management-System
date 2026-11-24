package com.enterprise.ticket.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactResponse {
  private Long id;
  private String name;
  private String code;
  private Integer levelWeight;
  private String description;
  private Boolean isActive;
  private Instant createdAt;
}
