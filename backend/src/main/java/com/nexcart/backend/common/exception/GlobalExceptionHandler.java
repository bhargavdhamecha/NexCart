package com.nexcart.backend.common.exception;

import com.nexcart.backend.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
		return buildResponse(exception.getStatus(), exception.getErrorCode(), exception.getMessage(), request);
	}

	// Covers both JPA's OptimisticLockException and Spring Data's
	// ObjectOptimisticLockingFailureException — thrown when an @Version-checked entity (e.g.
	// Order) was concurrently modified by another transaction between this request's read and
	// write. The client should just retry.
	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException exception,
		HttpServletRequest request) {
		return buildResponse(HttpStatus.CONFLICT, ErrorCode.CONCURRENT_MODIFICATION,
			"This record was updated concurrently by another request — please retry", request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception,
		HttpServletRequest request) {
		String message = exception.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(FieldError::getDefaultMessage)
			.orElse("Validation failed");
		return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception,
		HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, exception.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
		// Genuinely unanticipated — every expected failure mode has its own more specific
		// handler above. Was previously swallowed with zero server-side trace; logged at ERROR
		// so an actual bug doesn't look identical in the logs to "nothing happened."
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), exception);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
			"An unexpected error occurred", request);
	}

	private ResponseEntity<ApiError> buildResponse(HttpStatus status, ErrorCode errorCode, String message,
		HttpServletRequest request) {
		return ResponseEntity.status(status).body(ApiError.of(status, errorCode, message, request));
	}
}
