package com.enterprise.ticket.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RestException extends RuntimeException {

  private final HttpStatus status;

  public RestException(String message) {
    String sanitized = (message != null && !message.isBlank()) ? message.trim() : "Operation failed";
    super(sanitized);
    this.status = HttpStatus.BAD_REQUEST;
  }

  public RestException(HttpStatus status, String message) {
    HttpStatus resolvedStatus = (status != null) ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    String sanitized = (message != null && !message.isBlank()) ? message.trim() : resolvedStatus.getReasonPhrase();
    super(sanitized);
    this.status = resolvedStatus;
  }

  public static RestException badRequest(String message) {
    return new RestException(HttpStatus.BAD_REQUEST, message);
  }

  public static RestException notFound(String message) {
    return new RestException(HttpStatus.NOT_FOUND, message);
  }

  public static RestException unauthorized(String message) {
    return new RestException(HttpStatus.UNAUTHORIZED, message);
  }

  public static RestException forbidden(String message) {
    return new RestException(HttpStatus.FORBIDDEN, message);
  }

  public static RestException conflict(String message) {
    return new RestException(HttpStatus.CONFLICT, message);
  }
}
