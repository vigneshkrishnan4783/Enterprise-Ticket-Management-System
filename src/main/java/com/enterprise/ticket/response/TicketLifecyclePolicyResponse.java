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
public class TicketLifecyclePolicyResponse {

  private Long id;
  private String policyName;
  private Long organizationId;
  private String organizationName;
  private Boolean allowCustomerReopen;
  private Integer reopenWindowHours;
  private Integer maxReopenCount;
  private Boolean autoCloseEnabled;
  private Integer autoCloseAfterHours;
  private Boolean isActive;
  private Instant createdAt;
  private Instant updatedAt;
}
