package com.enterprise.ticket.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom application REST exception carrying HTTP status and user-friendly error message.
 * Replaces generic IllegalArgumentException across service and domain layers.
 */
@Getter
public class RestException extends RuntimeException {

  private final HttpStatus status;

  public RestException(String message) {
    super(message);
    this.status = HttpStatus.BAD_REQUEST;
  }

  public RestException(HttpStatus status, String message) {
    super(message);
    this.status = status;
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
