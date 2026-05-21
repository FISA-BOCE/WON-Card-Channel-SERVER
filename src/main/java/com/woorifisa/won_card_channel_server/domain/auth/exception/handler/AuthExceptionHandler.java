package com.woorifisa.won_card_channel_server.domain.auth.exception.handler;

import com.woorifisa.won_card_channel_server.domain.auth.exception.code.AuthErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import com.woorifisa.won_card_channel_server.global.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.woorifisa.won_card_channel_server.domain.auth.api")
public class AuthExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("business exception: code={}, message={}", errorCode.getCode(), e.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            ServletRequestBindingException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        if (e instanceof MethodArgumentNotValidException validationException) {
            String fields = validationException.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + ":" + fieldError.getCode())
                    .collect(Collectors.joining(", "));
            log.warn("auth bad request: type={}, fields={}", e.getClass().getSimpleName(), fields, e);
        } else if (e instanceof ConstraintViolationException constraintViolationException) {
            String violations = constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ":" + violation.getMessageTemplate())
                    .collect(Collectors.joining(", "));
            log.warn("auth bad request: type={}, violations={}", e.getClass().getSimpleName(), violations, e);
        } else if (e instanceof HttpMessageNotReadableException notReadableException) {
            Throwable rootCause = notReadableException.getMostSpecificCause();
            String rootCauseType = rootCause == null ? "unknown" : rootCause.getClass().getSimpleName();
            log.warn("auth bad request: type={}, rootCause={}", e.getClass().getSimpleName(), rootCauseType, e);
        } else {
            log.warn("auth bad request: type={}", e.getClass().getSimpleName(), e);
        }
        return ResponseEntity
                .status(AuthErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ErrorResponse.of(AuthErrorCode.INVALID_INPUT));
    }
}
