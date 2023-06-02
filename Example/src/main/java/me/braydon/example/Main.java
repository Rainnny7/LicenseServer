package me.braydon.example;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Braydon
 */
public final class Main {
    public static void main(String[] args) {
        LicenseExample.LicenseResponse response = LicenseExample.check("XXXX-XXXX-XXXX-XXXX", "Example");
        if (!response.isValid()) { // License isn't valid
            System.err.println("Invalid license: " + response.getError());
            return;
        }
        // License is valid
        System.out.println("License is valid!");
        if (response.getOwnerName() != null) {
            System.out.println("Welcome " + response.getOwnerName() + "!");
        }
        if (response.getDescription() != null) {
            System.out.println("Description: " + response.getDescription()); // License description
        }
        if (response.isPermanent()) { // License is permanent
            System.out.println("Your license is permanent");
        } else { // License has an expiration date
            System.out.printf("Your license will expire at: %s%n", response.getExpires().toInstant());
        }
    }
}