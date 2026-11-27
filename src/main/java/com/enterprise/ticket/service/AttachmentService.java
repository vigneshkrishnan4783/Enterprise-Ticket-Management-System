package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketAttachment;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.TicketAttachmentRepository;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.response.AttachmentResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AttachmentService {

  private static final long MAX_FILE_SIZE = 25 * 1024 * 1024L;
  private static final Set<String> ALLOWED_EXTENSIONS =
      Set.of("pdf", "png", "jpg", "jpeg", "doc", "docx", "xls", "xlsx", "txt", "log", "zip");
  private static final Path STORAGE_ROOT = Paths.get("uploads", "attachments").toAbsolutePath().normalize();

  private final TicketRepository ticketRepository;
  private final UserRepository userRepository;
  private final TicketAttachmentRepository attachmentRepository;

  @Transactional
  public AttachmentResponse uploadAttachment(String ticketNumber, MultipartFile file, String username) {
    if (file == null || file.isEmpty()) {
      throw RestException.badRequest("File cannot be empty");
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw RestException.badRequest("File exceeds maximum allowed size of 25MB");
    }

    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));

    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "upload attachment to");

    String originalFileName = sanitizeFileName(file.getOriginalFilename());
    String extension = getFileExtension(originalFileName);

    if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
      throw RestException.badRequest(
          "Disallowed file extension '." + extension + "'. Allowed: " + ALLOWED_EXTENSIONS);
    }

    String storedFileName = UUID.randomUUID() + "_" + originalFileName;
    Path targetDir = STORAGE_ROOT.resolve(ticket.getTicketNumber());
    Path targetFilePath = targetDir.resolve(storedFileName).normalize();

    if (!targetFilePath.startsWith(STORAGE_ROOT)) {
      throw RestException.badRequest("Invalid file path traversal attempt");
    }

    try {
      Files.createDirectories(targetDir);
      String checksum;
      try (InputStream is = file.getInputStream()) {
        checksum = calculateSha256(is);
      }
      try (InputStream is = file.getInputStream()) {
        Files.copy(is, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
      }

      String contentType = file.getContentType();
      if (contentType == null || contentType.isBlank()) {
        contentType = "application/octet-stream";
      }

      TicketAttachment attachment =
          TicketAttachment.builder()
              .ticket(ticket)
              .fileName(storedFileName)
              .originalFileName(originalFileName)
              .storagePath(targetFilePath.toString())
              .contentType(contentType)
              .fileSizeBytes(file.getSize())
              .checksumSha256(checksum)
              .uploadedBy(caller)
              .build();

      TicketAttachment saved = attachmentRepository.save(attachment);
      return toAttachmentResponse(saved);
    } catch (IOException | NoSuchAlgorithmException e) {
      throw RestException.badRequest("Failed to store attachment securely: " + e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public List<AttachmentResponse> getTicketAttachments(String ticketNumber, String username) {
    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));

    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "view attachments for");

    return attachmentRepository.findByTicketIdOrderByCreatedAtDesc(ticket.getId()).stream()
        .map(this::toAttachmentResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public DownloadAttachmentResult downloadAttachment(
      String ticketNumber, Long attachmentId, String username) {
    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));

    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "download attachment from");

    TicketAttachment attachment =
        attachmentRepository
            .findById(attachmentId)
            .orElseThrow(
                () -> RestException.notFound("Attachment not found with id: " + attachmentId));

    if (!attachment.getTicket().getId().equals(ticket.getId())) {
      throw RestException.badRequest("Attachment does not belong to ticket " + ticketNumber);
    }

    try {
      Path filePath = Paths.get(attachment.getStoragePath()).normalize();
      if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
        throw RestException.notFound("Attachment file not found on disk");
      }

      Resource resource = new UrlResource(filePath.toUri());
      return new DownloadAttachmentResult(resource, attachment.getOriginalFileName(), attachment.getContentType(), attachment.getFileSizeBytes());
    } catch (Exception e) {
      throw RestException.badRequest("Could not read attachment file: " + e.getMessage());
    }
  }

  private void enforceTicketAccess(Ticket ticket, User caller, String action) {
    boolean isAdmin =
        caller.getRoles() != null
            && caller.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN);
    if (isAdmin) {
      return;
    }

    if (caller.getUserType() == UserType.EXTERNAL_CUSTOMER) {
      boolean isReporter = ticket.getReporter().getId().equals(caller.getId());
      boolean isSameOrg =
          ticket.getOrganization() != null
              && (ticket.getOrganization().getName().equalsIgnoreCase(caller.getOrganizationName())
                  || (caller.getOrganizations() != null
                      && caller.getOrganizations().stream()
                          .anyMatch(o -> o.getId().equals(ticket.getOrganization().getId()))));

      if (!isReporter && !isSameOrg) {
        throw RestException.forbidden(
            "Access denied: You are not authorized to " + action + " ticket " + ticket.getTicketNumber());
      }
      return;
    }

    boolean isReporter = ticket.getReporter().getId().equals(caller.getId());
    boolean isAssignedAgent =
        ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(caller.getId());
    boolean isAssignedTeamMember =
        ticket.getAssignedTeam() != null
            && ticket.getAssignedTeam().getMembers().stream()
                .anyMatch(m -> m.getId().equals(caller.getId()));
    boolean isAgent =
        caller.getRoles() != null
            && caller.getRoles().stream().anyMatch(r -> r.getName() == RoleType.ROLE_AGENT);

    if (!isReporter && !isAssignedAgent && !isAssignedTeamMember && !isAgent) {
      throw RestException.forbidden(
          "Access denied: You are not authorized to " + action + " ticket " + ticket.getTicketNumber());
    }
  }

  private String sanitizeFileName(String name) {
    if (name == null || name.isBlank()) {
      return "attachment_" + System.currentTimeMillis();
    }
    return Paths.get(name).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  private String getFileExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
      return "";
    }
    return fileName.substring(dotIndex + 1);
  }

  private String calculateSha256(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      digest.update(buffer, 0, bytesRead);
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private AttachmentResponse toAttachmentResponse(TicketAttachment attachment) {
    return AttachmentResponse.builder()
        .id(attachment.getId())
        .ticketNumber(attachment.getTicket().getTicketNumber())
        .fileName(attachment.getFileName())
        .originalFileName(attachment.getOriginalFileName())
        .contentType(attachment.getContentType())
        .fileSizeBytes(attachment.getFileSizeBytes())
        .checksumSha256(attachment.getChecksumSha256())
        .uploadedByUsername(attachment.getUploadedBy().getUsername())
        .createdAt(attachment.getCreatedAt())
        .build();
  }

  public record DownloadAttachmentResult(
      Resource resource,
      String originalFileName,
      String contentType,
      Long fileSizeBytes
  ) {}
}
