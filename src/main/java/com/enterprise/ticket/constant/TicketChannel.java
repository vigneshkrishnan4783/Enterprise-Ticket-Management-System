package com.enterprise.ticket.constant;

/**
 * Intake channels through which complaints/tickets are submitted (FR-003).
 */
public enum TicketChannel {
  WEB("Web portal submission"),
  MOBILE("Mobile app submission"),
  EMAIL("Inbound email conversion"),
  API("External system or webhook API"),
  PORTAL("Customer self-service portal");

  private final String description;

  TicketChannel(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
