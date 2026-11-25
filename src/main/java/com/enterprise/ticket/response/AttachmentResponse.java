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
public class AttachmentResponse {

  private Long id;
  private String ticketNumber;
  private String fileName;
  private String originalFileName;
  private String contentType;
  private Long fileSizeBytes;
  private String checksumSha256;
  private String uploadedByUsername;
  private Instant createdAt;
}
