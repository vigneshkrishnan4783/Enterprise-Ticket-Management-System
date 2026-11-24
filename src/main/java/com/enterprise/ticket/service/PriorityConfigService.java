package com.enterprise.ticket.service;

import com.enterprise.ticket.constant.TicketPriority;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Impact;
import com.enterprise.ticket.model.PriorityMatrix;
import com.enterprise.ticket.model.Urgency;
import com.enterprise.ticket.repository.ImpactRepository;
import com.enterprise.ticket.repository.PriorityMatrixRepository;
import com.enterprise.ticket.repository.UrgencyRepository;
import com.enterprise.ticket.request.ConfigurePriorityMatrixRequest;
import com.enterprise.ticket.request.CreateImpactRequest;
import com.enterprise.ticket.request.CreateUrgencyRequest;
import com.enterprise.ticket.response.ImpactResponse;
import com.enterprise.ticket.response.PriorityMatrixResponse;
import com.enterprise.ticket.response.UrgencyResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriorityConfigService {

  private final ImpactRepository impactRepository;
  private final UrgencyRepository urgencyRepository;
  private final PriorityMatrixRepository priorityMatrixRepository;

  @Transactional(readOnly = true)
  public List<ImpactResponse> getAllImpacts() {
    return impactRepository.findByIsActiveTrueOrderByLevelWeightDesc().stream()
        .map(this::toImpactResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public ImpactResponse createImpact(CreateImpactRequest request) {
    if (request == null) {
      throw RestException.badRequest("Create impact request cannot be empty");
    }
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw RestException.badRequest("Impact name is required");
    }
    if (request.getCode() == null || request.getCode().trim().isEmpty()) {
      throw RestException.badRequest("Impact code is required");
    }
    if (request.getLevelWeight() == null || request.getLevelWeight() < 1) {
      throw RestException.badRequest("Impact level weight is required and must be at least 1");
    }
    if (impactRepository.existsByCode(request.getCode().trim().toUpperCase())) {
      throw RestException.conflict("Impact code already exists: " + request.getCode());
    }
    if (impactRepository.existsByName(request.getName().trim())) {
      throw RestException.conflict("Impact name already exists: " + request.getName());
    }

    Impact impact =
        Impact.builder()
            .name(request.getName().trim())
            .code(request.getCode().trim().toUpperCase())
            .levelWeight(request.getLevelWeight())
            .description(request.getDescription() != null ? request.getDescription().trim() : null)
            .isActive(true)
            .build();

    return toImpactResponse(impactRepository.save(impact));
  }

  @Transactional(readOnly = true)
  public List<UrgencyResponse> getAllUrgencies() {
    return urgencyRepository.findByIsActiveTrueOrderByLevelWeightDesc().stream()
        .map(this::toUrgencyResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public UrgencyResponse createUrgency(CreateUrgencyRequest request) {
    if (request == null) {
      throw RestException.badRequest("Create urgency request cannot be empty");
    }
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw RestException.badRequest("Urgency name is required");
    }
    if (request.getCode() == null || request.getCode().trim().isEmpty()) {
      throw RestException.badRequest("Urgency code is required");
    }
    if (request.getLevelWeight() == null || request.getLevelWeight() < 1) {
      throw RestException.badRequest("Urgency level weight is required and must be at least 1");
    }
    if (urgencyRepository.existsByCode(request.getCode().trim().toUpperCase())) {
      throw RestException.conflict("Urgency code already exists: " + request.getCode());
    }
    if (urgencyRepository.existsByName(request.getName().trim())) {
      throw RestException.conflict("Urgency name already exists: " + request.getName());
    }

    Urgency urgency =
        Urgency.builder()
            .name(request.getName().trim())
            .code(request.getCode().trim().toUpperCase())
            .levelWeight(request.getLevelWeight())
            .description(request.getDescription() != null ? request.getDescription().trim() : null)
            .isActive(true)
            .build();

    return toUrgencyResponse(urgencyRepository.save(urgency));
  }

  @Transactional(readOnly = true)
  public List<PriorityMatrixResponse> getPriorityMatrix() {
    return priorityMatrixRepository.findByIsActiveTrue().stream()
        .map(this::toPriorityMatrixResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public PriorityMatrixResponse configureMatrix(ConfigurePriorityMatrixRequest request) {
    if (request == null) {
      throw RestException.badRequest("Matrix configuration payload cannot be empty");
    }
    if (request.getImpactId() == null) {
      throw RestException.badRequest("Impact ID is required");
    }
    if (request.getUrgencyId() == null) {
      throw RestException.badRequest("Urgency ID is required");
    }
    if (request.getPriority() == null) {
      throw RestException.badRequest("Priority is required (CRITICAL, HIGH, MEDIUM, LOW)");
    }

    Impact impact =
        impactRepository
            .findById(request.getImpactId())
            .orElseThrow(
                () -> RestException.notFound("Impact not found with ID: " + request.getImpactId()));

    Urgency urgency =
        urgencyRepository
            .findById(request.getUrgencyId())
            .orElseThrow(
                () -> RestException.notFound("Urgency not found with ID: " + request.getUrgencyId()));

    PriorityMatrix matrix =
        priorityMatrixRepository
            .findByImpactIdAndUrgencyId(impact.getId(), urgency.getId())
            .orElseGet(
                () ->
                    PriorityMatrix.builder()
                        .impact(impact)
                        .urgency(urgency)
                        .isActive(true)
                        .build());

    matrix.setPriority(request.getPriority());
    return toPriorityMatrixResponse(priorityMatrixRepository.save(matrix));
  }

  @Transactional(readOnly = true)
  public TicketPriority calculatePriority(Long impactId, Long urgencyId) {
    if (impactId == null || urgencyId == null) {
      return null;
    }
    return priorityMatrixRepository
        .findByImpactIdAndUrgencyId(impactId, urgencyId)
        .map(PriorityMatrix::getPriority)
        .orElse(TicketPriority.MEDIUM);
  }

  private ImpactResponse toImpactResponse(Impact impact) {
    return ImpactResponse.builder()
        .id(impact.getId())
        .name(impact.getName())
        .code(impact.getCode())
        .levelWeight(impact.getLevelWeight())
        .description(impact.getDescription())
        .isActive(impact.getIsActive())
        .createdAt(impact.getCreatedAt())
        .build();
  }

  private UrgencyResponse toUrgencyResponse(Urgency urgency) {
    return UrgencyResponse.builder()
        .id(urgency.getId())
        .name(urgency.getName())
        .code(urgency.getCode())
        .levelWeight(urgency.getLevelWeight())
        .description(urgency.getDescription())
        .isActive(urgency.getIsActive())
        .createdAt(urgency.getCreatedAt())
        .build();
  }

  private PriorityMatrixResponse toPriorityMatrixResponse(PriorityMatrix matrix) {
    return PriorityMatrixResponse.builder()
        .id(matrix.getId())
        .impactId(matrix.getImpact().getId())
        .impactName(matrix.getImpact().getName())
        .impactCode(matrix.getImpact().getCode())
        .urgencyId(matrix.getUrgency().getId())
        .urgencyName(matrix.getUrgency().getName())
        .urgencyCode(matrix.getUrgency().getCode())
        .priority(matrix.getPriority())
        .isActive(matrix.getIsActive())
        .createdAt(matrix.getCreatedAt())
        .build();
  }
}
