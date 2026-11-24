package com.enterprise.ticket.service;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Team;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.TeamRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.CreateTeamRequest;
import com.enterprise.ticket.response.TeamResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {

  private final TeamRepository teamRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<TeamResponse> getAllActiveTeams() {
    return teamRepository.findByIsActiveTrue().stream()
        .map(this::toTeamResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public TeamResponse getTeamById(Long teamId) {
    Team team =
        teamRepository
            .findById(teamId)
            .orElseThrow(() -> RestException.notFound("Team not found with ID: " + teamId));
    return toTeamResponse(team);
  }

  @Transactional
  public TeamResponse createTeam(CreateTeamRequest request) {
    if (request == null) {
      throw RestException.badRequest("Create team payload cannot be empty");
    }
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw RestException.badRequest("Team name is required");
    }
    if (request.getCode() == null || request.getCode().trim().isEmpty()) {
      throw RestException.badRequest("Team code is required");
    }
    if (teamRepository.existsByCode(request.getCode().trim().toUpperCase())) {
      throw RestException.conflict("Team code already exists: " + request.getCode());
    }
    if (teamRepository.existsByName(request.getName().trim())) {
      throw RestException.conflict("Team name already exists: " + request.getName());
    }

    Team team =
        Team.builder()
            .name(request.getName().trim())
            .code(request.getCode().trim().toUpperCase())
            .description(request.getDescription() != null ? request.getDescription().trim() : null)
            .isActive(true)
            .build();

    return toTeamResponse(teamRepository.save(team));
  }

  @Transactional
  public TeamResponse addMemberToTeam(Long teamId, Long userId) {
    Team team =
        teamRepository
            .findById(teamId)
            .orElseThrow(() -> RestException.notFound("Team not found with ID: " + teamId));

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> RestException.notFound("User not found with ID: " + userId));

    team.getMembers().add(user);
    return toTeamResponse(teamRepository.save(team));
  }

  private TeamResponse toTeamResponse(Team team) {
    List<String> memberUsernames =
        team.getMembers().stream().map(User::getUsername).sorted().collect(Collectors.toList());

    return TeamResponse.builder()
        .id(team.getId())
        .name(team.getName())
        .code(team.getCode())
        .description(team.getDescription())
        .isActive(team.getIsActive())
        .memberUsernames(memberUsernames)
        .createdAt(team.getCreatedAt())
        .build();
  }
}
