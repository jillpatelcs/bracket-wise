package ca.bracketwise.api.dto;

import java.util.List;

public final class ErrorBodies {
    public record FieldError(String field, String code, String message) {}

    public record Problem(
            String type,
            String title,
            int status,
            String detail,
            String code,
            String requestId,
            List<FieldError> errors
    ) {}

    private ErrorBodies() {}
}
