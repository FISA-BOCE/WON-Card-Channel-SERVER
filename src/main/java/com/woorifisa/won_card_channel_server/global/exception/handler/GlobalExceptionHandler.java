package com.woorifisa.won_card_channel_server.global.exception.handler;

import com.woorifisa.won_card_channel_server.global.exception.code.CommonErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import com.woorifisa.won_card_channel_server.global.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("business exception: code={}, type={}", errorCode.getCode(), e.getClass().getSimpleName());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInputValue(Exception e) {
        if (e instanceof MethodArgumentNotValidException validationException) {
            String fields = validationException.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + ":" + fieldError.getCode())
                    .collect(Collectors.joining(", "));
            log.warn("invalid input: type={}, fields={}", e.getClass().getSimpleName(), fields, e);
        } else if (e instanceof ConstraintViolationException constraintViolationException) {
            String violations = constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ":" + violation.getMessageTemplate())
                    .collect(Collectors.joining(", "));
            log.warn("invalid input: type={}, violations={}", e.getClass().getSimpleName(), violations, e);
        }
        return ResponseEntity
                .status(CommonErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ErrorResponse.of(CommonErrorCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ServletRequestBindingException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequestFormat(Exception e) {
        if (e instanceof HttpMessageNotReadableException notReadableException) {
            Throwable rootCause = notReadableException.getMostSpecificCause();
            String rootCauseType = rootCause == null ? "unknown" : rootCause.getClass().getSimpleName();
            log.warn("invalid request format: type={}, rootCause={}", e.getClass().getSimpleName(), rootCauseType, e);
        } else {
            log.warn("invalid request format: type={}", e.getClass().getSimpleName(), e);
        }
        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST_FORMAT.getHttpStatus())
                .body(ErrorResponse.of(CommonErrorCode.INVALID_REQUEST_FORMAT));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("method not supported: {}", e.getMessage());
        return ResponseEntity
                .status(CommonErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(ErrorResponse.of(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("unexpected exception", e);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}
