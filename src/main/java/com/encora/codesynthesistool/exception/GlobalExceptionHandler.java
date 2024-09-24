package com.encora.codesynthesistool.exception;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception ex) {
        log.error("Exception caught: {}", ex.getMessage(), ex);
        // Customize error response based on the exception type
        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(
                                new ErrorResponse(
                                        HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage())));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResponseStatusException(
            ResponseStatusException ex) {
        log.error("ResponseStatusException caught: {}", ex.getMessage(), ex);
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            // Assuming your JWT exceptions return a stringified ErrorResponse
            ErrorResponse errorResponse = new Gson().fromJson(ex.getReason(), ErrorResponse.class);
            return Mono.just(ResponseEntity.status(errorResponse.getStatus()).body(errorResponse));
        } else {
            // Handle other ResponseStatusExceptions
            return Mono.just(
                    ResponseEntity.status(ex.getStatusCode())
                            .body(
                                    new ErrorResponse(
                                            HttpStatus.resolve(ex.getStatusCode().value()),
                                            ex.getReason())));
        }
    }
}
