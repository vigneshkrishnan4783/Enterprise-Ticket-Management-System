package com.enterprise.ticket.specification;

import com.enterprise.ticket.model.Ticket;
import com.enterprise.ticket.request.TicketSearchFilter;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecification {

  private TicketSpecification() {}

  public static Specification<Ticket> withFilter(TicketSearchFilter filter) {
    return (root, query, cb) -> {
      if (filter == null) {
        return cb.conjunction();
      }

      List<Predicate> predicates = new ArrayList<>();
      if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
        String pattern = "%" + filter.getKeyword().trim().toLowerCase() + "%";
        Predicate ticketNumMatch = cb.like(cb.lower(root.get("ticketNumber")), pattern);
        Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
        Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
        predicates.add(cb.or(ticketNumMatch, titleMatch, descMatch));
      }

      if (filter.getStatus() != null) {
        predicates.add(cb.equal(root.get("status"), filter.getStatus()));
      }

      if (filter.getPriority() != null) {
        predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
      }

      if (filter.getChannel() != null) {
        predicates.add(cb.equal(root.get("channel"), filter.getChannel()));
      }

      if (filter.getOrganizationId() != null) {
        predicates.add(cb.equal(root.get("organization").get("id"), filter.getOrganizationId()));
      }

      if (filter.getSiteId() != null) {
        predicates.add(cb.equal(root.get("site").get("id"), filter.getSiteId()));
      }

      if (filter.getAssetId() != null) {
        predicates.add(cb.equal(root.get("asset").get("id"), filter.getAssetId()));
      }

      if (filter.getCategoryId() != null) {
        predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
      }

      if (filter.getSubcategoryId() != null) {
        predicates.add(cb.equal(root.get("subcategory").get("id"), filter.getSubcategoryId()));
      }

      if (filter.getImpactId() != null) {
        predicates.add(cb.equal(root.get("impact").get("id"), filter.getImpactId()));
      }

      if (filter.getUrgencyId() != null) {
        predicates.add(cb.equal(root.get("urgency").get("id"), filter.getUrgencyId()));
      }

      if (filter.getReporterId() != null) {
        predicates.add(cb.equal(root.get("reporter").get("id"), filter.getReporterId()));
      }

      if (filter.getCreatedAfter() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedAfter()));
      }

      if (filter.getCreatedBefore() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedBefore()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
