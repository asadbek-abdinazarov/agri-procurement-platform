package com.agriprocurement.procurement.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private final boolean success;
    private final T data;
    private final String message;
    private final LocalDateTime timestamp;
    private final ErrorDetails error;

    private ApiResponse(boolean success, T data, String message, ErrorDetails error) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static <T> ApiResponse<T> error(String message, String details) {
        return new ApiResponse<>(false, null, message, new ErrorDetails(details));
    }

    public static <T> ApiResponse<T> error(String message, String details, String field) {
        return new ApiResponse<>(false, null, message, new ErrorDetails(details, field));
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private final String details;
        private final String field;

        public ErrorDetails(String details) {
            this(details, null);
        }

        public ErrorDetails(String details, String field) {
            this.details = details;
            this.field = field;
        }
    }
}
