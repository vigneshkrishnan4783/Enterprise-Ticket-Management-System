package com.enterprise.ticket.constant;

/** Role definitions matching the Stakeholders and Permissions matrix in the SRS. */
public enum RoleType {
  ROLE_ADMIN("System Administrator with complete access"),
  ROLE_AGENT("Support Agent investigating and resolving tickets"),
  ROLE_EMPLOYEE("Internal Requester creating and viewing own tickets"),
  ROLE_CUSTOMER("External Customer restricted to organization scope");

  private final String description;

  RoleType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
