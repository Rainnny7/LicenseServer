/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.common;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * @author Braydon
 */
@UtilityClass
public final class RandomUtils {
    /**
     * The license key format to use.
     */
    private static final String LICENSE_KEY_FORMAT = "%04X-%04X-%04X-%04X";
    
    /**
     * Generate a random license key.
     *
     * @return the random license key
     */
    @NonNull
    public static String generateLicenseKey() {
        int segments = LICENSE_KEY_FORMAT.split("-").length; // The amount of segments
        Object[] parts = new Object[segments];
        for (int i = 0; i < segments; i++) { // Generate a random part for each segment
            parts[i] = (int) (Math.random() * 0xFFFF);
        }
        return String.format(LICENSE_KEY_FORMAT, parts);
    }
}
