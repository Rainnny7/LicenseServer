package me.braydon.example;

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
        } else { // License has a duration
            long durationSeconds = TimeUnit.SECONDS.toMillis(response.getDuration()); // The duration in seconds
            System.out.println("Your license will expire in " + durationSeconds + " seconds");
        }
    }
}