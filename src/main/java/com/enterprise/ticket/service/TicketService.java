package com.enterprise.ticket.service;

import com.enterprise.ticket.request.CreateTicketRequest;
import com.enterprise.ticket.response.TicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketService {

  TicketResponse createTicket(CreateTicketRequest request, String reporterUsername);

  TicketResponse getTicketByNumber(String ticketNumber);

  Page<TicketResponse> getMyTickets(String reporterUsername, Pageable pageable);
}
