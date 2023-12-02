/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.example;

import java.io.File;

/**
 * @author Braydon
 */
public final class Main {
    public static void main(String[] args) {
        LicenseClient client = new LicenseClient("http://localhost:7500", "Example", new File("public.key")); // Create the client
        LicenseClient.LicenseResponse response = client.check("XXXX-XXXX-XXXX-XXXX"); // Check our license
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