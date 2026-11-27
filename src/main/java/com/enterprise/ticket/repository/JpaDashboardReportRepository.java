package com.enterprise.ticket.repository;

import com.enterprise.ticket.constant.TicketStatus;
import com.enterprise.ticket.response.dashboard.WorkloadReportResponse.AgentWorkloadItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDashboardReportRepository implements DashboardReportRepository {

  @PersistenceContext private EntityManager em;

  @Override
  public DashboardAggregate getSystemAggregate() {
    return buildDashboardAggregate("1=1", Map.of());
  }

  @Override
  public DashboardAggregate getAgentAggregate(Long agentId) {
    return buildDashboardAggregate("t.assignedAgent.id = :agentId", Map.of("agentId", agentId));
  }

  @Override
  public long countTeamQueueOpenTickets(Long agentId) {
    Long count =
        em.createQuery(
                "SELECT COUNT(DISTINCT t.id) FROM Ticket t JOIN t.assignedTeam tm JOIN tm.members m "
                    + "WHERE m.id = :agentId AND t.status IN (:openStatuses)",
                Long.class)
            .setParameter("agentId", agentId)
            .setParameter(
                "openStatuses",
                List.of(TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS))
            .getSingleResult();
    return count != null ? count : 0L;
  }

  @Override
  public DashboardAggregate getCustomerAggregate(Long customerId, String organizationName) {
    if (organizationName != null && !organizationName.isBlank()) {
      return buildDashboardAggregate(
          "(t.reporter.id = :customerId OR LOWER(t.organization.name) = LOWER(:orgName))",
          Map.of("customerId", customerId, "orgName", organizationName.trim()));
    }
    return buildDashboardAggregate("t.reporter.id = :customerId", Map.of("customerId", customerId));
  }

  @Override
  public DashboardAggregate getReporterAggregate(Long reporterId) {
    return buildDashboardAggregate("t.reporter.id = :reporterId", Map.of("reporterId", reporterId));
  }

  @Override
  public SlaAggregate getSlaAggregate(Instant from, Instant to) {
    StringBuilder where = new StringBuilder("1=1");
    Map<String, Object> params = new LinkedHashMap<>();
    appendDateRange(where, params, "t.createdAt", from, to);

    TypedQuery<Object[]> query =
        em.createQuery(
            "SELECT COUNT(t.id), "
                + "COALESCE(SUM(CASE WHEN t.isResponseBreached = true THEN 1L ELSE 0L END), 0L), "
                + "COALESCE(SUM(CASE WHEN t.isResolutionBreached = true THEN 1L ELSE 0L END), 0L), "
                + "COALESCE(SUM(CASE WHEN t.isSlaPaused = true THEN 1L ELSE 0L END), 0L) "
                + "FROM Ticket t WHERE "
                + where,
            Object[].class);
    applyParams(query, params);
    Object[] row = query.getSingleResult();
    return new SlaAggregate(
        toLong(row[0]), toLong(row[1]), toLong(row[2]), toLong(row[3]));
  }

  @Override
  public long countUnassignedTickets() {
    Long count =
        em.createQuery(
                "SELECT COUNT(t.id) FROM Ticket t WHERE t.assignedAgent IS NULL "
                    + "AND t.status NOT IN (:terminalStatuses)",
                Long.class)
            .setParameter(
                "terminalStatuses",
                List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED, TicketStatus.CANCELLED))
            .getSingleResult();
    return count != null ? count : 0L;
  }

  @Override
  public List<AgentWorkloadItem> getAgentWorkload() {
    List<Object[]> rows =
        em.createQuery(
                "SELECT t.assignedAgent.id, t.assignedAgent.username, COUNT(t.id), "
                    + "COALESCE(SUM(CASE WHEN t.status IN (:activeStatuses) THEN 1L ELSE 0L END), 0L), "
                    + "COALESCE(SUM(CASE WHEN t.status IN (:doneStatuses) THEN 1L ELSE 0L END), 0L), "
                    + "COALESCE(SUM(CASE WHEN (t.isResponseBreached = true OR t.isResolutionBreached = true) "
                    + "THEN 1L ELSE 0L END), 0L), "
                    + "COALESCE(SUM(CASE WHEN t.isEscalated = true THEN 1L ELSE 0L END), 0L) "
                    + "FROM Ticket t WHERE t.assignedAgent IS NOT NULL "
                    + "GROUP BY t.assignedAgent.id, t.assignedAgent.username "
                    + "ORDER BY COUNT(t.id) DESC",
                Object[].class)
            .setParameter(
                "activeStatuses",
                List.of(
                    TicketStatus.OPEN,
                    TicketStatus.ASSIGNED,
                    TicketStatus.IN_PROGRESS,
                    TicketStatus.REOPENED))
            .setParameter("doneStatuses", List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED))
            .getResultList();

    Map<Long, Double> csatByAgent = new LinkedHashMap<>();
    List<Object[]> csatRows =
        em.createQuery(
                "SELECT f.ticket.assignedAgent.id, AVG(f.rating) FROM TicketSatisfactionFeedback f "
                    + "WHERE f.ticket.assignedAgent IS NOT NULL GROUP BY f.ticket.assignedAgent.id",
                Object[].class)
            .getResultList();
    for (Object[] r : csatRows) {
      csatByAgent.put(toLong(r[0]), roundTwoDecimals((Double) r[1]));
    }

    List<AgentWorkloadItem> items = new ArrayList<>(rows.size());
    for (Object[] row : rows) {
      Long agentId = toLong(row[0]);
      items.add(
          new AgentWorkloadItem(
              agentId,
              (String) row[1],
              toLong(row[2]),
              toLong(row[3]),
              toLong(row[4]),
              toLong(row[5]),
              toLong(row[6]),
              csatByAgent.get(agentId)));
    }
    return items;
  }

  @Override
  public SummaryAggregate getSummaryAggregate(Instant from, Instant to) {
    StringBuilder ticketWhere = new StringBuilder("1=1");
    Map<String, Object> ticketParams = new LinkedHashMap<>();
    appendDateRange(ticketWhere, ticketParams, "t.createdAt", from, to);

    TypedQuery<Long> totalQuery =
        em.createQuery("SELECT COUNT(t.id) FROM Ticket t WHERE " + ticketWhere, Long.class);
    applyParams(totalQuery, ticketParams);
    long totalTickets = safeLong(totalQuery.getSingleResult());

    Map<String, Long> byStatus = queryGroupedCounts("t.status", ticketWhere.toString(), ticketParams);
    Map<String, Long> byPriority =
        queryGroupedCounts("t.priority", ticketWhere.toString(), ticketParams);

    TypedQuery<Object[]> catQuery =
        em.createQuery(
            "SELECT COALESCE(c.name, 'Uncategorized'), COUNT(t.id) "
                + "FROM Ticket t LEFT JOIN t.category c WHERE "
                + ticketWhere
                + " GROUP BY c.name",
            Object[].class);
    applyParams(catQuery, ticketParams);
    Map<String, Long> byCategory = new LinkedHashMap<>();
    for (Object[] row : catQuery.getResultList()) {
      byCategory.put(String.valueOf(row[0]), toLong(row[1]));
    }

    StringBuilder escWhere = new StringBuilder("1=1");
    Map<String, Object> escParams = new LinkedHashMap<>();
    appendDateRange(escWhere, escParams, "e.createdAt", from, to);
    TypedQuery<Long> escQuery =
        em.createQuery(
            "SELECT COUNT(e.id) FROM TicketEscalationHistory e WHERE " + escWhere, Long.class);
    applyParams(escQuery, escParams);
    long totalEscalations = safeLong(escQuery.getSingleResult());

    StringBuilder fbWhere = new StringBuilder("1=1");
    Map<String, Object> fbParams = new LinkedHashMap<>();
    appendDateRange(fbWhere, fbParams, "f.submittedAt", from, to);
    TypedQuery<Object[]> fbQuery =
        em.createQuery(
            "SELECT AVG(f.rating), COUNT(f.id) FROM TicketSatisfactionFeedback f WHERE " + fbWhere,
            Object[].class);
    applyParams(fbQuery, fbParams);
    Object[] fbRow = fbQuery.getSingleResult();
    Double avgCsat = fbRow[0] != null ? roundTwoDecimals(((Number) fbRow[0]).doubleValue()) : null;
    long feedbackCount = toLong(fbRow[1]);

    TypedQuery<Object[]> distQuery =
        em.createQuery(
            "SELECT f.rating, COUNT(f.id) FROM TicketSatisfactionFeedback f WHERE "
                + fbWhere
                + " GROUP BY f.rating ORDER BY f.rating ASC",
            Object[].class);
    applyParams(distQuery, fbParams);
    Map<Integer, Long> ratingDist = new LinkedHashMap<>();
    for (Object[] row : distQuery.getResultList()) {
      ratingDist.put(((Number) row[0]).intValue(), toLong(row[1]));
    }

    return new SummaryAggregate(
        totalTickets,
        totalEscalations,
        avgCsat,
        feedbackCount,
        byStatus,
        byPriority,
        byCategory,
        ratingDist);
  }

  private DashboardAggregate buildDashboardAggregate(String whereClause, Map<String, Object> params) {
    TypedQuery<Object[]> summaryQuery =
        em.createQuery(
            "SELECT COUNT(t.id), "
                + "COALESCE(SUM(CASE WHEN t.isEscalated = true THEN 1L ELSE 0L END), 0L), "
                + "COALESCE(SUM(CASE WHEN (t.isResponseBreached = true OR t.isResolutionBreached = true) "
                + "THEN 1L ELSE 0L END), 0L) "
                + "FROM Ticket t WHERE "
                + whereClause,
            Object[].class);
    applyParams(summaryQuery, params);
    Object[] row = summaryQuery.getSingleResult();

    Map<String, Long> byStatus = queryGroupedCounts("t.status", whereClause, params);
    Map<String, Long> byPriority = queryGroupedCounts("t.priority", whereClause, params);

    String feedbackWhere = whereClause.replace("t.", "f.ticket.");
    TypedQuery<Object[]> fbQuery =
        em.createQuery(
            "SELECT AVG(f.rating), COUNT(f.id) FROM TicketSatisfactionFeedback f WHERE "
                + feedbackWhere,
            Object[].class);
    applyParams(fbQuery, params);
    Object[] fbRow = fbQuery.getSingleResult();
    Double avgCsat = fbRow[0] != null ? roundTwoDecimals(((Number) fbRow[0]).doubleValue()) : null;
    long feedbackCount = toLong(fbRow[1]);

    return new DashboardAggregate(
        toLong(row[0]),
        toLong(row[1]),
        toLong(row[2]),
        avgCsat,
        feedbackCount,
        byStatus,
        byPriority);
  }

  private Map<String, Long> queryGroupedCounts(
      String fieldExpr, String whereClause, Map<String, Object> params) {
    TypedQuery<Object[]> query =
        em.createQuery(
            "SELECT "
                + fieldExpr
                + ", COUNT(t.id) FROM Ticket t WHERE "
                + whereClause
                + " GROUP BY "
                + fieldExpr,
            Object[].class);
    applyParams(query, params);
    Map<String, Long> result = new LinkedHashMap<>();
    for (Object[] row : query.getResultList()) {
      result.put(String.valueOf(row[0]), toLong(row[1]));
    }
    return result;
  }

  private void appendDateRange(
      StringBuilder where, Map<String, Object> params, String field, Instant from, Instant to) {
    if (from != null) {
      where.append(" AND ").append(field).append(" >= :fromDate");
      params.put("fromDate", from);
    }
    if (to != null) {
      where.append(" AND ").append(field).append(" <= :toDate");
      params.put("toDate", to);
    }
  }

  private void applyParams(TypedQuery<?> query, Map<String, Object> params) {
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      query.setParameter(entry.getKey(), entry.getValue());
    }
  }

  private long safeLong(Long value) {
    return value != null ? value : 0L;
  }

  private long toLong(Object value) {
    return value != null ? ((Number) value).longValue() : 0L;
  }

  private Double roundTwoDecimals(Double value) {
    if (value == null) {
      return null;
    }
    return Math.round(value * 100.0) / 100.0;
  }
}
