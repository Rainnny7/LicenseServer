package me.braydon.license.exception;

import me.braydon.license.model.License;
import org.springframework.http.HttpStatus;

/**
 * This exception is raised when
 * a {@link License} has its HWID
 * limit exceeded.
 *
 * @author Braydon
 */
public class LicenseHwidLimitExceededException extends APIException {
    public LicenseHwidLimitExceededException() {
        super(HttpStatus.BAD_REQUEST, "License key HWID limit has been exceeded");
    }
}
