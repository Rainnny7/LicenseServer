package me.braydon.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
 * TODO: Convert to okhttp?
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
    
    @NonNull
    public static LicenseResponse check(@NonNull String key, @NonNull String product) {
        String hardwareId = getHardwareId(); // Get the machine's hardware id
        
        // Build the body
        Map<String, Object> body = new HashMap<>();
        body.put("key", key);
        body.put("product", product);
        body.put("hwid", hardwareId);
        String bodyJson = GSON.toJson(body); // The json body
        
        HttpURLConnection connection = null;
        int responseCode = -1; // The response code
        try {
            // Try and send the request to the server
            connection = (HttpURLConnection) new URL(CHECK_ENDPOINT).openConnection();
            connection.setRequestMethod("POST"); // Sending a POST request
            connection.setRequestProperty("Content-Type", "application/json"); // We want JSON as the response
            connection.setDoOutput(true); // We want to send a body
            
            // Write the body to the connection
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] input = bodyJson.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
            }
            responseCode = connection.getResponseCode(); // Get the response code
            
            // If the response code is OK, we can read the response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream();
                     InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                     BufferedReader reader = new BufferedReader(inputStreamReader)
                ) {
                    // Read the response
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    // Parse the response as JSON
                    JsonObject json = GSON.fromJson(response.toString(), JsonObject.class);
                    JsonElement description = json.get("description");
                    JsonElement ownerSnowflake = json.get("ownerSnowflake");
                    JsonElement ownerName = json.get("ownerName");
                    JsonElement duration = json.get("duration");
                    return new LicenseResponse(200, null,
                        description.isJsonNull() ? null : description.getAsString(),
                        ownerSnowflake.isJsonNull() ? -1 : ownerSnowflake.getAsLong(),
                        ownerName.isJsonNull() ? null : ownerName.getAsString(),
                        duration.isJsonNull() ? -1 : duration.getAsLong()
                    );
                }
            } else { // Otherwise, the request failed
                // Check if the response has an error message in JSON format
                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) { // Read the error response
                        try (InputStreamReader errorStreamReader = new InputStreamReader(errorStream);
                             BufferedReader errorReader = new BufferedReader(errorStreamReader)
                        ) {
                            
                            StringBuilder errorResponse = new StringBuilder();
                            String errorLine;
                            while ((errorLine = errorReader.readLine()) != null) {
                                errorResponse.append(errorLine);
                            }
                            // Parse the error response as JSON
                            JsonObject jsonError = GSON.fromJson(errorResponse.toString(), JsonObject.class);
                            JsonElement errorMessage = jsonError.get("error");
                            if (!errorMessage.isJsonNull()) { // If the error message isn't null, we can return it
                                return new LicenseResponse(responseCode, errorMessage.getAsString());
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            // Close the connection if it's open
            if (connection != null) {
                connection.disconnect();
            }
        }
        // Didn't find an error message, return an unknown error
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
         * The duration of the license, present if valid.
         * <p>
         * If -1, the license will be permanent.
         * </p>
         */
        private long duration;
        
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
            return duration == -1;
        }
    }
}