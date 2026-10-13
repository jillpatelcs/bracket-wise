package ca.bracketwise.api;

import ca.bracketwise.api.dto.ErrorBodies;
import ca.bracketwise.domain.DomainException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@RestControllerAdvice
public class ProblemDetailsHandler {
    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorBodies.Problem> domain(DomainException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.code(), ex.getMessage(),
                List.of(new ErrorBodies.FieldError(ex.field(), ex.code(), ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBodies.Problem> bean(MethodArgumentNotValidException ex) {
        List<ErrorBodies.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorBodies.FieldError(fe.getField(), "NEGATIVE_VALUE", fe.getDefaultMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "NEGATIVE_VALUE", "Validation failed.", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBodies.Problem> unreadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof UnrecognizedPropertyException upe) {
            return problem(HttpStatus.BAD_REQUEST, "UNKNOWN_PROPERTY",
                    "Unknown property: " + upe.getPropertyName(),
                    List.of(new ErrorBodies.FieldError(upe.getPropertyName(), "UNKNOWN_PROPERTY",
                            "Unknown property: " + upe.getPropertyName())));
        }
        if (cause instanceof JsonProcessingException) {
            return problem(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "The request body is not valid JSON.",
                    List.of(new ErrorBodies.FieldError(null, "MALFORMED_JSON", "The request body is not valid JSON.")));
        }
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "The request body could not be read.",
                List.of(new ErrorBodies.FieldError(null, "MALFORMED_JSON", "The request body could not be read.")));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorBodies.Problem> media(HttpMediaTypeNotSupportedException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type must be application/json.",
                List.of(new ErrorBodies.FieldError(null, "UNSUPPORTED_MEDIA_TYPE", "Content-Type must be application/json.")));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorBodies.Problem> tooLarge(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Request body exceeds 16 KB.",
                List.of(new ErrorBodies.FieldError(null, "PAYLOAD_TOO_LARGE", "Request body exceeds 16 KB.")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBodies.Problem> other(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.",
                List.of(new ErrorBodies.FieldError(null, "INTERNAL_ERROR", "An unexpected error occurred.")));
    }

    private ResponseEntity<ErrorBodies.Problem> problem(HttpStatus status, String code, String detail,
                                                        List<ErrorBodies.FieldError> errors) {
        ErrorBodies.Problem body = new ErrorBodies.Problem(
                "about:blank",
                status.getReasonPhrase(),
                status.value(),
                detail,
                code,
                MDC.get("requestId"),
                errors
        );
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
