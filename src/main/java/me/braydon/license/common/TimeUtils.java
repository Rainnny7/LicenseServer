/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.common;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Braydon
 */
@UtilityClass
public final class TimeUtils {
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    
    /**
     * Get the current date time.
     *
     * @return the current date time
     */
    @NonNull
    public static String dateTime() {
        return DATE_TIME_FORMAT.format(new Date());
    }
}
