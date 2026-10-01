package com.cocos.challenge.infrastructure.web.exception;

import com.cocos.challenge.domain.exception.DomainException;
import com.cocos.challenge.domain.exception.DuplicateOrderException;
import com.cocos.challenge.domain.exception.InstrumentNotFoundException;
import com.cocos.challenge.domain.exception.InvalidOrderException;
import com.cocos.challenge.domain.exception.InvalidOrderStateException;
import com.cocos.challenge.domain.exception.OrderNotFoundException;
import com.cocos.challenge.domain.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.cocos.challenge.infrastructure.web.filter.MdcTraceIdFilter;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        log.warn("[EXCEPTION] [USER_NOT_FOUND] {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), "urn:cocos:error:user-not-found");
    }

    @ExceptionHandler(InstrumentNotFoundException.class)
    public ProblemDetail handleInstrumentNotFound(InstrumentNotFoundException ex) {
        log.warn("[EXCEPTION] [INSTRUMENT_NOT_FOUND] {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "Instrument Not Found", ex.getMessage(), "urn:cocos:error:instrument-not-found");
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("[EXCEPTION] [ORDER_NOT_FOUND] {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "Order Not Found", ex.getMessage(), "urn:cocos:error:order-not-found");
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ProblemDetail handleInvalidOrderState(InvalidOrderStateException ex) {
        log.warn("[EXCEPTION] [INVALID_ORDER_STATE] {}", ex.getMessage());
        return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Order State Transition", ex.getMessage(), "urn:cocos:error:invalid-state");
    }

    @ExceptionHandler(DuplicateOrderException.class)
    public ProblemDetail handleDuplicateOrder(DuplicateOrderException ex) {
        log.warn("[EXCEPTION] [DUPLICATE_ORDER] {}", ex.getMessage());
        return createProblemDetail(HttpStatus.CONFLICT, "Duplicate Order", ex.getMessage(), "urn:cocos:error:duplicate-order");
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("[EXCEPTION] [CONCURRENCY_CONFLICT] {}", ex.getMessage());
        return createProblemDetail(HttpStatus.CONFLICT, "Conflict", "A concurrent request with the same unique key or data is already being processed.", "urn:cocos:error:data-conflict");
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ProblemDetail handleInvalidOrder(InvalidOrderException ex) {
        log.warn("[EXCEPTION] [INVALID_ORDER] {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Order", ex.getMessage(), "urn:cocos:error:invalid-order");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("[EXCEPTION] [VALIDATION_ERROR] fieldErrors={}", fieldErrors);

        ProblemDetail pd = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation Error", "Invalid request body parameters", "urn:cocos:error:validation");
        pd.setProperty("errors", fieldErrors);
        return pd;
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParams(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        log.warn("[EXCEPTION] [MISSING_PARAMETER] parameterName={}", ex.getParameterName());
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Missing Parameter",
                String.format("Required request parameter '%s' is missing.", ex.getParameterName()),
                "urn:cocos:error:missing-parameter"
        );
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        log.warn("[EXCEPTION] [TYPE_MISMATCH] parameterName={}", ex.getName());
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Type Mismatch",
                String.format("Parameter '%s' has an invalid value format.", ex.getName()),
                "urn:cocos:error:type-mismatch"
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[EXCEPTION] [MALFORMED_REQUEST] {}", ex.getMessage());
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Malformed Request",
                "The request body is invalid or malformed.",
                "urn:cocos:error:malformed-request"
        );
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        log.warn("[EXCEPTION] [BUSINESS_RULE_VIOLATION] {}", ex.getMessage());
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Business Rule Violation", ex.getMessage(), "urn:cocos:error:business-rule");
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.warn("[EXCEPTION] [RESOURCE_NOT_FOUND] {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), "urn:cocos:error:resource-not-found");
    }

    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ProblemDetail handleNoHandlerFound(org.springframework.web.servlet.NoHandlerFoundException ex) {
        log.warn("[EXCEPTION] [ENDPOINT_NOT_FOUND] {}", ex.getMessage());
        return createProblemDetail(HttpStatus.NOT_FOUND, "Endpoint Not Found", ex.getMessage(), "urn:cocos:error:endpoint-not-found");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("[EXCEPTION] [UNHANDLED_ERROR] {}", ex.getMessage(), ex);
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred.", "urn:cocos:error:internal");
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail, String type) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create(type));
        pd.setProperty("timestamp", Instant.now());
        String traceId = MDC.get(MdcTraceIdFilter.MDC_TRACE_ID_KEY);
        if (traceId != null) {
            pd.setProperty(MdcTraceIdFilter.MDC_TRACE_ID_KEY, traceId);
        }
        return pd;
    }
}
