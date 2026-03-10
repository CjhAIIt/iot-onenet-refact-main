package com.aurora.iotonenet.api.exception;

import com.aurora.iotonenet.api.dto.LedOperationResponse;
import com.aurora.iotonenet.api.dto.LoginResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        String path = ex.getParameter().getExecutable().getName();
        if (path.contains("login")) {
            return ResponseEntity.badRequest().body(new LoginResponse(false, message));
        }
        return ResponseEntity.badRequest().body(new LedOperationResponse("error", null, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<LedOperationResponse> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new LedOperationResponse("error", null, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<LedOperationResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new LedOperationResponse("error", null, ex.getMessage()));
    }
}
