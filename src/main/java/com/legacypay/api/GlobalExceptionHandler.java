package com.legacypay.api;

import com.legacypay.payment.IdempotencyKeyReuseException;
import com.legacypay.payment.MissingIdempotencyKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Request validation failed");

        return new ApiError(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingIdempotencyKey(MissingIdempotencyKeyException exception) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyReuseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleIdempotencyKeyReuse(IdempotencyKeyReuseException exception) {
        return new ApiError(HttpStatus.CONFLICT.value(), exception.getMessage());
    }
}
