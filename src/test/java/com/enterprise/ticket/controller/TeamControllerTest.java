package com.enterprise.ticket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enterprise.ticket.request.CreateTeamRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.TeamResponse;
import com.enterprise.ticket.service.TeamService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TeamControllerTest {

  @Test
  @DisplayName("getAllActiveTeams should return list of active teams")
  void testGetAllActiveTeams() {
    TeamService dummyTeamService = new TeamService(null, null) {
      @Override
      public List<TeamResponse> getAllActiveTeams() {
        return List.of(TeamResponse.builder().id(1L).name("L1 Support").build());
      }
    };

    TeamController controller = new TeamController(dummyTeamService);
    ResponseEntity<ApiResponse<List<TeamResponse>>> response = controller.getAllActiveTeams();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getData().size());
    assertEquals("L1 Support", response.getBody().getData().get(0).getName());
  }

  @Test
  @DisplayName("createTeam should return 201 Created and team response")
  void testCreateTeam() {
    TeamService dummyTeamService = new TeamService(null, null) {
      @Override
      public TeamResponse createTeam(CreateTeamRequest request) {
        return TeamResponse.builder().id(2L).name(request.getName()).build();
      }
    };

    TeamController controller = new TeamController(dummyTeamService);
    CreateTeamRequest req = CreateTeamRequest.builder().name("DevOps Team").build();
    ResponseEntity<ApiResponse<TeamResponse>> response = controller.createTeam(req);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isSuccess());
    assertEquals("DevOps Team", response.getBody().getData().getName());
  }
}
