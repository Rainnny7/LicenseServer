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
public final class MiscUtils {
    /**
     * Obfuscate the given key.
     *
     * @param rawKey the key to obfuscate
     * @return the obfuscated key
     */
    @NonNull
    public static String obfuscateKey(@NonNull String rawKey) {
        int length = 9; // The amount of chars to show
        String key = rawKey.substring(0, length);
        return key + "*".repeat(rawKey.length() - length);
    }
}
