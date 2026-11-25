package com.enterprise.ticket.support;

import com.enterprise.ticket.exception.RestException;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RestException.class)
  public ResponseEntity<ApiResponse<Void>> handleRestException(RestException ex) {
    return ResponseBuilder.<Void>builder()
        .withSuccess(false)
        .withStatus(ex.getStatus())
        .withMessage(ex.getMessage())
        .build();
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });
    return ResponseBuilder.<Map<String, String>>builder()
        .withSuccess(false)
        .withStatus(HttpStatus.BAD_REQUEST)
        .withMessage("Validation failed")
        .withData(errors)
        .build();
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
    return ResponseBuilder.<Void>builder()
        .withSuccess(false)
        .withStatus(HttpStatus.UNAUTHORIZED)
        .withMessage("Invalid username or password")
        .build();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseBuilder.<Void>builder()
        .withSuccess(false)
        .withStatus(HttpStatus.BAD_REQUEST)
        .withMessage(ex.getMessage())
        .build();
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
    return ResponseBuilder.<Void>builder()
        .withSuccess(false)
        .withStatus(HttpStatus.NOT_FOUND)
        .withMessage("Resource not found: " + ex.getResourcePath())
        .build();
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
    return ResponseBuilder.<Void>builder()
        .withSuccess(false)
        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        .withMessage("An unexpected error occurred: " + ex.getMessage())
        .build();
  }
}
