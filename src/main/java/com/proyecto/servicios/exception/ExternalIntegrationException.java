package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ExternalIntegrationException extends RuntimeException {

    private final HttpStatus status;

    public ExternalIntegrationException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public ExternalIntegrationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ExternalIntegrationException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public ExternalIntegrationException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
