package com.enterprise.ticket.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility builder for standardizing ResponseEntity<ApiResponse<T>> construction in controllers.
 */
public final class ResponseBuilder {

  private ResponseBuilder() {}

  public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
    return ResponseEntity.ok(ApiResponse.success(data));
  }

  public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
    return ResponseEntity.ok(ApiResponse.success(message, data));
  }

  public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
  }

  public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message, data));
  }

  public static <T> ResponseEntity<ApiResponse<T>> status(HttpStatus status, String message, T data) {
    return ResponseEntity.status(status).body(ApiResponse.success(message, data));
  }

  public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(ApiResponse.error(message));
  }

  public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message, T data) {
    return ResponseEntity.status(status).body(ApiResponse.error(message, data));
  }
}
