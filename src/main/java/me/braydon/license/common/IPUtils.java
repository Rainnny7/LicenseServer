/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * @author Braydon
 */
@UtilityClass
public final class IPUtils {
    /**
     * The regex expression for validating IPv4 addresses.
     */
    public static final String IPV4_REGEX = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$";
    
    /**
     * The regex expression for validating IPv6 addresses.
     */
    public static final String IPV6_REGEX = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^(([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4})?::(([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4})?$";
    
    private static final String[] IP_HEADERS = new String[] {
        "CF-Connecting-IP",
        "X-Forwarded-For"
    };
    
    /**
     * Get the real IP from the given request.
     *
     * @param request the request
     * @return the real IP
     */
    @NonNull
    public static String getRealIp(@NonNull HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        for (String headerName : IP_HEADERS) {
            String header = request.getHeader(headerName);
            if (header == null) {
                continue;
            }
            if (!header.contains(",")) { // Handle single IP
                ip = header;
                break;
            }
            // Handle multiple IPs
            String[] ips = header.split(",");
            for (String ipHeader : ips) {
                ip = ipHeader;
                break;
            }
        }
        return ip;
    }
    
    /**
     * Get the IP type of the given input.
     *
     * @param input the input
     * @return the IP type, -1 if invalid
     */
    public static int getIpType(@NonNull String input) {
        return isIpV4(input) ? 4 : isIpV6(input) ? 6 : -1;
    }
    
    /**
     * Check if the given input is
     * a valid IPv4 address.
     *
     * @param input the input
     * @return true if IPv4, otherwise false
     */
    public static boolean isIpV4(@NonNull String input) {
        return input.matches(IPV4_REGEX);
    }
    
    /**
     * Check if the given input is
     * a valid IPv6 address.
     *
     * @param input the input
     * @return true if IPv6, otherwise false
     */
    public static boolean isIpV6(@NonNull String input) {
        return input.matches(IPV6_REGEX);
    }
}
