package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.NotificationEventType;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketSatisfactionFeedback;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.TicketSatisfactionFeedbackRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.SubmitFeedbackRequest;
import com.enterprise.ticket.response.FeedbackResponse;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketFeedbackService {

  private final TicketSatisfactionFeedbackRepository feedbackRepository;
  private final TicketRepository ticketRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  @Autowired
  public TicketFeedbackService(
      TicketSatisfactionFeedbackRepository feedbackRepository,
      TicketRepository ticketRepository,
      UserRepository userRepository,
      @Autowired(required = false) NotificationService notificationService) {
    this.feedbackRepository = feedbackRepository;
    this.ticketRepository = ticketRepository;
    this.userRepository = userRepository;
    this.notificationService = notificationService;
  }

  @Transactional
  public FeedbackResponse submitFeedback(
      String ticketNumber, SubmitFeedbackRequest request, String username) {
    if (request == null || request.getRating() == null) {
      throw RestException.badRequest("Rating is required");
    }
    if (request.getRating() < 1 || request.getRating() > 5) {
      throw RestException.badRequest("Rating must be between 1 and 5");
    }

    User caller = findUser(username);
    Ticket ticket = findTicket(ticketNumber);

    if (ticket.getStatus() != TicketStatus.RESOLVED
        && ticket.getStatus() != TicketStatus.CLOSED) {
      throw RestException.badRequest(
          "Feedback can only be submitted after ticket resolution or closure");
    }

    if (!isRequesterOrSameOrg(ticket, caller)) {
      throw RestException.forbidden(
          "Only the ticket requester can submit satisfaction feedback for ticket "
              + ticket.getTicketNumber());
    }

    if (feedbackRepository.existsByTicketId(ticket.getId())) {
      throw RestException.conflict(
          "Feedback has already been submitted for ticket " + ticket.getTicketNumber());
    }

    String normalizedComment =
        request.getFeedbackComment() != null && !request.getFeedbackComment().isBlank()
            ? request.getFeedbackComment().trim()
            : null;

    TicketSatisfactionFeedback feedback =
        TicketSatisfactionFeedback.builder()
            .ticket(ticket)
            .submittedBy(caller)
            .rating(request.getRating())
            .feedbackComment(normalizedComment)
            .submittedAt(Instant.now())
            .build();

    TicketSatisfactionFeedback saved = feedbackRepository.save(feedback);

    if (notificationService != null && ticket.getAssignedAgent() != null) {
      notificationService.createNotification(
          ticket.getAssignedAgent(),
          ticket,
          NotificationEventType.COMMENT_ADDED,
          "Customer feedback received for " + ticket.getTicketNumber(),
          "Rating: " + saved.getRating() + "/5");
    }

    return toResponse(saved, ticket.getTicketNumber(), caller.getUsername());
  }

  @Transactional(readOnly = true)
  public FeedbackResponse getFeedback(String ticketNumber, String username) {
    User caller = findUser(username);
    Ticket ticket = findTicket(ticketNumber);

    enforceReadAccess(ticket, caller);

    TicketSatisfactionFeedback feedback =
        feedbackRepository
            .findByTicketId(ticket.getId())
            .orElseThrow(
                () ->
                    RestException.notFound(
                        "Satisfaction feedback not found for ticket " + ticket.getTicketNumber()));

    return toResponse(
        feedback,
        ticket.getTicketNumber(),
        feedback.getSubmittedBy() != null ? feedback.getSubmittedBy().getUsername() : null);
  }

  private User findUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> RestException.notFound("User not found: " + username));
  }

  private Ticket findTicket(String ticketNumber) {
    return ticketRepository
        .findByTicketNumber(ticketNumber.trim())
        .orElseThrow(
            () -> RestException.notFound("Ticket not found with number: " + ticketNumber));
  }

  private boolean isRequesterOrSameOrg(Ticket ticket, User caller) {
    if (ticket.getReporter() != null && ticket.getReporter().getId().equals(caller.getId())) {
      return true;
    }
    if (caller.getUserType() == UserType.EXTERNAL_CUSTOMER && ticket.getOrganization() != null) {
      return (caller.getOrganizationName() != null
              && ticket.getOrganization().getName().equalsIgnoreCase(caller.getOrganizationName()))
          || (caller.getOrganizations() != null
              && caller.getOrganizations().stream()
                  .anyMatch(o -> o.getId().equals(ticket.getOrganization().getId())));
    }
    return false;
  }

  private void enforceReadAccess(Ticket ticket, User caller) {
    boolean isAdminOrAgent =
        caller.getRoles() != null
            && caller.getRoles().stream()
                .anyMatch(
                    r -> r.getName() == RoleType.ROLE_ADMIN || r.getName() == RoleType.ROLE_AGENT);
    if (isAdminOrAgent || isRequesterOrSameOrg(ticket, caller)) {
      return;
    }
    throw RestException.forbidden(
        "Access denied: You are not authorized to view feedback for ticket "
            + ticket.getTicketNumber());
  }

  private FeedbackResponse toResponse(
      TicketSatisfactionFeedback feedback, String ticketNumber, String submittedByUsername) {
    return FeedbackResponse.builder()
        .id(feedback.getId())
        .ticketNumber(ticketNumber)
        .rating(feedback.getRating())
        .feedbackComment(feedback.getFeedbackComment())
        .submittedByUsername(submittedByUsername)
        .submittedAt(feedback.getSubmittedAt())
        .build();
  }
}
