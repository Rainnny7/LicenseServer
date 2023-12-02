/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import me.braydon.license.model.License;

import java.util.Date;

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
     * The plan for this license.
     */
    @NonNull private String plan;
    
    /**
     * The latest version of the product this license is for.
     */
    @NonNull private String latestVersion;
    
    /**
     * The optional expiration {@link Date} of this license.
     */
    private Date expires;
}
