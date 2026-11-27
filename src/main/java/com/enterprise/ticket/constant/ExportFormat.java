package com.enterprise.ticket.constant;

public enum ExportFormat {
  CSV("text/csv", "csv"),
  XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
  PDF("application/pdf", "pdf");

  private final String contentType;
  private final String extension;

  ExportFormat(String contentType, String extension) {
    this.contentType = contentType;
    this.extension = extension;
  }

  public String getContentType() {
    return contentType;
  }

  public String getExtension() {
    return extension;
  }
}
