package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.CreateTicketRequest;
import com.enterprise.ticket.response.TicketResponse;
import com.enterprise.ticket.support.TicketNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing ticket lifecycle with internal business validation.
 */
@Service
@RequiredArgsConstructor
public class TicketService {

  private final TicketRepository ticketRepository;
  private final UserRepository userRepository;
  private final TicketNumberGenerator ticketNumberGenerator;

  @Transactional
  public TicketResponse createTicket(CreateTicketRequest request, String reporterUsername) {
    validateCreateTicketRequest(request);

    User reporter =
        userRepository
            .findByUsername(reporterUsername)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + reporterUsername));

    String generatedNumber = ticketNumberGenerator.generateTicketNumber();

    Ticket ticket =
        Ticket.builder()
            .ticketNumber(generatedNumber)
            .title(request.getTitle().trim())
            .description(request.getDescription().trim())
            .priority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM)
            .category(
                request.getCategory() != null && !request.getCategory().isBlank()
                    ? request.getCategory().trim()
                    : "GENERAL")
            .status(TicketStatus.OPEN)
            .reporter(reporter)
            .build();

    ticket = ticketRepository.save(ticket);
    return toTicketResponse(ticket);
  }

  @Transactional(readOnly = true)
  public TicketResponse getTicketByNumber(String ticketNumber) {
    if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
      throw new IllegalArgumentException("Ticket number is required");
    }
    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> new IllegalArgumentException("Ticket not found with number: " + ticketNumber));
    return toTicketResponse(ticket);
  }

  @Transactional(readOnly = true)
  public Page<TicketResponse> getMyTickets(String reporterUsername, Pageable pageable) {
    User reporter =
        userRepository
            .findByUsername(reporterUsername)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + reporterUsername));
    return ticketRepository
        .findByReporterId(reporter.getId(), pageable)
        .map(this::toTicketResponse);
  }

  private void validateCreateTicketRequest(CreateTicketRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Create ticket payload cannot be empty");
    }
    if (request.getTitle() == null || request.getTitle().trim().length() < 5) {
      throw new IllegalArgumentException("Title is required and must be at least 5 characters");
    }
    if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
      throw new IllegalArgumentException("Description is required");
    }
  }

  private TicketResponse toTicketResponse(Ticket ticket) {
    return TicketResponse.builder()
        .id(ticket.getId())
        .ticketNumber(ticket.getTicketNumber())
        .title(ticket.getTitle())
        .description(ticket.getDescription())
        .status(ticket.getStatus())
        .priority(ticket.getPriority())
        .category(ticket.getCategory())
        .reporterUsername(ticket.getReporter() != null ? ticket.getReporter().getUsername() : null)
        .reporterOrganization(
            ticket.getReporter() != null ? ticket.getReporter().getOrganizationName() : null)
        .createdAt(ticket.getCreatedAt())
        .updatedAt(ticket.getUpdatedAt())
        .build();
  }
}
