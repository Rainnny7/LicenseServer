/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.*;
import okhttp3.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * An example of how to interact
 * with the license server. This
 * can be conveniently used in
 * any project by simply copying
 * the class into your project.
 *
 * @author Braydon
 * @see <a href="https://git.rainnny.club/Rainnny/LicenseServer">License Server</a>
 */
public final class LicenseClient {
    private static final String ALGORITHM = "RSA"; // The crypto algorithm to use
    
    /**
     * The endpoint to use for downloading the {@link PublicKey}.
     */
    private static final String PUBLIC_KEY_ENDPOINT = "/crypto/pub";
    
    /**
     * The endpoint to check licenses at.
     */
    private static final String CHECK_ENDPOINT = "/check";
    
    /**
     * The {@link Gson} instance to use.
     */
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    
    /**
     * The URL of the license server to make requests to.
     */
    @NonNull private final String appUrl;
    
    /**
     * The product to use for client.
     */
    @NonNull private final String product;
    
    /**
     * The {@link OkHttpClient} to use for requests.
     */
    @NonNull private final OkHttpClient httpClient;
    
    /**
     * The {@link PublicKey} to use for encryption.
     */
    @NonNull private final PublicKey publicKey;
    
    public LicenseClient(@NonNull String appUrl, @NonNull String product, @NonNull File publicKeyFile) {
        this.appUrl = appUrl;
        this.product = product;
        httpClient = new OkHttpClient(); // Create a new http client
        publicKey = fetchPublicKey(publicKeyFile); // Fetch our public key
    }
    
