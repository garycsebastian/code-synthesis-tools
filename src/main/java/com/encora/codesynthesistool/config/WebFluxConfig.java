package com.encora.codesynthesistool.config;

import com.encora.codesynthesistool.exception.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
@AllArgsConstructor
public class WebFluxConfig {

    private final ObjectMapper objectMapper; // Inject ObjectMapper

    @Bean
    @Order(-2)
    public ErrorWebExceptionHandler errorWebExceptionHandler() {
        return (exchange, ex) -> {
            log.error("ErrorWebExceptionHandler caught: {}", ex.getMessage(), ex);

            if (ex instanceof ResponseStatusException) {
                return handleResponseStatusException(exchange, (ResponseStatusException) ex);
            } else {
                return handleGeneralException(exchange, ex);
            }
        };
    }

    private Mono<Void> handleResponseStatusException(
            ServerWebExchange exchange, ResponseStatusException ex) {
        exchange.getResponse().setStatusCode(ex.getStatusCode());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.resolve(ex.getStatusCode().value()), ex.getReason());
        return exchange.getResponse().writeWith(Mono.just(toJson(exchange, errorResponse)));
    }

    private Mono<Void> handleGeneralException(ServerWebExchange exchange, Throwable ex) {
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        return exchange.getResponse().writeWith(Mono.just(toJson(exchange, errorResponse)));
    }

    private DataBuffer toJson(ServerWebExchange exchange, Object object) {
        try {
            return exchange.getResponse()
                    .bufferFactory()
                    .wrap(objectMapper.writeValueAsBytes(object));
        } catch (JsonProcessingException e) {
            log.error("Error writing exception to response", e);
            return exchange.getResponse()
                    .bufferFactory()
                    .wrap("Error writing exception to response".getBytes());
        }
    }
}
