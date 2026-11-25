package com.enterprise.ticket.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResponseBuilder<T> {

  private HttpStatus status = HttpStatus.OK;
  private boolean success = true;
  private String message;
  private T data;

  private ResponseBuilder() {}

  public static <T> ResponseBuilder<T> builder() {
    return new ResponseBuilder<>();
  }

  public ResponseBuilder<T> success() {
    this.success = true;
    this.status = HttpStatus.OK;
    return this;
  }

  public ResponseBuilder<T> success(String message) {
    this.success = true;
    this.status = HttpStatus.OK;
    this.message = message;
    return this;
  }

  public ResponseBuilder<T> created() {
    this.success = true;
    this.status = HttpStatus.CREATED;
    return this;
  }

  public ResponseBuilder<T> created(String message) {
    this.success = true;
    this.status = HttpStatus.CREATED;
    this.message = message;
    return this;
  }

  public ResponseBuilder<T> error(HttpStatus status, String message) {
    this.success = false;
    this.status = status;
    this.message = message;
    return this;
  }

  public ResponseBuilder<T> withStatus(HttpStatus status) {
    this.status = status;
    return this;
  }

  public ResponseBuilder<T> withSuccess(boolean success) {
    this.success = success;
    return this;
  }

  public ResponseBuilder<T> withMessage(String message) {
    this.message = message;
    return this;
  }

  public ResponseBuilder<T> withData(T data) {
    this.data = data;
    return this;
  }

  public ResponseEntity<ApiResponse<T>> build() {
    String resolvedMessage = this.message;
    if (resolvedMessage == null || resolvedMessage.trim().isEmpty()) {
      resolvedMessage = this.success ? "Operation successful" : "Operation failed";
    }

    ApiResponse<T> apiResponse =
        ApiResponse.<T>builder()
            .success(this.success)
            .message(resolvedMessage)
            .data(this.data)
            .build();

    return ResponseEntity.status(this.status).body(apiResponse);
  }
}
