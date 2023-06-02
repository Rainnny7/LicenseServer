package me.braydon.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import okhttp3.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
public final class LicenseExample {
    /**
     * The endpoint to check licenses at.
     */
    private static final String CHECK_ENDPOINT = "http://localhost:7500/check";
    
    /**
     * The {@link Gson} instance to use.
     */
    private static final Gson GSON = new GsonBuilder()
                                         .serializeNulls()
                                         .create();
    
    /**
     * Check the license with the given
     * key for the given product.
     *
     * @param key     the key to check
     * @param product the product the key belongs to
     * @return the license response
     * @see LicenseResponse for response
     */
    @NonNull
    public static LicenseResponse check(@NonNull String key, @NonNull String product) {
        String hardwareId = getHardwareId(); // Get the hardware id of the machine
        
        // Build the json body
        Map<String, Object> body = new HashMap<>();
        body.put("key", key);
        body.put("product", product);
        body.put("hwid", hardwareId);
        String bodyJson = GSON.toJson(body); // The json body
        
        OkHttpClient client = new OkHttpClient(); // Create a new http client
        MediaType mediaType = MediaType.parse("application/json"); // Ensure the media type is json
        RequestBody requestBody = RequestBody.create(bodyJson, mediaType); // Build the request body
        Request request = new Request.Builder()
                              .url(CHECK_ENDPOINT)
                              .post(requestBody)
                              .build(); // Build the POST request
        
        Response response = null; // The response of the request
        int responseCode = -1; // The response code of the request
        try { // Attempt to execute the request
            response = client.newCall(request).execute();
            responseCode = response.code();
            
            // If the response is successful, we can parse the response
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                assert responseBody != null; // We don't want the response body being null
                
                JsonObject json = GSON.fromJson(responseBody.string(), JsonObject.class); // Parse the json
                JsonElement description = json.get("description");
                JsonElement ownerSnowflake = json.get("ownerSnowflake");
                JsonElement ownerName = json.get("ownerName");
                
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
     * Get the unique hardware
     * identifier of this machine.
     *
     * @return the hardware id
     */
    @NonNull
    private static String getHardwareId() {
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
    
    @AllArgsConstructor
    @Getter
    @ToString
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