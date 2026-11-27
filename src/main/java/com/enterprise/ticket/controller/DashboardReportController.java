package com.enterprise.ticket.controller;

import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.response.dashboard.OperationalSummaryReportResponse;
import com.enterprise.ticket.response.dashboard.RoleDashboardResponse;
import com.enterprise.ticket.response.dashboard.SlaComplianceReportResponse;
import com.enterprise.ticket.response.dashboard.WorkloadReportResponse;
import com.enterprise.ticket.service.dashboard.DashboardReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(
    name = "Dashboards & Reports",
    description = "Role-specific dashboards and operational SLA, workload, and CSAT reports")
public class DashboardReportController {

  private final DashboardReportService dashboardReportService;

  @GetMapping("/dashboards/admin")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get admin dashboard",
      description = "Returns system-wide KPIs and ticket breakdowns for managers/admins")
  public ResponseEntity<ApiResponse<RoleDashboardResponse>> getAdminDashboard(Principal principal) {
    return ResponseBuilder.<RoleDashboardResponse>builder()
        .success()
        .withData(dashboardReportService.getAdminDashboard(principal.getName()))
        .build();
  }

  @GetMapping("/dashboards/agent")
  @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
  @Operation(
      summary = "Get agent/team lead dashboard",
      description = "Returns assigned ticket KPIs and team queue count for agents and team leads")
  public ResponseEntity<ApiResponse<RoleDashboardResponse>> getAgentDashboard(Principal principal) {
    return ResponseBuilder.<RoleDashboardResponse>builder()
        .success()
        .withData(dashboardReportService.getAgentDashboard(principal.getName()))
        .build();
  }

  @GetMapping("/dashboards/customer")
  @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
  @Operation(
      summary = "Get customer dashboard",
      description = "Returns organization-scoped ticket KPIs and CSAT for external customers")
  public ResponseEntity<ApiResponse<RoleDashboardResponse>> getCustomerDashboard(
      Principal principal) {
    return ResponseBuilder.<RoleDashboardResponse>builder()
        .success()
        .withData(dashboardReportService.getCustomerDashboard(principal.getName()))
        .build();
  }

  @GetMapping("/dashboards/employee")
  @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
  @Operation(
      summary = "Get employee dashboard",
      description = "Returns own submitted ticket KPIs for internal employees")
  public ResponseEntity<ApiResponse<RoleDashboardResponse>> getEmployeeDashboard(
      Principal principal) {
    return ResponseBuilder.<RoleDashboardResponse>builder()
        .success()
        .withData(dashboardReportService.getEmployeeDashboard(principal.getName()))
        .build();
  }

  @GetMapping("/reports/sla-compliance")
  @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
  @Operation(
      summary = "Get SLA compliance report",
      description =
          "Returns response and resolution SLA compliance percentages and breach counts for optional date range")
  public ResponseEntity<ApiResponse<SlaComplianceReportResponse>> getSlaComplianceReport(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return ResponseBuilder.<SlaComplianceReportResponse>builder()
        .success()
        .withData(dashboardReportService.getSlaComplianceReport(from, to))
        .build();
  }

  @GetMapping("/reports/workload")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get agent workload report",
      description =
          "Returns per-agent assigned, active, resolved, breached, escalated counts, CSAT rating, and unassigned queue count")
  public ResponseEntity<ApiResponse<WorkloadReportResponse>> getWorkloadReport() {
    return ResponseBuilder.<WorkloadReportResponse>builder()
        .success()
        .withData(dashboardReportService.getWorkloadReport())
        .build();
  }

  @GetMapping("/reports/summary")
  @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
  @Operation(
      summary = "Get operational summary report",
      description =
          "Returns ticket distribution by status, priority, category, escalation count, and CSAT rating breakdown")
  public ResponseEntity<ApiResponse<OperationalSummaryReportResponse>> getOperationalSummaryReport(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return ResponseBuilder.<OperationalSummaryReportResponse>builder()
        .success()
        .withData(dashboardReportService.getOperationalSummaryReport(from, to))
        .build();
  }
}
