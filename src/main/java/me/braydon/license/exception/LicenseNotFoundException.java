/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.exception;

import me.braydon.license.model.License;
import org.springframework.http.HttpStatus;

/**
 * This exception is raised when
 * a {@link License} is not found.
 *
 * @author Braydon
 */
public class LicenseNotFoundException extends APIException {
    public LicenseNotFoundException() {
        super(HttpStatus.NOT_FOUND, "License not found");
    }
}
