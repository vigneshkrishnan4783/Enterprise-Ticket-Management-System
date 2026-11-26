package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.request.CreateAssignmentRuleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssignmentRuleServiceTest {

  @Test
  @DisplayName("createRule should validate empty payload")
  void testCreateRuleNullPayload() {
    AssignmentRuleService service =
        new AssignmentRuleService(null, null, null, null, null, null, null, null, null);
    RestException ex = assertThrows(RestException.class, () -> service.createRule(null));
    assertEquals("Create assignment rule payload cannot be empty", ex.getMessage());
  }

  @Test
  @DisplayName("createRule should validate blank rule name")
  void testCreateRuleBlankName() {
    AssignmentRuleService service =
        new AssignmentRuleService(null, null, null, null, null, null, null, null, null);
    CreateAssignmentRuleRequest req = CreateAssignmentRuleRequest.builder().name("   ").build();
    RestException ex = assertThrows(RestException.class, () -> service.createRule(req));
    assertEquals("Rule name is required", ex.getMessage());
  }

  @Test
  @DisplayName("createRule should validate missing target team ID")
  void testCreateRuleMissingTargetTeam() {
    AssignmentRuleService service =
        new AssignmentRuleService(null, null, null, null, null, null, null, null, null);
    CreateAssignmentRuleRequest req =
        CreateAssignmentRuleRequest.builder().name("Valid Rule Name").targetTeamId(null).build();
    RestException ex = assertThrows(RestException.class, () -> service.createRule(req));
    assertEquals("Target team ID is required", ex.getMessage());
  }
}
