/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.exception;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.http.HttpStatus;

/**
 * Represents an API exception.
 *
 * @author Braydon
 */
@Getter
public class APIException extends RuntimeException {
    /**
     * The status of this exception.
     *
     * @see HttpStatus for status
     */
    @NonNull private final HttpStatus status;
    
    public APIException(@NonNull HttpStatus status, @NonNull String message) {
        super(message);
        this.status = status;
    }
}
