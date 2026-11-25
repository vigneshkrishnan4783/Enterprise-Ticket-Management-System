package com.enterprise.ticket.service;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.AssignmentRule;
import com.enterprise.ticket.model.Category;
import com.enterprise.ticket.model.Impact;
import com.enterprise.ticket.model.Organization;
import com.enterprise.ticket.model.Site;
import com.enterprise.ticket.model.Subcategory;
import com.enterprise.ticket.model.Team;
import com.enterprise.ticket.model.Urgency;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.AssignmentRuleRepository;
import com.enterprise.ticket.repository.CategoryRepository;
import com.enterprise.ticket.repository.ImpactRepository;
import com.enterprise.ticket.repository.OrganizationRepository;
import com.enterprise.ticket.repository.SiteRepository;
import com.enterprise.ticket.repository.SubcategoryRepository;
import com.enterprise.ticket.repository.TeamRepository;
import com.enterprise.ticket.repository.UrgencyRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.CreateAssignmentRuleRequest;
import com.enterprise.ticket.response.AssignmentRuleResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignmentRuleService {

  private final AssignmentRuleRepository assignmentRuleRepository;
  private final CategoryRepository categoryRepository;
  private final SubcategoryRepository subcategoryRepository;
  private final OrganizationRepository organizationRepository;
  private final SiteRepository siteRepository;
  private final ImpactRepository impactRepository;
  private final UrgencyRepository urgencyRepository;
  private final TeamRepository teamRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<AssignmentRuleResponse> getAllActiveRules() {
    return assignmentRuleRepository.findByIsActiveTrueOrderByPriorityOrderAsc().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public AssignmentRuleResponse getRuleById(Long id) {
    AssignmentRule rule =
        assignmentRuleRepository
            .findById(id)
            .orElseThrow(() -> RestException.notFound("Assignment rule not found with ID: " + id));
    return toResponse(rule);
  }

  @Transactional
  public AssignmentRuleResponse createRule(CreateAssignmentRuleRequest request) {
    validateRequest(request);

    if (assignmentRuleRepository.existsByName(request.getName().trim())) {
      throw RestException.conflict("Assignment rule already exists with name: " + request.getName());
    }

    Team targetTeam =
        teamRepository
            .findById(request.getTargetTeamId())
            .orElseThrow(
                () ->
                    RestException.notFound(
                        "Target team not found with ID: " + request.getTargetTeamId()));

    User targetAgent = null;
    if (request.getTargetAgentId() != null) {
      targetAgent =
          userRepository
              .findById(request.getTargetAgentId())
              .orElseThrow(
                  () ->
                      RestException.notFound(
                          "Target agent not found with ID: " + request.getTargetAgentId()));

      boolean isMember =
          targetTeam.getMembers().stream().anyMatch(m -> m.getId().equals(request.getTargetAgentId()));
      if (!isMember) {
        throw RestException.badRequest(
            "Agent '"
                + targetAgent.getUsername()
                + "' is not a member of team '"
                + targetTeam.getName()
                + "'");
      }
    }

    Category category =
        request.getCategoryId() != null
            ? categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(
                    () ->
                        RestException.notFound(
                            "Category not found with ID: " + request.getCategoryId()))
            : null;

    Subcategory subcategory =
        request.getSubcategoryId() != null
            ? subcategoryRepository
                .findById(request.getSubcategoryId())
                .orElseThrow(
                    () ->
                        RestException.notFound(
                            "Subcategory not found with ID: " + request.getSubcategoryId()))
            : null;

    Organization organization =
        request.getOrganizationId() != null
            ? organizationRepository
                .findById(request.getOrganizationId())
                .orElseThrow(
                    () ->
                        RestException.notFound(
                            "Organization not found with ID: " + request.getOrganizationId()))
            : null;

    Site site =
        request.getSiteId() != null
            ? siteRepository
                .findById(request.getSiteId())
                .orElseThrow(
                    () -> RestException.notFound("Site not found with ID: " + request.getSiteId()))
            : null;

    Impact impact =
        request.getImpactId() != null
            ? impactRepository
                .findById(request.getImpactId())
                .orElseThrow(
                    () ->
                        RestException.notFound(
                            "Impact not found with ID: " + request.getImpactId()))
            : null;

    Urgency urgency =
        request.getUrgencyId() != null
            ? urgencyRepository
                .findById(request.getUrgencyId())
                .orElseThrow(
                    () ->
                        RestException.notFound(
                            "Urgency not found with ID: " + request.getUrgencyId()))
            : null;

    AssignmentRule rule =
        AssignmentRule.builder()
            .name(request.getName().trim())
            .description(request.getDescription() != null ? request.getDescription().trim() : null)
            .priorityOrder(request.getPriorityOrder() != null ? request.getPriorityOrder() : 100)
            .isActive(true)
            .category(category)
            .subcategory(subcategory)
            .organization(organization)
            .site(site)
            .impact(impact)
            .urgency(urgency)
            .priority(request.getPriority())
            .channel(request.getChannel())
            .targetTeam(targetTeam)
            .targetAgent(targetAgent)
            .reasonTemplate(
                request.getReasonTemplate() != null ? request.getReasonTemplate().trim() : null)
            .build();

    return toResponse(assignmentRuleRepository.save(rule));
  }

  @Transactional
  public void deactivateRule(Long id) {
    AssignmentRule rule =
        assignmentRuleRepository
            .findById(id)
            .orElseThrow(() -> RestException.notFound("Assignment rule not found with ID: " + id));
    rule.setIsActive(false);
    assignmentRuleRepository.save(rule);
  }

  private void validateRequest(CreateAssignmentRuleRequest request) {
    if (request == null) {
      throw RestException.badRequest("Create assignment rule payload cannot be empty");
    }
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw RestException.badRequest("Rule name is required");
    }
    if (request.getTargetTeamId() == null) {
      throw RestException.badRequest("Target team ID is required");
    }
  }

  private AssignmentRuleResponse toResponse(AssignmentRule rule) {
    return AssignmentRuleResponse.builder()
        .id(rule.getId())
        .name(rule.getName())
        .description(rule.getDescription())
        .priorityOrder(rule.getPriorityOrder())
        .isActive(rule.getIsActive())
        .categoryId(rule.getCategory() != null ? rule.getCategory().getId() : null)
        .categoryName(rule.getCategory() != null ? rule.getCategory().getName() : null)
        .subcategoryId(rule.getSubcategory() != null ? rule.getSubcategory().getId() : null)
        .subcategoryName(rule.getSubcategory() != null ? rule.getSubcategory().getName() : null)
        .organizationId(rule.getOrganization() != null ? rule.getOrganization().getId() : null)
        .organizationName(rule.getOrganization() != null ? rule.getOrganization().getName() : null)
        .siteId(rule.getSite() != null ? rule.getSite().getId() : null)
        .siteName(rule.getSite() != null ? rule.getSite().getName() : null)
        .impactId(rule.getImpact() != null ? rule.getImpact().getId() : null)
        .impactName(rule.getImpact() != null ? rule.getImpact().getName() : null)
        .urgencyId(rule.getUrgency() != null ? rule.getUrgency().getId() : null)
        .urgencyName(rule.getUrgency() != null ? rule.getUrgency().getName() : null)
        .priority(rule.getPriority())
        .channel(rule.getChannel())
        .targetTeamId(rule.getTargetTeam().getId())
        .targetTeamName(rule.getTargetTeam().getName())
        .targetAgentId(rule.getTargetAgent() != null ? rule.getTargetAgent().getId() : null)
        .targetAgentUsername(
            rule.getTargetAgent() != null ? rule.getTargetAgent().getUsername() : null)
        .reasonTemplate(rule.getReasonTemplate())
        .createdAt(rule.getCreatedAt())
        .build();
  }
}
