/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import me.braydon.license.common.CryptographyUtils;
import me.braydon.license.common.IPUtils;
import me.braydon.license.dto.LicenseCheckBodyDTO;
import me.braydon.license.dto.LicenseDTO;
import me.braydon.license.exception.APIException;
import me.braydon.license.model.License;
import me.braydon.license.service.CryptographyService;
import me.braydon.license.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.PrivateKey;
import java.util.Map;

/**
 * @author Braydon
 */
@RestController
@RequestMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
public final class LicenseController {
    /**
     * The {@link CryptographyService} to use.
     */
    @NonNull private final CryptographyService cryptographyService;
    
    /**
     * The {@link LicenseService} to use.
     */
    @NonNull private final LicenseService licenseService;
    
    @Autowired
    public LicenseController(@NonNull CryptographyService cryptographyService, @NonNull LicenseService licenseService) {
        this.cryptographyService = cryptographyService;
        this.licenseService = licenseService;
    }
    
    /**
     * This route handle checking of licenses.
     *
     * @param body the body of the request
     * @return the response entity
     * @see License for license
     * @see LicenseCheckBodyDTO for body
     * @see ResponseEntity for response entity
     */
    @PostMapping("/check")
    @ResponseBody
    public ResponseEntity<?> check(@NonNull HttpServletRequest request, @RequestBody @NonNull LicenseCheckBodyDTO body) {
        try { // Attempt to check the license
            if (!body.isValid()) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Invalid request body");
            }
            // Ensure the IP is valid
            String ip = IPUtils.getRealIp(request); // The IP of the requester
            if (IPUtils.getIpType(ip) == -1) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Invalid IP address");
            }
            String key;
            String hwid;
            try {
                PrivateKey privateKey = cryptographyService.getKeyPair().getPrivate(); // Get our private key
                key = CryptographyUtils.decryptMessage(body.getKey(), privateKey); // Decrypt our license key
                hwid = CryptographyUtils.decryptMessage(body.getHwid(), privateKey); // Decrypt our hwid
            } catch (IllegalArgumentException ex) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Signature Error");
            }
            
            // Validating that the UUID is in the correct format
            boolean invalidHwid = true;
            if (hwid.contains("-")) {
                int segments = hwid.substring(0, hwid.lastIndexOf("-")).split("-").length;
                if (segments == 4) {
                    invalidHwid = false;
                }
            }
            if (invalidHwid) { // Invalid HWID
                throw new APIException(HttpStatus.BAD_REQUEST, "Invalid HWID");
            }
            
            // Check the license
            License license = licenseService.check(
                key,
                body.getProduct(),
                ip,
                hwid
            );
            // Return OK with the license DTO
            return ResponseEntity.ok(new LicenseDTO(
                license.getDescription(),
                license.getOwnerSnowflake(),
                license.getOwnerName(),
                license.getPlan(),
                license.getLatestVersion(),
                license.getExpires()
            ));
        } catch (APIException ex) { // Handle the exception
            return ResponseEntity.status(ex.getStatus())
                       .body(Map.of("error", ex.getLocalizedMessage()));
        }
    }
}
