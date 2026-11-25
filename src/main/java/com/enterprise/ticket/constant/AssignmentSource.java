package com.enterprise.ticket.constant;

public enum AssignmentSource {
  MANUAL("Manual assignment by user or manager"),
  AUTO_RULE("Automatic assignment by configured routing rule"),
  SYSTEM("System default or fallback assignment");

  private final String description;

  AssignmentSource(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
