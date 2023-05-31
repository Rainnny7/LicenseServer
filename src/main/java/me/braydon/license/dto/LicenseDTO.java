package me.braydon.license.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.braydon.license.model.License;

/**
 * A data transfer object for a {@link License}.
 *
 * @author Braydon
 */
@AllArgsConstructor @Getter @ToString
public class LicenseDTO {
    /**
     * The optional description of this license.
     */
    private String description;
    
    /**
     * The duration that this licensee is valid for.
     * <p>
     * If -1, the license will be permanent.
     * </p>
     */
    private long duration;
}
