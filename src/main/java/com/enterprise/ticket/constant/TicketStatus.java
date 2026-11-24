package com.enterprise.ticket.constant;

/** Standard Ticket Statuses as defined in SRS section 5 (Ticket lifecycle and workflows). */
public enum TicketStatus {
  /** Created and awaiting triage or assignment. */
  OPEN("Created and awaiting triage or assignment"),

  /** Owned by a team and/or support agent. */
  ASSIGNED("Owned by a team and/or agent"),

  /** Investigation or resolution action is actively underway. */
  IN_PROGRESS("Investigation or action underway"),

  /** Paused: Waiting for requester response or clarification. */
  PENDING_CUSTOMER("Waiting for requester response or action"),

  /** Waiting for another internal department or dependency. */
  PENDING_INTERNAL("Waiting for internal team or dependency"),

  /** Resolution recorded and sent to requester. */
  RESOLVED("Resolution recorded and sent to requester"),

  /** Work completed and ticket formally closed. */
  CLOSED("Work completed and ticket closed"),

  /** Previously resolved or closed issue is active again. */
  REOPENED("Previously resolved/closed issue is active again"),

  /** Duplicate, withdrawn, or invalid ticket. */
  CANCELLED("Duplicate, withdrawn, or invalid ticket");

  private final String description;

  TicketStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
