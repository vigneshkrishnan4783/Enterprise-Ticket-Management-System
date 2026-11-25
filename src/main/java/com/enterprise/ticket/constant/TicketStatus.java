package com.enterprise.ticket.constant;

public enum TicketStatus {
    OPEN("Created and awaiting triage or assignment"),

    ASSIGNED("Owned by a team and/or agent"),

    IN_PROGRESS("Investigation or action underway"),

    PENDING_CUSTOMER("Waiting for requester response or action"),

    PENDING_INTERNAL("Waiting for internal team or dependency"),

    RESOLVED("Resolution recorded and sent to requester"),

    CLOSED("Work completed and ticket closed"),

    REOPENED("Previously resolved/closed issue is active again"),

    CANCELLED("Duplicate, withdrawn, or invalid ticket");

  private final String description;

  TicketStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