    /**
     * Read the public key from the given bytes.
     *
     * @param bytes the bytes of the public key
     * @return the public key
     * @see PrivateKey for public key
     */
    @SneakyThrows
    private static PublicKey readPublicKey(byte[] bytes) {
        return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(bytes));
    }
    
    /**
     * Check the license with the given
     * key for the given product.
     *
     * @param key     the key to check
     * @return the license response
     * @see LicenseResponse for response
     */
    @NonNull
    public LicenseResponse check(@NonNull String key) {
        String hardwareId = getHardwareId(); // Get the hardware id of the machine
        
        // Build the json body
        Map<String, Object> body = new HashMap<>();
        body.put("key", encrypt(key));
        body.put("product", product);
        body.put("hwid", encrypt(hardwareId));
        String bodyJson = GSON.toJson(body); // The json body
        
        MediaType mediaType = MediaType.parse("application/json"); // Ensure the media type is json
        RequestBody requestBody = RequestBody.create(mediaType, bodyJson); // Build the request body
        Request request = new Request.Builder()
                              .url(appUrl + CHECK_ENDPOINT)
                              .post(requestBody)
                              .build(); // Build the POST request
        
        Response response = null; // The response of the request
        int responseCode = -1; // The response code of the request
        try { // Attempt to execute the request
            response = httpClient.newCall(request).execute();
            responseCode = response.code();
            
            // If the response is successful, we can parse the response
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                assert responseBody != null; // We don't want the response body being null
                
                JsonObject json = GSON.fromJson(responseBody.string(), JsonObject.class); // Parse the json
                JsonElement description = json.get("description");
                JsonElement ownerSnowflake = json.get("ownerSnowflake");
                JsonElement ownerName = json.get("ownerName");
                JsonElement plan = json.get("plan");
                JsonElement latestVersion = json.get("latestVersion");
                
                // Parsing the expiration date if we have one
                JsonElement expires = json.get("expires");
                Date expiresDate = null;
                if (!expires.isJsonNull()) {
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(expires.getAsString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    expiresDate = Date.from(offsetDateTime.toInstant());
                }
                
                // Return the license response
                return new LicenseResponse(200, null,
                    description.isJsonNull() ? null : description.getAsString(),
                    ownerSnowflake.isJsonNull() ? -1 : ownerSnowflake.getAsLong(),
                    ownerName.isJsonNull() ? null : ownerName.getAsString(),
                    plan.getAsString(),
                    latestVersion.getAsString(),
                    expires.isJsonNull() ? null : expiresDate
                );
            } else {
                ResponseBody errorBody = response.body(); // Get the error body
                if (errorBody != null) { // If we have an error body, we can parse it
                    String errorResponse = errorBody.string();
                    JsonObject jsonError = GSON.fromJson(errorResponse, JsonObject.class);
                    JsonElement errorMessage = jsonError.get("error");
                    if (!errorMessage.isJsonNull()) { // We have an error message, return it
                        return new LicenseResponse(responseCode, errorMessage.getAsString());
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            // Close the response if it's open
            if (response != null) {
                response.close();
            }
        }
        // Return an unknown error
        return new LicenseResponse(responseCode, "An unknown error occurred");
    }
    
    /**
     * Fetch the public key.
     * <p>
     * If the public key is not already present, we
     * fetch it from the server. Otherwise, the public
     * key is loaded from the file.
     * </p>
     *
     * @param publicKeyFile the public key file
     * @return the public key
     * @see PublicKey for public key
     */
    @SneakyThrows
    private PublicKey fetchPublicKey(@NonNull File publicKeyFile) {
        byte[] publicKey;
        if (publicKeyFile.exists()) { // Public key exists, use it
            publicKey = Files.readAllBytes(publicKeyFile.toPath());
        } else {
            Request request = new Request.Builder()
                                  .url(appUrl + PUBLIC_KEY_ENDPOINT)
                                  .build(); // Build the GET request
            @Cleanup Response response = httpClient.newCall(request).execute(); // Make the request
            if (!response.isSuccessful()) { // Response wasn't successful
                throw new IOException("Failed to download the public key, got response " + response.code());
            }
            ResponseBody body = response.body(); // Get the response body
            assert body != null; // We need a response body
            publicKey = body.bytes(); // Read our public key
            
            // Write the response to the public key file
            try (FileOutputStream outputStream = new FileOutputStream(publicKeyFile)) {
                outputStream.write(publicKey);
            }
        }
        return readPublicKey(publicKey);
    }
    
    /**
     * Get the unique hardware
     * identifier of this machine.
     *
     * @return the hardware id
     */
    @NonNull
    private String getHardwareId() {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
        CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
        ComputerSystem computerSystem = hardwareAbstractionLayer.getComputerSystem();
        
        // Retrieve necessary hardware information
        String vendor = operatingSystem.getManufacturer();
        String processorSerialNumber = computerSystem.getSerialNumber();
        String uuid = computerSystem.getHardwareUUID();
        String processorIdentifier = centralProcessor.getProcessorIdentifier().getIdentifier();
        int processors = centralProcessor.getLogicalProcessorCount();
        
        // Generate a unique hardware id using the retrieved information
        return String.format("%08x", vendor.hashCode()) + "-"
                   + String.format("%08x", processorSerialNumber.hashCode()) + "-"
                   + String.format("%08x", uuid.hashCode()) + "-"
                   + String.format("%08x", processorIdentifier.hashCode()) + "-" + processors;
    }
    
    /**
     * Encrypt the given input.
     *
     * @param input the encrypted input
     * @return the encrypted result
     */
    @SneakyThrows @NonNull
    private String encrypt(@NonNull String input) {
        Cipher cipher = Cipher.getInstance(ALGORITHM); // Create our cipher
        cipher.init(Cipher.ENCRYPT_MODE, publicKey); // Set our mode and public key
        return Base64.getEncoder().encodeToString(cipher.doFinal(input.getBytes())); // Return our encrypted result
    }
    
    /**
     * The response of a license check.
     *
     * @see #check(String)
     */
    @AllArgsConstructor @Getter @ToString
    public static class LicenseResponse {
        /**
         * The status code of the response.
         */
        private final long status;
        
        /**
         * The error in the response, null if none.
         */
        private String error;
        
        /**
         * The description of the license, present if valid.
         */
        private String description;
        
        /**
         * The Discord snowflake of the license owner, present
         * if valid and there is an owner.
         */
        private long ownerSnowflake;
        
        /**
         * The Discord name of the license owner, present
         * if valid and there is an owner.
         */
        private String ownerName;
        
        /**
         * The plan for this license.
         */
        @NonNull private String plan;
        
        /**
         * The latest version of the product this license is for.
         */
        @NonNull private String latestVersion;
        
        /**
         * The optional expiration {@link Date} of the license.
         */
        private Date expires;
        
        public LicenseResponse(long status, @NonNull String error) {
            this.status = status;
            this.error = error;
        }
        
        /**
         * Check if the license is valid.
         *
         * @return true if valid, otherwise false
         */
        public boolean isValid() {
            return status == 200;
        }
        
        /**
         * Check if the license is permanent.
         *
         * @return true if permanent, otherwise false
         */
        public boolean isPermanent() {
            return expires == null;
        }
    }
}