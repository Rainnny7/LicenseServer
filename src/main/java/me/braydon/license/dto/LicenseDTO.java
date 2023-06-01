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
     * The Discord snowflake of the owner of this license.
     * <p>
     * If this is -1, the license is not owned by anyone.
     * </p>
     */
    private long ownerSnowflake;
    
    /**
     * The Discord name of the owner of this license.
     * <p>
     * If this is null, the license is not owned by anyone.
     * </p>
     */
    private String ownerName;
    
    /**
     * The duration that this licensee is valid for.
     * <p>
     * If -1, the license will be permanent.
     * </p>
     */
    private long duration;
}
