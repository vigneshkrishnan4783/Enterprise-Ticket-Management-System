package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.NotificationEventType;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketComment;
import com.enterprise.ticket.model.TicketStatusHistory;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.TicketCommentRepository;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.TicketStatusHistoryRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.AddCommentRequest;
import com.enterprise.ticket.request.UpdateTicketStatusRequest;
import com.enterprise.ticket.response.TicketCommentResponse;
import com.enterprise.ticket.response.TicketResponse;
import com.enterprise.ticket.response.TicketSlaPauseIntervalResponse;
import com.enterprise.ticket.response.TicketStatusHistoryResponse;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketTimelineService {

  private final TicketRepository ticketRepository;
  private final UserRepository userRepository;
  private final TicketCommentRepository ticketCommentRepository;
  private final TicketStatusHistoryRepository statusHistoryRepository;
  private final TicketService ticketService;
  private final SlaPauseService slaPauseService;
  private final NotificationService notificationService;

  @Transactional
  public TicketCommentResponse addComment(
      String ticketNumber, AddCommentRequest request, String username) {
    if (request == null || request.getBody() == null || request.getBody().trim().isEmpty()) {
      throw RestException.badRequest("Comment body cannot be empty");
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

    enforceTicketAccess(ticket, caller, "comment on");

    boolean isInternal = Boolean.TRUE.equals(request.getIsInternal());
    if (isInternal && caller.getUserType() == UserType.EXTERNAL_CUSTOMER) {
      throw RestException.forbidden("External customers cannot create private internal notes");
    }

    TicketComment comment =
        TicketComment.builder()
            .ticket(ticket)
            .author(caller)
            .body(request.getBody().trim())
            .isInternal(isInternal)
            .build();

    if (caller.getUserType() != UserType.EXTERNAL_CUSTOMER && ticket.getFirstRespondedAt() == null) {
      ticket.setFirstRespondedAt(java.time.Instant.now());
      if (ticket.getResponseDueAt() != null && ticket.getFirstRespondedAt().isAfter(ticket.getResponseDueAt())) {
        ticket.setIsResponseBreached(true);
      }
      ticketRepository.save(ticket);
    }

    TicketComment savedComment = ticketCommentRepository.save(comment);

    if (notificationService != null && !Boolean.TRUE.equals(request.getIsInternal())) {
      if (caller.getId().equals(ticket.getReporter().getId())) {
        if (ticket.getAssignedAgent() != null) {
          notificationService.createNotification(
              ticket.getAssignedAgent(),
              ticket,
              NotificationEventType.COMMENT_ADDED,
              "New reply on ticket " + ticket.getTicketNumber(),
              "Requester posted a new reply on ticket " + ticket.getTicketNumber() + ": " + savedComment.getBody());
        }
      } else {
        notificationService.createNotification(
            ticket.getReporter(),
            ticket,
            NotificationEventType.COMMENT_ADDED,
            "New reply on ticket " + ticket.getTicketNumber(),
            "A support reply was posted on ticket " + ticket.getTicketNumber() + ": " + savedComment.getBody());
      }
    }

    return toCommentResponse(savedComment, ticket.getTicketNumber());
  }

  @Transactional(readOnly = true)
  public List<TicketCommentResponse> getComments(String ticketNumber, String username) {
    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));

    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "view comments on");

    List<TicketComment> comments;
    if (caller.getUserType() == UserType.EXTERNAL_CUSTOMER) {
      comments =
          ticketCommentRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(
              ticket.getId());
    } else {
      comments = ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());
    }

    return comments.stream()
        .map(c -> toCommentResponse(c, ticket.getTicketNumber()))
        .collect(Collectors.toList());
  }

  @Transactional
  public TicketResponse updateStatus(
      String ticketNumber, UpdateTicketStatusRequest request, String username) {
    if (request == null || request.getStatus() == null) {
      throw RestException.badRequest("Target status is required");
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

    enforceTicketAccess(ticket, caller, "update status of");

    TicketStatus previousStatus = ticket.getStatus();
    TicketStatus newStatus = request.getStatus();

    if (previousStatus == newStatus) {
      throw RestException.badRequest("Ticket is already in status " + newStatus);
    }

    if (newStatus == TicketStatus.RESOLVED
        && (request.getResolutionNotes() == null || request.getResolutionNotes().isBlank())) {
      throw RestException.badRequest(
          "Resolution details (resolutionNotes) are required before resolving a ticket");
    }

    ticket.setStatus(newStatus);
    if (newStatus == TicketStatus.RESOLVED) {
      ticket.setResolutionNotes(request.getResolutionNotes().trim());
      if (request.getResolutionCode() != null) {
        ticket.setResolutionCode(request.getResolutionCode());
      }
      ticket.setResolvedBy(caller);
    }
    if ((newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) && ticket.getResolvedAt() == null) {
      ticket.setResolvedAt(java.time.Instant.now());
      if (ticket.getResolutionDueAt() != null && ticket.getResolvedAt().isAfter(ticket.getResolutionDueAt())) {
        ticket.setIsResolutionBreached(true);
      }
    }

    if (slaPauseService != null) {
      slaPauseService.handleStatusTransition(
          ticket, previousStatus, newStatus, caller, request.getReason());
    }

    ticket = ticketRepository.save(ticket);

    TicketStatusHistory history =
        TicketStatusHistory.builder()
            .ticket(ticket)
            .previousStatus(previousStatus)
            .newStatus(newStatus)
            .changedBy(caller)
            .reason(request.getReason() != null ? request.getReason().trim() : null)
            .build();
    statusHistoryRepository.save(history);

    if (notificationService != null) {
      notificationService.createNotification(
          ticket.getReporter(),
          ticket,
          NotificationEventType.STATUS_CHANGED,
          "Ticket " + ticket.getTicketNumber() + " status updated",
          "Status changed from " + previousStatus + " to " + newStatus);
      if (ticket.getAssignedAgent() != null && !ticket.getAssignedAgent().getId().equals(caller.getId())) {
        notificationService.createNotification(
            ticket.getAssignedAgent(),
            ticket,
            NotificationEventType.STATUS_CHANGED,
            "Ticket " + ticket.getTicketNumber() + " status updated",
            "Status changed from " + previousStatus + " to " + newStatus + " by " + caller.getUsername());
      }
    }

    return ticketService.getTicketByNumber(ticketNumber, username);
  }

  @Transactional(readOnly = true)
  public List<TicketStatusHistoryResponse> getStatusHistory(String ticketNumber, String username) {
    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));

    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "view status history of");

    return statusHistoryRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
        .map(h -> toStatusHistoryResponse(h, ticket.getTicketNumber()))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<TicketSlaPauseIntervalResponse> getSlaPauseHistory(
      String ticketNumber, String username) {
    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));

    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "view SLA pause history of");

    return slaPauseService.getPauseIntervals(ticket.getId(), ticket.getTicketNumber());
  }

  private void enforceTicketAccess(Ticket ticket, User caller, String action) {
    Set<RoleType> roles =
        caller.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());

    if (roles.contains(RoleType.ROLE_ADMIN)) {
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
            "Access denied: You are not authorized to "
                + action
                + " ticket "
                + ticket.getTicketNumber());
      }
      return;
    }

    boolean isReporter = ticket.getReporter().getId().equals(caller.getId());
    boolean isAssignedAgent =
        ticket.getAssignedAgent() != null
            && ticket.getAssignedAgent().getId().equals(caller.getId());
    boolean isAssignedTeamMember =
        ticket.getAssignedTeam() != null
            && ticket.getAssignedTeam().getMembers().stream()
                .anyMatch(m -> m.getId().equals(caller.getId()));

    if (!isReporter
        && !isAssignedAgent
        && !isAssignedTeamMember
        && !roles.contains(RoleType.ROLE_AGENT)) {
      throw RestException.forbidden(
          "Access denied: You are not authorized to "
              + action
              + " ticket "
              + ticket.getTicketNumber());
    }
  }

  private TicketCommentResponse toCommentResponse(TicketComment comment, String ticketNumber) {
    String fullName =
        comment.getAuthor().getFirstName()
            + (comment.getAuthor().getLastName() != null
                ? " " + comment.getAuthor().getLastName()
                : "");

    return TicketCommentResponse.builder()
        .id(comment.getId())
        .ticketId(comment.getTicket().getId())
        .ticketNumber(ticketNumber)
        .authorId(comment.getAuthor().getId())
        .authorUsername(comment.getAuthor().getUsername())
        .authorFullName(fullName.trim())
        .body(comment.getBody())
        .isInternal(comment.getIsInternal())
        .createdAt(comment.getCreatedAt())
        .build();
  }

  private TicketStatusHistoryResponse toStatusHistoryResponse(
      TicketStatusHistory history, String ticketNumber) {
    return TicketStatusHistoryResponse.builder()
        .id(history.getId())
        .ticketId(history.getTicket().getId())
        .ticketNumber(ticketNumber)
        .previousStatus(history.getPreviousStatus())
        .newStatus(history.getNewStatus())
        .changedById(history.getChangedBy().getId())
        .changedByUsername(history.getChangedBy().getUsername())
        .reason(history.getReason())
        .createdAt(history.getCreatedAt())
        .build();
  }
}
