package me.braydon.license.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import me.braydon.license.LicenseServer;
import me.braydon.license.common.IPUtils;
import me.braydon.license.dto.LicenseDTO;
import me.braydon.license.exception.APIException;
import me.braydon.license.model.License;
import me.braydon.license.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Braydon
 */
@RestController
@RequestMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
public final class LicenseController {
    /**
     * The {@link LicenseService} to use.
     */
    @NonNull private final LicenseService service;
    
    @Autowired
    public LicenseController(@NonNull LicenseService service) {
        this.service = service;
    }
    
    /**
     * This route handle checking of licenses.
     *
     * @param body the body of the request
     * @return the response entity
     * @see License for license
     * @see ResponseEntity for response entity
     */
    @PostMapping("/check")
    @ResponseBody
    public ResponseEntity<?> check(@NonNull HttpServletRequest request, @RequestBody @NonNull String body) {
        try { // Attempt to check the license
            String ip = IPUtils.getRealIp(request); // The IP of the requester
            
            JsonObject jsonObject = LicenseServer.GSON.fromJson(body, JsonObject.class);
            JsonElement key = jsonObject.get("key"); // Get the key
            JsonElement product = jsonObject.get("product"); // Get the product
            JsonElement hwid = jsonObject.get("hwid"); // Get the hwid
            
            // Ensure the body keys aren't null
            if (key.isJsonNull() || product.isJsonNull() || hwid.isJsonNull()) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Invalid request body");
            }
            // Ensure the IP is valid
            if (IPUtils.getIpType(ip) == -1) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Invalid IP address");
            }
            // Ensure the HWID is valid
            // TODO: improve :)
            String hwidString = hwid.getAsString();
            boolean invalidHwid = true;
            if (hwidString.contains("-")) {
                int segments = hwidString.substring(0, hwidString.lastIndexOf("-")).split("-").length;
                if (segments == 4) {
                    invalidHwid = false;
                }
            }
            if (invalidHwid) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Invalid HWID");
            }
            
            // Check the license
            License license = service.check(
                key.getAsString(),
                product.getAsString(),
                ip,
                hwidString
            );
            // Return OK with the license DTO
            return ResponseEntity.ok(new LicenseDTO(
                license.getDescription(),
                license.getOwnerSnowflake(),
                license.getOwnerName(),
                license.getExpires()
            ));
        } catch (APIException ex) { // Handle the exception
            return ResponseEntity.status(ex.getStatus())
                       .body(Map.of("error", ex.getLocalizedMessage()));
        }
    }
}
