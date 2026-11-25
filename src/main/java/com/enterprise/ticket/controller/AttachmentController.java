package com.enterprise.ticket.controller;

import com.enterprise.ticket.response.AttachmentResponse;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.service.AttachmentService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/tickets/{ticketNumber}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

  private final AttachmentService attachmentService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
      @PathVariable String ticketNumber,
      @RequestParam("file") MultipartFile file,
      Principal principal) {
    AttachmentResponse response =
        attachmentService.uploadAttachment(ticketNumber, file, principal.getName());
    return ResponseBuilder.<AttachmentResponse>builder()
        .created("Attachment uploaded successfully")
        .withData(response)
        .build();
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getAttachments(
      @PathVariable String ticketNumber, Principal principal) {
    List<AttachmentResponse> responses =
        attachmentService.getTicketAttachments(ticketNumber, principal.getName());
    return ResponseBuilder.<List<AttachmentResponse>>builder()
        .success()
        .withData(responses)
        .build();
  }

  @GetMapping("/{attachmentId}/download")
  public ResponseEntity<Resource> downloadAttachment(
      @PathVariable String ticketNumber,
      @PathVariable Long attachmentId,
      Principal principal) {
    AttachmentService.DownloadAttachmentResult result =
        attachmentService.downloadAttachment(ticketNumber, attachmentId, principal.getName());

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(result.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + result.originalFileName() + "\"")
        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(result.fileSizeBytes()))
        .body(result.resource());
  }
}
