package me.braydon.license.exception;

import me.braydon.license.model.License;
import org.springframework.http.HttpStatus;

/**
 * This exception is raised when
 * a {@link License} has been used
 * but is expired.
 *
 * @author Braydon
 */
public class LicenseExpiredException extends APIException {
    public LicenseExpiredException() {
        super(HttpStatus.BAD_REQUEST, "License has expired");
    }
}
