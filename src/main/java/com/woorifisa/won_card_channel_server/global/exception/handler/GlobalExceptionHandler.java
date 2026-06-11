package com.woorifisa.won_card_channel_server.global.exception.handler;

import com.woorifisa.won_card_channel_server.domain.chat.exception.code.ChatErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.code.CommonErrorCode;
import com.woorifisa.won_card_channel_server.global.exception.code.ErrorCode;
import com.woorifisa.won_card_channel_server.global.response.ErrorResponse;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("business exception: code={}, method={}, uri={}", errorCode.getCode(), request.getMethod(), request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInputValue(Exception e, HttpServletRequest request) {
        if (e instanceof MethodArgumentNotValidException validationException) {
            String fields = validationException.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + ":" + fieldError.getCode())
                    .collect(Collectors.joining(", "));
            log.warn("invalid input: method={}, uri={}, fields={}", request.getMethod(), request.getRequestURI(), fields);
        } else if (e instanceof ConstraintViolationException constraintViolationException) {
            String violations = constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ":" + violation.getMessageTemplate())
                    .collect(Collectors.joining(", "));
            log.warn("invalid input: method={}, uri={}, violations={}", request.getMethod(), request.getRequestURI(), violations);
        }
        return ResponseEntity
                .status(CommonErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ErrorResponse.of(CommonErrorCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ServletRequestBindingException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequestFormat(Exception e, HttpServletRequest request) {
        if (e instanceof HttpMessageNotReadableException notReadableException) {
            Throwable rootCause = notReadableException.getMostSpecificCause();
            String rootCauseType = rootCause == null ? "unknown" : rootCause.getClass().getSimpleName();
            log.warn("invalid request format: method={}, uri={}, rootCause={}", request.getMethod(), request.getRequestURI(), rootCauseType);
        } else {
            log.warn("invalid request format: method={}, uri={}", request.getMethod(), request.getRequestURI());
        }
        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST_FORMAT.getHttpStatus())
                .body(ErrorResponse.of(CommonErrorCode.INVALID_REQUEST_FORMAT));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("method not supported: method={}, uri={}", request.getMethod(), request.getRequestURI());
        return ResponseEntity
                .status(CommonErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(ErrorResponse.of(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientResponseException(WebClientResponseException e) {
        log.error("webclient error: status={}, url={}", e.getStatusCode(), e.getRequest() != null ? e.getRequest().getURI() : "unknown");
        return ResponseEntity
                .status(ChatErrorCode.COMMON_WAS_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ChatErrorCode.COMMON_WAS_ERROR));
    }

    @ExceptionHandler(WebClientException.class)
    public ResponseEntity<ErrorResponse> handleWebClientException(WebClientException e) {
        log.error("webclient request error: {}", e.getMessage());
        return ResponseEntity
                .status(ChatErrorCode.COMMON_WAS_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ChatErrorCode.COMMON_WAS_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("unexpected exception: method={}, uri={}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}
