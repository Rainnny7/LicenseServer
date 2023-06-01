package me.braydon.license;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * @author Braydon
 */
@SpringBootApplication
@Slf4j(topic = "License Server")
public class LicenseServer {
    public static final Gson GSON = new GsonBuilder()
                                        .serializeNulls()
                                        .create();
    
    @SneakyThrows
    public static void main(@NonNull String[] args) {
        File config = new File("application.yml");
        if (!config.exists()) { // Saving the default config if it doesn't exist locally
            Files.copy(Objects.requireNonNull(LicenseServer.class.getResourceAsStream("/application.yml")), config.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved the default configuration to '{}', please re-launch the application", // Log the default config being saved
                config.getAbsolutePath()
            );
            return;
        }
        log.info("Found configuration at '{}'", config.getAbsolutePath()); // Log the found config
        SpringApplication.run(LicenseServer.class, args); // Load the application
    }
    
    @PostConstruct
    public void onInitialize() {
        // Log a randomly generated salt
        log.info("Generated a random salt: {} (This is only for you to copy and paste for config)", BCrypt.gensalt());
    }
}
