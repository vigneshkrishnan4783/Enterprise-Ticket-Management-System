package com.enterprise.ticket.service.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Role;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.DashboardReportRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.response.dashboard.OperationalSummaryReportResponse;
import com.enterprise.ticket.response.dashboard.RoleDashboardResponse;
import com.enterprise.ticket.response.dashboard.SlaComplianceReportResponse;
import com.enterprise.ticket.response.dashboard.WorkloadReportResponse;
import com.enterprise.ticket.response.dashboard.WorkloadReportResponse.AgentWorkloadItem;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DashboardReportServiceTest {

  private DashboardReportService dashboardReportService;
  private List<User> userStore;

  @BeforeEach
  void setUp() {
    userStore = new ArrayList<>();

    Role adminRole = new Role();
    adminRole.setId(1L);
    adminRole.setName(RoleType.ROLE_ADMIN);

    Role agentRole = new Role();
    agentRole.setId(2L);
    agentRole.setName(RoleType.ROLE_AGENT);

    Role customerRole = new Role();
    customerRole.setId(3L);
    customerRole.setName(RoleType.ROLE_CUSTOMER);

    User admin =
        User.builder()
            .username("manoj_mgr")
            .userType(UserType.INTERNAL)
            .roles(Set.of(adminRole))
            .build();
    admin.setId(1L);

    User agent =
        User.builder()
            .username("harish_dev")
            .userType(UserType.INTERNAL)
            .roles(Set.of(agentRole))
            .build();
    agent.setId(2L);

    User customer =
        User.builder()
            .username("kerala_traffic_client")
            .userType(UserType.EXTERNAL_CUSTOMER)
            .organizationName("Kerala Traffic Police")
            .roles(Set.of(customerRole))
            .build();
    customer.setId(3L);

    userStore.add(admin);
    userStore.add(agent);
    userStore.add(customer);

    dashboardReportService =
        new DashboardReportService(new StubDashboardReportRepository(), createUserRepo(userStore));
  }

  @Test
  @DisplayName("getMyDashboard should return SYSTEM_WIDE metrics for ROLE_ADMIN")
  void testGetMyDashboardForAdmin() {
    RoleDashboardResponse response = dashboardReportService.getMyDashboard("manoj_mgr");

    assertEquals("ROLE_ADMIN", response.getRole());
    assertEquals("SYSTEM_WIDE", response.getScope());
    assertEquals(25L, response.getTotalTickets());
    assertEquals(7L, response.getOpenTickets());
    assertEquals(5L, response.getInProgressTickets());
    assertEquals(3L, response.getPendingTickets());
    assertEquals(6L, response.getResolvedTickets());
    assertEquals(3L, response.getClosedTickets());
    assertEquals(1L, response.getReopenedTickets());
    assertEquals(4L, response.getEscalatedTickets());
    assertEquals(2L, response.getSlaBreachedTickets());
    assertEquals(4.5, response.getAverageCsatRating());
  }

  @Test
  @DisplayName("getMyDashboard should return AGENT_AND_TEAM metrics for ROLE_AGENT")
  void testGetMyDashboardForAgent() {
    RoleDashboardResponse response = dashboardReportService.getMyDashboard("harish_dev");

    assertEquals("ROLE_AGENT", response.getRole());
    assertEquals("AGENT_AND_TEAM", response.getScope());
    assertEquals(8L, response.getTotalTickets());
    assertEquals(6L, response.getTeamQueueOpenTickets());
    assertEquals(4.8, response.getAverageCsatRating());
  }

  @Test
  @DisplayName("getMyDashboard should return CUSTOMER_ORGANIZATION metrics for customer")
  void testGetMyDashboardForCustomer() {
    RoleDashboardResponse response =
        dashboardReportService.getMyDashboard("kerala_traffic_client");

    assertEquals("ROLE_CUSTOMER", response.getRole());
    assertEquals("CUSTOMER_ORGANIZATION", response.getScope());
    assertEquals(4L, response.getTotalTickets());
    assertEquals(0L, response.getTeamQueueOpenTickets());
  }

  @Test
  @DisplayName("getSlaComplianceReport should compute compliance percentages accurately")
  void testGetSlaComplianceReportCalculatesPercentages() {
    Instant from = Instant.now().minus(30, ChronoUnit.DAYS);
    Instant to = Instant.now();

    SlaComplianceReportResponse report = dashboardReportService.getSlaComplianceReport(from, to);

    assertEquals(20L, report.getTotalTickets());
    assertEquals(18L, report.getResponseMetCount());
    assertEquals(2L, report.getResponseBreachedCount());
    assertEquals(90.0, report.getResponseCompliancePercent());
    assertEquals(15L, report.getResolutionMetCount());
    assertEquals(5L, report.getResolutionBreachedCount());
    assertEquals(75.0, report.getResolutionCompliancePercent());
    assertEquals(3L, report.getCurrentlyPausedCount());
  }

  @Test
  @DisplayName("getSlaComplianceReport should reject invalid date range")
  void testGetSlaComplianceReportRejectsInvalidDateRange() {
    Instant from = Instant.now();
    Instant to = from.minus(1, ChronoUnit.DAYS);

    RestException ex =
        assertThrows(
            RestException.class, () -> dashboardReportService.getSlaComplianceReport(from, to));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
  }

  @Test
  @DisplayName("getWorkloadReport and getOperationalSummaryReport should return aggregated metrics")
  void testGetWorkloadReportAndOperationalSummary() {
    WorkloadReportResponse workload = dashboardReportService.getWorkloadReport();
    assertEquals(3L, workload.getUnassignedTickets());
    assertEquals(1, workload.getAgents().size());
    assertEquals("harish_dev", workload.getAgents().getFirst().agentUsername());

    OperationalSummaryReportResponse summary =
        dashboardReportService.getOperationalSummaryReport(null, null);
    assertEquals(25L, summary.getTotalTickets());
    assertEquals(4L, summary.getTotalEscalations());
    assertEquals(4.5, summary.getAverageCsatRating());
    assertEquals(10L, summary.getTotalFeedbackCount());
  }

  private static UserRepository createUserRepo(List<User> store) {
    return (UserRepository)
        Proxy.newProxyInstance(
            UserRepository.class.getClassLoader(),
            new Class<?>[] {UserRepository.class},
            (proxy, method, args) -> {
              if ("findByUsername".equals(method.getName())) {
                String name = (String) args[0];
                return store.stream().filter(u -> name.equals(u.getUsername())).findFirst();
              }
              throw new UnsupportedOperationException(method.getName());
            });
  }

  private static class StubDashboardReportRepository implements DashboardReportRepository {

    @Override
    public DashboardAggregate getSystemAggregate() {
      return new DashboardAggregate(
          25L,
          4L,
          2L,
          4.5,
          10L,
          Map.of(
              "OPEN", 4L,
              "ASSIGNED", 3L,
              "IN_PROGRESS", 5L,
              "PENDING_CUSTOMER", 2L,
              "PENDING_INTERNAL", 1L,
              "RESOLVED", 6L,
              "CLOSED", 3L,
              "REOPENED", 1L),
          Map.of("HIGH", 10L, "MEDIUM", 15L));
    }

    @Override
    public DashboardAggregate getAgentAggregate(Long agentId) {
      return new DashboardAggregate(
          8L,
          1L,
          0L,
          4.8,
          5L,
          Map.of("ASSIGNED", 2L, "IN_PROGRESS", 3L, "RESOLVED", 3L),
          Map.of("HIGH", 5L, "MEDIUM", 3L));
    }

    @Override
    public long countTeamQueueOpenTickets(Long agentId) {
      return 6L;
    }

    @Override
    public DashboardAggregate getCustomerAggregate(Long customerId, String organizationName) {
      return new DashboardAggregate(
          4L,
          0L,
          0L,
          5.0,
          2L,
          Map.of("OPEN", 1L, "RESOLVED", 2L, "CLOSED", 1L),
          Map.of("HIGH", 4L));
    }

    @Override
    public DashboardAggregate getReporterAggregate(Long reporterId) {
      return new DashboardAggregate(2L, 0L, 0L, null, 0L, Map.of("OPEN", 2L), Map.of("LOW", 2L));
    }

    @Override
    public SlaAggregate getSlaAggregate(Instant from, Instant to) {
      return new SlaAggregate(20L, 2L, 5L, 3L);
    }

    @Override
    public long countUnassignedTickets() {
      return 3L;
    }

    @Override
    public List<AgentWorkloadItem> getAgentWorkload() {
      return List.of(new AgentWorkloadItem(2L, "harish_dev", 8L, 5L, 3L, 0L, 1L, 4.8));
    }

    @Override
    public SummaryAggregate getSummaryAggregate(Instant from, Instant to) {
      return new SummaryAggregate(
          25L,
          4L,
          4.5,
          10L,
          Map.of("OPEN", 4L, "RESOLVED", 6L),
          Map.of("HIGH", 10L, "MEDIUM", 15L),
          Map.of("ANPR Camera", 15L, "Network", 10L),
          Map.of(5, 6L, 4, 3L, 3, 1L));
    }
  }
}
