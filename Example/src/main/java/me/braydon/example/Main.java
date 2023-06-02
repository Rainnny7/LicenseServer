package me.braydon.example;

/**
 * @author Braydon
 */
public final class Main {
    public static void main(String[] args) {
        LicenseExample.LicenseResponse response = LicenseExample.check("C45E-40F6-924C-753B", "CloudSpigot");
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