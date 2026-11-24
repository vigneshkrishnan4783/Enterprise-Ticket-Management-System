package com.enterprise.ticket.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateImpactRequest {
  private String name;
  private String code;
  private Integer levelWeight;
  private String description;
}
