/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.braydon.license.model.License;

/**
 * A data transfer object that contains
 * the body for checking a {@link License}.
 *
 * @author Braydon
 */
@AllArgsConstructor @Getter @ToString
public class LicenseCheckBodyDTO {
    /**
     * The license key to check.
     */
    private String key;
    
    /**
     * The product of the license to check.
     */
    private String product;
    
    /**
     * The hardware id of the user checking the license.
     */
    private String hwid;
    
    /**
     * Are these params valid?
     *
     * @return whether the params are valid
     */
    public boolean isValid() {
        return key != null && product != null && hwid != null;
    }
}
