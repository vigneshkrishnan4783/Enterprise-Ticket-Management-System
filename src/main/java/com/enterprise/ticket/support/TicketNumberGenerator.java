package com.enterprise.ticket.support;

import com.enterprise.ticket.repository.TicketRepository;
import org.springframework.stereotype.Component;

@Component
public class TicketNumberGenerator {

  private final TicketRepository ticketRepository;

  public TicketNumberGenerator(TicketRepository ticketRepository) {
    this.ticketRepository = ticketRepository;
  }

  public String generateTicketNumber() {
    Long nextSeq = ticketRepository.getNextTicketSequence();
    return String.format("TKT-%05d", nextSeq);
  }
}
