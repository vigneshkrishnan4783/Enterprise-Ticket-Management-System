package com.enterprise.ticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.request.CreateTeamRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamServiceTest {

  @Test
  @DisplayName("createTeam should validate empty payload")
  void testCreateTeamNullPayload() {
    TeamService service = new TeamService(null, null);
    RestException ex = assertThrows(RestException.class, () -> service.createTeam(null));
    assertEquals("Create team payload cannot be empty", ex.getMessage());
  }

  @Test
  @DisplayName("createTeam should validate blank name")
  void testCreateTeamBlankName() {
    TeamService service = new TeamService(null, null);
    CreateTeamRequest request = CreateTeamRequest.builder().name("   ").build();
    RestException ex = assertThrows(RestException.class, () -> service.createTeam(request));
    assertEquals("Team name is required", ex.getMessage());
  }
}
