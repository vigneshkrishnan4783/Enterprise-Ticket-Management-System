package com.enterprise.ticket.service;

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
import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.model.Urgency;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.AssetRepository;
import com.enterprise.ticket.repository.CategoryRepository;
import com.enterprise.ticket.repository.ImpactRepository;
import com.enterprise.ticket.repository.OrganizationRepository;
import com.enterprise.ticket.repository.SiteRepository;
import com.enterprise.ticket.repository.SubcategoryRepository;
import com.enterprise.ticket.repository.TicketRepository;
import com.enterprise.ticket.repository.UrgencyRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.request.CreateTicketRequest;
import com.enterprise.ticket.request.TicketSearchFilter;
import com.enterprise.ticket.response.TicketResponse;
import com.enterprise.ticket.specification.TicketSpecification;
import com.enterprise.ticket.support.TicketNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
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
  private final PriorityConfigService priorityConfigService;
  private final TicketNumberGenerator ticketNumberGenerator;

  @Transactional
  public TicketResponse createTicket(CreateTicketRequest request, String reporterUsername) {
    validateCreateTicketRequest(request);

    User reporter = userRepository
        .findByUsername(reporterUsername)
        .orElseThrow(() -> RestException.notFound("User not found: " + reporterUsername));

    // FR-003 & FR-004: Validate organization scope and access
    Organization organization = null;
    Long orgId = request.getOrganizationId();
    if (orgId != null) {
      organization = organizationRepository
          .findById(orgId)
          .orElseThrow(() -> RestException.notFound("Organization not found with ID: " + orgId));

      validateUserOrganizationAccess(reporter, organization);
    }

    // FR-004: Validate site scope
    Site site = null;
    Long siteId = request.getSiteId();
    if (siteId != null) {
      if (organization == null) {
        throw RestException.badRequest("Site requires an Organization to be selected");
      }
      site = siteRepository
          .findByIdAndOrganizationId(siteId, orgId)
          .orElseThrow(
              () -> RestException.badRequest(
                  "Selected Site does not belong to Organization ID: " + orgId));
    }

    // FR-004: Validate asset scope
    Asset asset = null;
    Long assetId = request.getAssetId();
    if (assetId != null) {
      if (organization == null) {
        throw RestException.badRequest("Asset requires an Organization to be selected");
      }
      asset = assetRepository
          .findByIdAndOrganizationId(assetId, orgId)
          .orElseThrow(
              () -> RestException.badRequest(
                  "Selected Asset does not belong to Organization ID: " + orgId));

      if (site != null && asset.getSite() != null && !asset.getSite().getId().equals(site.getId())) {
        throw RestException.badRequest(
            "Selected Asset does not belong to Site ID: " + site.getId());
      }
    }

    // FR-005: Validate category and subcategory
    Category category = null;
    Long catId = request.getCategoryId();
    if (catId != null) {
      category = categoryRepository
          .findById(catId)
          .orElseThrow(() -> RestException.notFound("Category not found with ID: " + catId));
    }

    Subcategory subcategory = null;
    Long subcatId = request.getSubcategoryId();
    if (subcatId != null) {
      if (category == null) {
        throw RestException.badRequest("Subcategory requires a Category to be selected");
      }
      subcategory = subcategoryRepository
          .findByIdAndCategoryId(subcatId, catId)
          .orElseThrow(
              () -> RestException.badRequest(
                  "Selected Subcategory does not belong to Category ID: " + catId));
    }

    // FR-006: Support Impact, Urgency, and Dynamic Priority Matrix Calculation
    Impact impact = null;
    if (request.getImpactId() != null) {
      impact = impactRepository
          .findById(request.getImpactId())
          .orElseThrow(
              () -> RestException.notFound("Impact not found with ID: " + request.getImpactId()));
    }

    Urgency urgency = null;
    if (request.getUrgencyId() != null) {
      urgency = urgencyRepository
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

    Ticket ticket = Ticket.builder()
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

    ticket = ticketRepository.save(ticket);
    return toTicketResponse(ticket);
  }

  @Transactional(readOnly = true)
  public TicketResponse getTicketByNumber(String ticketNumber) {
    if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
      throw RestException.badRequest("Ticket number is required");
    }
    Ticket ticket = ticketRepository
        .findByTicketNumber(ticketNumber.trim())
        .orElseThrow(
            () -> RestException.notFound("Ticket not found with number: " + ticketNumber));
    return toTicketResponse(ticket);
  }

  @Transactional(readOnly = true)
  public Page<TicketResponse> getMyTickets(String reporterUsername, Pageable pageable) {
    User reporter = userRepository
        .findByUsername(reporterUsername)
        .orElseThrow(() -> RestException.notFound("User not found: " + reporterUsername));
    return ticketRepository
        .findByReporterId(reporter.getId(), pageable)
        .map(this::toTicketResponse);
  }

  /**
   * FR-007: Advanced ticket search, filtering across all dimensions, sorting, and
   * pagination.
   */
  @Transactional(readOnly = true)
  public Page<TicketResponse> searchTickets(TicketSearchFilter filter, Pageable pageable) {
    return ticketRepository
        .findAll(TicketSpecification.withFilter(filter), pageable)
        .map(this::toTicketResponse);
  }

  private void validateUserOrganizationAccess(User user, Organization organization) {
    if (user.getUserType() == UserType.EXTERNAL_CUSTOMER) {
      boolean matchesOrgName = user.getOrganizationName() != null
          && user.getOrganizationName().equalsIgnoreCase(organization.getName());

      boolean matchesOrgList = user.getOrganizations() != null
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
        .createdAt(ticket.getCreatedAt())
        .updatedAt(ticket.getUpdatedAt())
        .build();
  }
}
