package ca.bracketwise.domain;

public class DomainException extends RuntimeException {
    private final String code;
    private final String field;

    public DomainException(String code, String message) {
        this(code, null, message);
    }

    public DomainException(String code, String field, String message) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String code() {
        return code;
    }

    public String field() {
        return field;
    }
}
