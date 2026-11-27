package com.enterprise.ticket.controller;

import com.enterprise.ticket.constant.ExportFormat;
import com.enterprise.ticket.constant.ReportType;
import com.enterprise.ticket.service.dashboard.ReportExportService;
import com.enterprise.ticket.service.dashboard.ReportExportService.ExportedReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(
    name = "Report Export",
    description = "Export authorized operational reports in CSV, XLSX, or PDF format")
public class ReportExportController {

  private final ReportExportService reportExportService;

  @GetMapping("/export")
  @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
  @Operation(
      summary = "Export operational report",
      description =
          "Exports SLA_COMPLIANCE, WORKLOAD (Admin only), or SUMMARY report to CSV, XLSX, or PDF")
  public ResponseEntity<byte[]> exportReport(
      @RequestParam ReportType reportType,
      @RequestParam ExportFormat format,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      Principal principal) {
    ExportedReport exported =
        reportExportService.exportReport(reportType, format, from, to, principal.getName());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + exported.fileName() + "\"")
        .contentType(MediaType.parseMediaType(exported.format().getContentType()))
        .contentLength(exported.content().length)
        .body(exported.content());
  }
}
