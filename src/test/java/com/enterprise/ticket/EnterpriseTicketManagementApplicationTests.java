package com.enterprise.ticket;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnterpriseTicketManagementApplicationTests {

  @Test
  @DisplayName("Context loads verification for main application class")
  void contextLoads() {
    EnterpriseTicketManagementApplication app = new EnterpriseTicketManagementApplication();
    assertNotNull(app);
  }
}
