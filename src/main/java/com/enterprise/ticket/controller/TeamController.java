package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.CreateTeamRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.response.TeamResponse;
import com.enterprise.ticket.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Endpoints for managing support teams and team membership")
public class TeamController {

  private final TeamService teamService;

  @GetMapping
  @Operation(summary = "List all active teams", description = "Retrieves active support teams and their assigned member agents")
  public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllActiveTeams() {
    return ResponseBuilder.<List<TeamResponse>>builder()
        .success()
        .withData(teamService.getAllActiveTeams())
        .build();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get team by ID", description = "Retrieves team details by ID")
  public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable Long id) {
    return ResponseBuilder.<TeamResponse>builder()
        .success()
        .withData(teamService.getTeamById(id))
        .build();
  }

  @PostMapping
  @Operation(summary = "Create team", description = "Creates a new support team")
  public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
      @RequestBody CreateTeamRequest request) {
    return ResponseBuilder.<TeamResponse>builder()
        .created("Team created successfully")
        .withData(teamService.createTeam(request))
        .build();
  }

  @PostMapping("/{teamId}/members/{userId}")
  @Operation(summary = "Add member to team", description = "Adds an agent/user to a support team")
  public ResponseEntity<ApiResponse<TeamResponse>> addMemberToTeam(
      @PathVariable Long teamId, @PathVariable Long userId) {
    return ResponseBuilder.<TeamResponse>builder()
        .success("Member added to team successfully")
        .withData(teamService.addMemberToTeam(teamId, userId))
        .build();
  }
}
