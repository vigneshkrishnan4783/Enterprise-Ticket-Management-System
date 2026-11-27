package com.enterprise.ticket.service.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enterprise.ticket.constant.ExportFormat;
import com.enterprise.ticket.constant.ReportType;
import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.constant.UserType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.Role;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.DashboardReportRepository;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.response.dashboard.WorkloadReportResponse.AgentWorkloadItem;
import com.enterprise.ticket.service.dashboard.ReportExportService.ExportedReport;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ReportExportServiceTest {

  private ReportExportService reportExportService;

  @BeforeEach
  void setUp() {
    List<User> userStore = new ArrayList<>();

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
            .roles(Set.of(customerRole))
            .build();
    customer.setId(3L);

    userStore.add(admin);
    userStore.add(agent);
    userStore.add(customer);

    UserRepository userRepository = createUserRepo(userStore);
    DashboardReportService dashboardReportService =
        new DashboardReportService(new StubDashboardReportRepository(), userRepository);
    reportExportService = new ReportExportService(dashboardReportService, userRepository);
  }

  @Test
  @DisplayName("exportReport should export SLA_COMPLIANCE to valid CSV")
  void testExportSlaComplianceToCsv() {
    ExportedReport exported =
        reportExportService.exportReport(
            ReportType.SLA_COMPLIANCE, ExportFormat.CSV, null, null, "harish_dev");

    assertTrue(exported.fileName().startsWith("sla_compliance_"));
    assertTrue(exported.fileName().endsWith(".csv"));
    String csv = new String(exported.content(), StandardCharsets.UTF_8);
    assertTrue(csv.contains("Metric,Value"));
    assertTrue(csv.contains("Total Tickets,20"));
    assertTrue(csv.contains("Response Compliance (%),90.0"));
  }

  @Test
  @DisplayName("exportReport should export WORKLOAD to valid XLSX for ROLE_ADMIN")
  void testExportWorkloadToXlsxAsAdmin() throws Exception {
    ExportedReport exported =
        reportExportService.exportReport(
            ReportType.WORKLOAD, ExportFormat.XLSX, null, null, "manoj_mgr");

    assertTrue(exported.fileName().endsWith(".xlsx"));
    assertTrue(exported.content().length > 0);

    try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(exported.content()))) {
      assertEquals("Agent Username", wb.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
      assertEquals("harish_dev", wb.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
    }
  }

  @Test
  @DisplayName("exportReport should export SUMMARY to valid PDF for ROLE_AGENT")
  void testExportSummaryToPdfAsAgent() {
    ExportedReport exported =
        reportExportService.exportReport(
            ReportType.SUMMARY, ExportFormat.PDF, null, null, "harish_dev");

    assertTrue(exported.fileName().endsWith(".pdf"));
    String header = new String(exported.content(), 0, 5, StandardCharsets.US_ASCII);
    assertEquals("%PDF-", header);
  }

  @Test
  @DisplayName("exportReport should forbid ROLE_AGENT from exporting WORKLOAD report")
  void testExportWorkloadRejectedForAgent() {
    RestException ex =
        assertThrows(
            RestException.class,
            () ->
                reportExportService.exportReport(
                    ReportType.WORKLOAD, ExportFormat.CSV, null, null, "harish_dev"));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
  }

  @Test
  @DisplayName("exportReport should forbid ROLE_CUSTOMER from exporting any report")
  void testExportRejectedForCustomer() {
    RestException ex =
        assertThrows(
            RestException.class,
            () ->
                reportExportService.exportReport(
                    ReportType.SLA_COMPLIANCE,
                    ExportFormat.PDF,
                    null,
                    null,
                    "kerala_traffic_client"));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
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
      return new DashboardAggregate(0L, 0L, 0L, null, 0L, Map.of(), Map.of());
    }

    @Override
    public DashboardAggregate getAgentAggregate(Long agentId) {
      return new DashboardAggregate(0L, 0L, 0L, null, 0L, Map.of(), Map.of());
    }

    @Override
    public long countTeamQueueOpenTickets(Long agentId) {
      return 0L;
    }

    @Override
    public DashboardAggregate getCustomerAggregate(Long customerId, String organizationName) {
      return new DashboardAggregate(0L, 0L, 0L, null, 0L, Map.of(), Map.of());
    }

    @Override
    public DashboardAggregate getReporterAggregate(Long reporterId) {
      return new DashboardAggregate(0L, 0L, 0L, null, 0L, Map.of(), Map.of());
    }

    @Override
    public SlaAggregate getSlaAggregate(Instant from, Instant to) {
      return new SlaAggregate(20L, 2L, 5L, 3L);
    }

    @Override
    public long countUnassignedTickets() {
      return 2L;
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
          Map.of("HIGH", 10L),
          Map.of("ANPR Camera", 15L),
          Map.of(5, 6L, 4, 4L));
    }
  }
}
