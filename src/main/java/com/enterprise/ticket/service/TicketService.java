package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.AssignmentSource;
import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.TicketChannel;
import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Asset;
import com.enterprise.ticket.model.Category;
import com.enterprise.ticket.model.Impact;
import com.enterprise.ticket.model.Organization;
import com.enterprise.ticket.model.Site;
import com.enterprise.ticket.model.Subcategory;
import com.enterprise.ticket.model.Team;
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.TicketAssignmentHistory;
import com.enterprise.ticket.model.Urgency;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.AssetRepository;
import com.enterprise.ticket.repository.CategoryRepository;
import com.enterprise.ticket.repository.ImpactRepository;
import com.enterprise.ticket.repository.OrganizationRepository;
import com.enterprise.ticket.repository.SiteRepository;
import com.enterprise.ticket.repository.SubcategoryRepository;
import com.enterprise.ticket.repository.TeamRepository;
import com.enterprise.ticket.repository.TicketAssignmentHistoryRepository;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.UrgencyRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.AssignTicketRequest;
import com.enterprise.ticket.request.CreateTicketRequest;
import com.enterprise.ticket.request.TicketSearchFilter;
import com.enterprise.ticket.response.TicketAssignmentHistoryResponse;
import com.enterprise.ticket.response.TicketResponse;
import com.enterprise.ticket.specification.TicketSpecification;
import com.enterprise.ticket.support.TicketNumberGenerator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

  private final TicketRepository ticketRepository;
  private final UserRepository userRepository;
  private final OrganizationRepository organizationRepository;
  private final SiteRepository siteRepository;
  private final AssetRepository assetRepository;
  private final CategoryRepository categoryRepository;
  private final SubcategoryRepository subcategoryRepository;
  private final ImpactRepository impactRepository;
  private final UrgencyRepository urgencyRepository;
  private final TeamRepository teamRepository;
  private final TicketAssignmentHistoryRepository assignmentHistoryRepository;
  private final PriorityConfigService priorityConfigService;
  private final AssignmentRuleEngine assignmentRuleEngine;
  private final TicketNumberGenerator ticketNumberGenerator;

  @Transactional
  public TicketResponse createTicket(CreateTicketRequest request, String reporterUsername) {
    validateCreateTicketRequest(request);

    User reporter =
        userRepository
            .findByUsername(reporterUsername)
            .orElseThrow(() -> RestException.notFound("User not found: " + reporterUsername));
    Organization organization = null;
    Long orgId = request.getOrganizationId();
    if (orgId != null) {
      organization =
          organizationRepository
              .findById(orgId)
              .orElseThrow(() -> RestException.notFound("Organization not found with ID: " + orgId));

      validateUserOrganizationAccess(reporter, organization);
    }
    Site site = null;
    Long siteId = request.getSiteId();
    if (siteId != null) {
      if (organization == null) {
        throw RestException.badRequest("Site requires an Organization to be selected");
      }
      site =
          siteRepository
              .findByIdAndOrganizationId(siteId, orgId)
              .orElseThrow(
                  () ->
                      RestException.badRequest(
                          "Selected Site does not belong to Organization ID: " + orgId));
    }
    Asset asset = null;
    Long assetId = request.getAssetId();
    if (assetId != null) {
      if (organization == null) {
        throw RestException.badRequest("Asset requires an Organization to be selected");
      }
      asset =
          assetRepository
              .findByIdAndOrganizationId(assetId, orgId)
              .orElseThrow(
                  () ->
                      RestException.badRequest(
                          "Selected Asset does not belong to Organization ID: " + orgId));

      if (site != null && asset.getSite() != null && !asset.getSite().getId().equals(site.getId())) {
        throw RestException.badRequest(
            "Selected Asset does not belong to Site ID: " + site.getId());
      }
    }
    Category category = null;
    Long catId = request.getCategoryId();
    if (catId != null) {
      category =
          categoryRepository
              .findById(catId)
              .orElseThrow(() -> RestException.notFound("Category not found with ID: " + catId));
    }

    Subcategory subcategory = null;
    Long subcatId = request.getSubcategoryId();
    if (subcatId != null) {
      if (category == null) {
        throw RestException.badRequest("Subcategory requires a Category to be selected");
      }
      subcategory =
          subcategoryRepository
              .findByIdAndCategoryId(subcatId, catId)
              .orElseThrow(
                  () ->
                      RestException.badRequest(
                          "Selected Subcategory does not belong to Category ID: " + catId));
    }
    Impact impact = null;
    if (request.getImpactId() != null) {
      impact =
          impactRepository
              .findById(request.getImpactId())
              .orElseThrow(
                  () -> RestException.notFound("Impact not found with ID: " + request.getImpactId()));
    }

    Urgency urgency = null;
    if (request.getUrgencyId() != null) {
      urgency =
          urgencyRepository
              .findById(request.getUrgencyId())
              .orElseThrow(
                  () -> RestException.notFound("Urgency not found with ID: " + request.getUrgencyId()));
    }

    TicketPriority resolvedPriority = request.getPriority();
    if (impact != null && urgency != null) {
      resolvedPriority = priorityConfigService.calculatePriority(impact.getId(), urgency.getId());
    } else if (resolvedPriority == null) {
      resolvedPriority = TicketPriority.MEDIUM;
    }

    String generatedNumber = ticketNumberGenerator.generateTicketNumber();

    Ticket ticket =
        Ticket.builder()
            .ticketNumber(generatedNumber)
            .title(request.getTitle().trim())
            .description(request.getDescription().trim())
            .priority(resolvedPriority)
            .channel(request.getChannel() != null ? request.getChannel() : TicketChannel.WEB)
            .status(TicketStatus.OPEN)
            .reporter(reporter)
            .organization(organization)
            .site(site)
            .asset(asset)
            .category(category)
            .subcategory(subcategory)
            .impact(impact)
            .urgency(urgency)
            .build();
    Optional<AssignmentRuleEngine.RuleEvaluationResult> autoAssignResult =
        assignmentRuleEngine.evaluate(ticket);

    if (autoAssignResult.isPresent()) {
      AssignmentRuleEngine.RuleEvaluationResult result = autoAssignResult.get();
      ticket.setAssignedTeam(result.getTargetTeam());
      ticket.setAssignedAgent(result.getTargetAgent());
      ticket.setAssignedAt(Instant.now());
      ticket.setAssignmentSource(AssignmentSource.AUTO_RULE);
      ticket.setAssignmentReason(result.getReason());
      ticket.setStatus(TicketStatus.IN_PROGRESS);

      ticket = ticketRepository.save(ticket);
      TicketAssignmentHistory history =
          TicketAssignmentHistory.builder()
              .ticket(ticket)
              .previousTeam(null)
              .newTeam(result.getTargetTeam())
              .previousAgent(null)
              .newAgent(result.getTargetAgent())
              .assignedBy(null)
              .assignmentSource(AssignmentSource.AUTO_RULE)
              .reason(result.getReason())
              .rule(result.getMatchedRule())
              .comment("Auto-assigned on ticket creation")
              .build();
      assignmentHistoryRepository.save(history);
    } else {
      ticket = ticketRepository.save(ticket);
    }

    return toTicketResponse(ticket);
  }

    @Transactional(readOnly = true)
  public TicketResponse getTicketByNumber(String ticketNumber, String username) {
    if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
      throw RestException.badRequest("Ticket number is required");
    }
    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));

    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "read");
    return toTicketResponse(ticket);
  }

  @Transactional(readOnly = true)
  public Page<TicketResponse> getMyTickets(String reporterUsername, Pageable pageable) {
    User reporter =
        userRepository
            .findByUsername(reporterUsername)
            .orElseThrow(() -> RestException.notFound("User not found: " + reporterUsername));
    return ticketRepository
        .findByReporterId(reporter.getId(), pageable)
        .map(this::toTicketResponse);
  }

    @Transactional(readOnly = true)
  public Page<TicketResponse> searchTickets(
      TicketSearchFilter filter, String username, Pageable pageable) {
    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));
    if (caller.getUserType() == UserType.EXTERNAL_CUSTOMER) {
      if (caller.getOrganizations() != null && !caller.getOrganizations().isEmpty()) {
        filter.setOrganizationId(caller.getOrganizations().iterator().next().getId());
      } else {
        filter.setReporterId(caller.getId());
      }
    }

    return ticketRepository
        .findAll(TicketSpecification.withFilter(filter), pageable)
        .map(this::toTicketResponse);
  }

    @Transactional
  public TicketResponse assignTicket(
      String ticketNumber, AssignTicketRequest request, String assignedByUsername) {
    if (request == null || (request.getTeamId() == null && request.getAgentId() == null)) {
      throw RestException.badRequest("At least one target (teamId or agentId) must be specified");
    }

    User assigner =
        userRepository
            .findByUsername(assignedByUsername)
            .orElseThrow(() -> RestException.notFound("User not found: " + assignedByUsername));

    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> RestException.notFound("Ticket not found with number: " + ticketNumber));
    enforceTicketAccess(ticket, assigner, "assign");

    Team previousTeam = ticket.getAssignedTeam();
    User previousAgent = ticket.getAssignedAgent();

    Team newTeam = null;
    if (request.getTeamId() != null) {
      newTeam =
          teamRepository
              .findById(request.getTeamId())
              .orElseThrow(
                  () -> RestException.notFound("Team not found with ID: " + request.getTeamId()));
    } else {
      newTeam = previousTeam;
    }

    User newAgent = null;
    if (request.getAgentId() != null) {
      newAgent =
          userRepository
              .findById(request.getAgentId())
              .orElseThrow(
                  () -> RestException.notFound("Agent not found with ID: " + request.getAgentId()));
      if (newTeam != null) {
        boolean isMember =
            newTeam.getMembers().stream().anyMatch(m -> m.getId().equals(request.getAgentId()));
        if (!isMember) {
          throw RestException.badRequest(
              "Agent '" + newAgent.getUsername() + "' is not a member of team '" + newTeam.getName() + "'");
        }
      }
    } else {
      newAgent = previousAgent;
    }

    String reason =
        (request.getComment() != null && !request.getComment().trim().isEmpty())
            ? request.getComment().trim()
            : "Manually assigned by " + assigner.getUsername();

    ticket.setAssignedTeam(newTeam);
    ticket.setAssignedAgent(newAgent);
    ticket.setAssignedAt(Instant.now());
    ticket.setAssignmentSource(AssignmentSource.MANUAL);
    ticket.setAssignmentReason(reason);
    if (ticket.getStatus() == TicketStatus.OPEN) {
      ticket.setStatus(TicketStatus.IN_PROGRESS);
    }

    ticket = ticketRepository.save(ticket);
    TicketAssignmentHistory history =
        TicketAssignmentHistory.builder()
            .ticket(ticket)
            .previousTeam(previousTeam)
            .newTeam(newTeam)
            .previousAgent(previousAgent)
            .newAgent(newAgent)
            .assignedBy(assigner)
            .assignmentSource(AssignmentSource.MANUAL)
            .reason(reason)
            .comment(request.getComment() != null ? request.getComment().trim() : null)
            .build();
    assignmentHistoryRepository.save(history);

    return toTicketResponse(ticket);
  }

    @Transactional(readOnly = true)
  public List<TicketAssignmentHistoryResponse> getAssignmentHistory(
      String ticketNumber, String username) {
    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));

    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "read assignment history of");

    return assignmentHistoryRepository.findByTicketIdOrderByCreatedAtDesc(ticket.getId()).stream()
        .map(h -> toAssignmentHistoryResponse(h, ticket.getTicketNumber()))
        .collect(Collectors.toList());
  }

    @Transactional
  public TicketResponse autoAssignTicket(String ticketNumber, String username) {
    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));

    Ticket ticket =
        ticketRepository
            .findByTicketNumber(ticketNumber.trim())
            .orElseThrow(
                () -> RestException.notFound("Ticket not found with number: " + ticketNumber));

    enforceTicketAccess(ticket, caller, "auto-assign");

    Optional<AssignmentRuleEngine.RuleEvaluationResult> matchResult =
        assignmentRuleEngine.evaluate(ticket);

    if (matchResult.isEmpty()) {
      throw RestException.badRequest("No active assignment rule matched the ticket criteria");
    }

    AssignmentRuleEngine.RuleEvaluationResult result = matchResult.get();
    Team previousTeam = ticket.getAssignedTeam();
    User previousAgent = ticket.getAssignedAgent();

    ticket.setAssignedTeam(result.getTargetTeam());
    ticket.setAssignedAgent(result.getTargetAgent());
    ticket.setAssignedAt(Instant.now());
    ticket.setAssignmentSource(AssignmentSource.AUTO_RULE);
    ticket.setAssignmentReason(result.getReason());

    if (ticket.getStatus() == TicketStatus.OPEN) {
      ticket.setStatus(TicketStatus.IN_PROGRESS);
    }

    ticket = ticketRepository.save(ticket);

    TicketAssignmentHistory history =
        TicketAssignmentHistory.builder()
            .ticket(ticket)
            .previousTeam(previousTeam)
            .newTeam(result.getTargetTeam())
            .previousAgent(previousAgent)
            .newAgent(result.getTargetAgent())
            .assignedBy(caller)
            .assignmentSource(AssignmentSource.AUTO_RULE)
            .reason(result.getReason())
            .rule(result.getMatchedRule())
            .comment("Triggered rule-based automatic assignment")
            .build();
    assignmentHistoryRepository.save(history);

    return toTicketResponse(ticket);
  }

    private void enforceTicketAccess(Ticket ticket, User caller, String action) {
    Set<RoleType> roles =
        caller.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());
    if (roles.contains(RoleType.ROLE_ADMIN)) {
      return;
    }
    if (caller.getUserType() == UserType.EXTERNAL_CUSTOMER) {
      boolean isReporter = ticket.getReporter().getId().equals(caller.getId());
      boolean isSameOrg =
          ticket.getOrganization() != null
              && (ticket.getOrganization().getName().equalsIgnoreCase(caller.getOrganizationName())
                  || (caller.getOrganizations() != null
                      && caller.getOrganizations().stream()
                          .anyMatch(o -> o.getId().equals(ticket.getOrganization().getId()))));

      if (!isReporter && !isSameOrg) {
        throw RestException.forbidden(
            "Access denied: You are not authorized to " + action + " ticket " + ticket.getTicketNumber());
      }
      return;
    }
    boolean isReporter = ticket.getReporter().getId().equals(caller.getId());
    boolean isAssignedAgent =
        ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(caller.getId());
    boolean isAssignedTeamMember =
        ticket.getAssignedTeam() != null
            && ticket.getAssignedTeam().getMembers().stream()
                .anyMatch(m -> m.getId().equals(caller.getId()));

    if (!isReporter && !isAssignedAgent && !isAssignedTeamMember && !roles.contains(RoleType.ROLE_AGENT)) {
      throw RestException.forbidden(
          "Access denied: You are not authorized to " + action + " ticket " + ticket.getTicketNumber());
    }
  }

  private void validateUserOrganizationAccess(User user, Organization organization) {
    if (user.getUserType() == UserType.EXTERNAL_CUSTOMER) {
      boolean matchesOrgName =
          user.getOrganizationName() != null
              && user.getOrganizationName().equalsIgnoreCase(organization.getName());

      boolean matchesOrgList =
          user.getOrganizations() != null
              && user.getOrganizations().stream()
                  .anyMatch(org -> org.getId().equals(organization.getId()));

      if (!matchesOrgName && !matchesOrgList) {
        throw RestException.forbidden(
            "Access denied: User is not authorized to create tickets for organization: "
                + organization.getName());
      }
    }
  }

  private void validateCreateTicketRequest(CreateTicketRequest request) {
    if (request == null) {
      throw RestException.badRequest("Create ticket payload cannot be empty");
    }
    if (request.getTitle() == null || request.getTitle().trim().length() < 5) {
      throw RestException.badRequest("Title is required and must be at least 5 characters");
    }
    if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
      throw RestException.badRequest("Description is required");
    }
  }

  private TicketResponse toTicketResponse(Ticket ticket) {
    return TicketResponse.builder()
        .id(ticket.getId())
        .ticketNumber(ticket.getTicketNumber())
        .title(ticket.getTitle())
        .description(ticket.getDescription())
        .status(ticket.getStatus())
        .priority(ticket.getPriority())
        .channel(ticket.getChannel())
        .reporterUsername(ticket.getReporter() != null ? ticket.getReporter().getUsername() : null)
        .organizationName(ticket.getOrganization() != null ? ticket.getOrganization().getName() : null)
        .siteName(ticket.getSite() != null ? ticket.getSite().getName() : null)
        .assetTag(ticket.getAsset() != null ? ticket.getAsset().getAssetTag() : null)
        .assetName(ticket.getAsset() != null ? ticket.getAsset().getName() : null)
        .categoryName(ticket.getCategory() != null ? ticket.getCategory().getName() : null)
        .subcategoryName(ticket.getSubcategory() != null ? ticket.getSubcategory().getName() : null)
        .impactId(ticket.getImpact() != null ? ticket.getImpact().getId() : null)
        .impactName(ticket.getImpact() != null ? ticket.getImpact().getName() : null)
        .urgencyId(ticket.getUrgency() != null ? ticket.getUrgency().getId() : null)
        .urgencyName(ticket.getUrgency() != null ? ticket.getUrgency().getName() : null)
        .assignedTeamId(ticket.getAssignedTeam() != null ? ticket.getAssignedTeam().getId() : null)
        .assignedTeamName(
            ticket.getAssignedTeam() != null ? ticket.getAssignedTeam().getName() : null)
        .assignedAgentId(ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getId() : null)
        .assignedAgentUsername(
            ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getUsername() : null)
        .assignedAt(ticket.getAssignedAt())
        .assignmentSource(ticket.getAssignmentSource())
        .assignmentReason(ticket.getAssignmentReason())
        .createdAt(ticket.getCreatedAt())
        .updatedAt(ticket.getUpdatedAt())
        .build();
  }

  private TicketAssignmentHistoryResponse toAssignmentHistoryResponse(
      TicketAssignmentHistory history, String ticketNumber) {
    return TicketAssignmentHistoryResponse.builder()
        .id(history.getId())
        .ticketId(history.getTicket().getId())
        .ticketNumber(ticketNumber)
        .previousTeamName(
            history.getPreviousTeam() != null ? history.getPreviousTeam().getName() : null)
        .newTeamName(history.getNewTeam() != null ? history.getNewTeam().getName() : null)
        .previousAgentUsername(
            history.getPreviousAgent() != null ? history.getPreviousAgent().getUsername() : null)
        .newAgentUsername(history.getNewAgent() != null ? history.getNewAgent().getUsername() : null)
        .assignedByUsername(
            history.getAssignedBy() != null ? history.getAssignedBy().getUsername() : "SYSTEM_RULE")
        .assignmentSource(history.getAssignmentSource())
        .reason(history.getReason())
        .ruleId(history.getRule() != null ? history.getRule().getId() : null)
        .ruleName(history.getRule() != null ? history.getRule().getName() : null)
        .comment(history.getComment())
        .createdAt(history.getCreatedAt())
        .build();
  }
}
