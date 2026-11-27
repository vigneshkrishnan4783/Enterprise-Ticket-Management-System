package com.enterprise.ticket.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketLifecyclePolicyRequest {

  @NotBlank(message = "Policy name is required")
  private String policyName;

  private Long organizationId;

  private Boolean allowCustomerReopen;

  @Min(value = 0, message = "Reopen window hours cannot be negative")
  private Integer reopenWindowHours;

  @Min(value = 0, message = "Max reopen count cannot be negative")
  private Integer maxReopenCount;

  private Boolean autoCloseEnabled;

  @Min(value = 1, message = "Auto-close after hours must be at least 1")
  private Integer autoCloseAfterHours;

  private Boolean isActive;
}
