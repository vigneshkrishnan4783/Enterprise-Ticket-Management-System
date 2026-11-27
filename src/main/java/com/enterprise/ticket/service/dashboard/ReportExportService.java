package com.enterprise.ticket.service.dashboard;

import com.enterprise.ticket.constant.ExportFormat;
import com.enterprise.ticket.constant.ReportType;
import com.enterprise.ticket.constant.RoleType;
import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.model.User;
import com.enterprise.ticket.repository.UserRepository;
import com.enterprise.ticket.response.dashboard.OperationalSummaryReportResponse;
import com.enterprise.ticket.response.dashboard.SlaComplianceReportResponse;
import com.enterprise.ticket.response.dashboard.WorkloadReportResponse;
import com.enterprise.ticket.response.dashboard.WorkloadReportResponse.AgentWorkloadItem;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportExportService {

  private static final DateTimeFormatter FILE_TS_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

  private final DashboardReportService dashboardReportService;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public ExportedReport exportReport(
      ReportType reportType, ExportFormat format, Instant from, Instant to, String username) {
    if (reportType == null || format == null) {
      throw RestException.badRequest("Report type and export format are required");
    }

    User caller =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> RestException.notFound("User not found: " + username));

    enforceReportAuthorization(reportType, caller);

    ReportTable table = buildReportTable(reportType, from, to);
    byte[] content =
        switch (format) {
          case CSV -> renderCsv(table);
          case XLSX -> renderXlsx(table);
          case PDF -> renderPdf(table);
        };

    String fileName =
        reportType.name().toLowerCase()
            + "_"
            + FILE_TS_FORMATTER.format(Instant.now())
            + "."
            + format.getExtension();

    return new ExportedReport(content, fileName, format);
  }

  private void enforceReportAuthorization(ReportType reportType, User caller) {
    boolean isAdmin = hasRole(caller, RoleType.ROLE_ADMIN);
    boolean isAgent = hasRole(caller, RoleType.ROLE_AGENT);

    if (reportType == ReportType.WORKLOAD) {
      if (!isAdmin) {
        throw RestException.forbidden("Only administrators can export workload reports");
      }
      return;
    }

    if (!isAdmin && !isAgent) {
      throw RestException.forbidden("You are not authorized to export operational reports");
    }
  }

  private boolean hasRole(User user, RoleType roleType) {
    return user.getRoles() != null
        && user.getRoles().stream().anyMatch(r -> r.getName() == roleType);
  }

  private ReportTable buildReportTable(ReportType reportType, Instant from, Instant to) {
    return switch (reportType) {
      case SLA_COMPLIANCE -> buildSlaTable(dashboardReportService.getSlaComplianceReport(from, to));
      case WORKLOAD -> buildWorkloadTable(dashboardReportService.getWorkloadReport());
      case SUMMARY ->
          buildSummaryTable(dashboardReportService.getOperationalSummaryReport(from, to));
    };
  }

  private ReportTable buildSlaTable(SlaComplianceReportResponse report) {
    List<String> headers = List.of("Metric", "Value");
    List<List<String>> rows = new ArrayList<>();
    rows.add(List.of("From", report.getFrom() != null ? report.getFrom().toString() : "All Time"));
    rows.add(List.of("To", report.getTo() != null ? report.getTo().toString() : "Present"));
    rows.add(List.of("Total Tickets", String.valueOf(report.getTotalTickets())));
    rows.add(List.of("Response SLA Met", String.valueOf(report.getResponseMetCount())));
    rows.add(List.of("Response SLA Breached", String.valueOf(report.getResponseBreachedCount())));
    rows.add(
        List.of(
            "Response Compliance (%)", String.valueOf(report.getResponseCompliancePercent())));
    rows.add(List.of("Resolution SLA Met", String.valueOf(report.getResolutionMetCount())));
    rows.add(
        List.of("Resolution SLA Breached", String.valueOf(report.getResolutionBreachedCount())));
    rows.add(
        List.of(
            "Resolution Compliance (%)", String.valueOf(report.getResolutionCompliancePercent())));
    rows.add(List.of("Currently Paused SLA", String.valueOf(report.getCurrentlyPausedCount())));
    return new ReportTable("SLA Compliance Report", headers, rows);
  }

  private ReportTable buildWorkloadTable(WorkloadReportResponse report) {
    List<String> headers =
        List.of(
            "Agent ID",
            "Agent Username",
            "Total Assigned",
            "Active",
            "Resolved/Closed",
            "SLA Breached",
            "Escalated",
            "Avg CSAT");
    List<List<String>> rows = new ArrayList<>();
    if (report.getAgents() != null) {
      for (AgentWorkloadItem item : report.getAgents()) {
        rows.add(
            List.of(
                String.valueOf(item.agentId()),
                safeText(item.agentUsername()),
                String.valueOf(item.totalAssigned()),
                String.valueOf(item.activeCount()),
                String.valueOf(item.resolvedOrClosedCount()),
                String.valueOf(item.slaBreachedCount()),
                String.valueOf(item.escalatedCount()),
                item.averageCsatRating() != null ? String.valueOf(item.averageCsatRating()) : "N/A"));
      }
    }
    rows.add(
        List.of(
            "-",
            "UNASSIGNED_QUEUE",
            String.valueOf(report.getUnassignedTickets()),
            "-",
            "-",
            "-",
            "-",
            "-"));
    return new ReportTable("Agent Workload Report", headers, rows);
  }

  private ReportTable buildSummaryTable(OperationalSummaryReportResponse report) {
    List<String> headers = List.of("Section", "Key", "Count / Value");
    List<List<String>> rows = new ArrayList<>();
    rows.add(List.of("Overview", "Total Tickets", String.valueOf(report.getTotalTickets())));
    rows.add(List.of("Overview", "Total Escalations", String.valueOf(report.getTotalEscalations())));
    rows.add(
        List.of(
            "Overview",
            "Average CSAT Rating",
            report.getAverageCsatRating() != null
                ? String.valueOf(report.getAverageCsatRating())
                : "N/A"));
    rows.add(
        List.of("Overview", "Total Feedback Count", String.valueOf(report.getTotalFeedbackCount())));
    appendMapRows(rows, "By Status", report.getTicketsByStatus());
    appendMapRows(rows, "By Priority", report.getTicketsByPriority());
    appendMapRows(rows, "By Category", report.getTicketsByCategory());
    if (report.getCsatRatingDistribution() != null) {
      for (Map.Entry<Integer, Long> entry : report.getCsatRatingDistribution().entrySet()) {
        rows.add(
            List.of(
                "CSAT Distribution", entry.getKey() + " Star", String.valueOf(entry.getValue())));
      }
    }
    return new ReportTable("Operational Summary Report", headers, rows);
  }

  private void appendMapRows(
      List<List<String>> rows, String section, Map<String, Long> map) {
    if (map == null) {
      return;
    }
    for (Map.Entry<String, Long> entry : map.entrySet()) {
      rows.add(List.of(section, safeText(entry.getKey()), String.valueOf(entry.getValue())));
    }
  }

  private byte[] renderCsv(ReportTable table) {
    StringBuilder sb = new StringBuilder();
    appendCsvRow(sb, table.headers());
    for (List<String> row : table.rows()) {
      appendCsvRow(sb, row);
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private void appendCsvRow(StringBuilder sb, List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(escapeCsvCell(values.get(i)));
    }
    sb.append("\r\n");
  }

  private String escapeCsvCell(String raw) {
    String value = safeText(raw);
    if (!value.isEmpty()) {
      char first = value.charAt(0);
      if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t'
          || first == '\r') {
        value = "'" + value;
      }
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private byte[] renderXlsx(ReportTable table) {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Report");

      CellStyle headerStyle = workbook.createCellStyle();
      org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);

      Row headerRow = sheet.createRow(0);
      for (int c = 0; c < table.headers().size(); c++) {
        Cell cell = headerRow.createCell(c);
        cell.setCellValue(table.headers().get(c));
        cell.setCellStyle(headerStyle);
      }

      for (int r = 0; r < table.rows().size(); r++) {
        Row row = sheet.createRow(r + 1);
        List<String> values = table.rows().get(r);
        for (int c = 0; c < values.size(); c++) {
          row.createCell(c).setCellValue(safeText(values.get(c)));
        }
      }

      for (int c = 0; c < table.headers().size(); c++) {
        sheet.autoSizeColumn(c);
      }

      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw RestException.badRequest("Failed to generate XLSX report: " + e.getMessage());
    }
  }

  private byte[] renderPdf(ReportTable table) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
      PdfWriter.getInstance(document, out);
      document.open();

      Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, new Color(30, 58, 138));
      Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
      Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
      Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

      Paragraph title = new Paragraph(table.title(), titleFont);
      title.setSpacingAfter(4f);
      document.add(title);

      Paragraph generatedAt =
          new Paragraph("Generated (UTC): " + Instant.now(), metaFont);
      generatedAt.setSpacingAfter(12f);
      document.add(generatedAt);

      PdfPTable pdfTable = new PdfPTable(table.headers().size());
      pdfTable.setWidthPercentage(100f);

      for (String header : table.headers()) {
        PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
        cell.setBackgroundColor(new Color(30, 58, 138));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        pdfTable.addCell(cell);
      }

      for (List<String> row : table.rows()) {
        for (String val : row) {
          PdfPCell cell = new PdfPCell(new Phrase(safeText(val), bodyFont));
          cell.setPadding(5f);
          pdfTable.addCell(cell);
        }
      }

      document.add(pdfTable);
      document.close();
      return out.toByteArray();
    } catch (DocumentException | IOException e) {
      throw RestException.badRequest("Failed to generate PDF report: " + e.getMessage());
    }
  }

  private String safeText(String value) {
    return value != null ? value : "";
  }

  private record ReportTable(String title, List<String> headers, List<List<String>> rows) {}

  public record ExportedReport(byte[] content, String fileName, ExportFormat format) {}
}
