package com.enterprise.ticket.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBusinessCalendarRequest {

  @NotBlank(message = "Calendar name is required")
  private String name;

  @Builder.Default
  private String timeZone = "UTC";

  @Builder.Default
  private LocalTime workStartTime = LocalTime.of(9, 0);

  @Builder.Default
  private LocalTime workEndTime = LocalTime.of(18, 0);

  @Builder.Default
  private String workDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";

  @Builder.Default
  private Boolean isDefault = false;
}
