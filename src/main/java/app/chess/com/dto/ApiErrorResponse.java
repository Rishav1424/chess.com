package app.chess.com.dto;

import java.util.List;

public record ApiErrorResponse(boolean success, ErrorBody error) {

    public record ErrorBody(String code, String message, List<FieldError> details) {}

    public record FieldError(String field, String message) {}

    public ApiErrorResponse(String code, String message) {
        this(false, new ErrorBody(code, message, null));
    }

    public ApiErrorResponse(String code, String message, List<FieldError> details) {
        this(false, new ErrorBody(code, message, details));
    }
}