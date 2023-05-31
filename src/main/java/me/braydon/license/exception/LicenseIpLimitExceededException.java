package me.braydon.license.exception;

import me.braydon.license.model.License;
import org.springframework.http.HttpStatus;

/**
 * This exception is raised when
 * a {@link License} has its IP
 * limit exceeded.
 *
 * @author Braydon
 */
public class LicenseIpLimitExceededException extends APIException {
    public LicenseIpLimitExceededException() {
        super(HttpStatus.BAD_REQUEST, "License key IP limit has been exceeded");
    }
}
