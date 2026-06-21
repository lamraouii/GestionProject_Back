package com.ensao.gestionprojet.exception;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>>
    handleAuthException(
            AuthException ex
    ) {

        return errorResponse(ex.getMessage(), ex.getStatus());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>>
    handleEmailAlreadyExists(
            EmailAlreadyExistsException ex
    ) {

        return errorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PasswordsDoNotMatchException.class)
    public ResponseEntity<Map<String, String>>
    handlePasswordsDoNotMatch(
            PasswordsDoNotMatchException ex
    ) {

        return errorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>>
    handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null
                ? fieldError.getDefaultMessage()
                : "Donnees invalides";

        return errorResponse(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>>
    handleConstraintViolation(
            ConstraintViolationException ex
    ) {

        return errorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>>
    handleDataIntegrityViolation() {

        return errorResponse(
                "Cette operation viole une contrainte de donnees.",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>>
    handleRuntimeException(
            RuntimeException ex
    ) {

        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Une erreur est survenue";

        return errorResponse(message, resolveStatus(message));
    }

    private ResponseEntity<Map<String, String>> errorResponse(
            String message,
            HttpStatus status
    ) {

        Map<String, String> error = new HashMap<>();
        error.put("message", message);

        return new ResponseEntity<>(error, status);
    }

    private HttpStatus resolveStatus(String message) {

        String normalizedMessage = message.toLowerCase();

        if (normalizedMessage.contains("acces refuse")
                || normalizedMessage.contains("accès refusé")
                || normalizedMessage.contains("seul le")
                || normalizedMessage.contains("seule la")
                || normalizedMessage.contains("seuls les admin")
                || normalizedMessage.contains("admin de l'entreprise")) {
            return HttpStatus.FORBIDDEN;
        }

        if (normalizedMessage.contains("introuvable")
                || normalizedMessage.contains("invalide")) {
            return HttpStatus.NOT_FOUND;
        }

        return HttpStatus.BAD_REQUEST;
    }
}
