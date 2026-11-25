package com.enterprise.ticket.constant;

public enum TicketPriority {
  CRITICAL("Critical business impact requiring immediate response"),
  HIGH("High priority issue impacting key business operations"),
  MEDIUM("Standard business issue with normal handling priority"),
  LOW("Minor issue or cosmetic request with standard turnaround");

  private final String description;

  TicketPriority(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
