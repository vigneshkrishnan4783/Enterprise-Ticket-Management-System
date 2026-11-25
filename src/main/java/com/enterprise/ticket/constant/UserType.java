package com.enterprise.ticket.constant;

public enum UserType {
  INTERNAL("Internal employee or staff member"),
  EXTERNAL_CUSTOMER("External customer or client organization");

  private final String description;

  UserType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
