package me.braydon.license.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.braydon.license.common.MiscUtils;
import me.braydon.license.exception.*;
import me.braydon.license.model.License;
import me.braydon.license.repository.LicenseRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

/**
 * The service for managing {@link License}'s.
 *
 * @author Braydon
 */
@Service
@Slf4j(topic = "Licenses")
public final class LicenseService {
    /**
     * The {@link LicenseRepository} to use.
     */
    @NonNull private final LicenseRepository repository;
    
    /**
     * The {@link DiscordService} to use for logging.
     */
    @NonNull private final DiscordService discordService;
    
    /**
     * The salt to use for hashing license keys.
     */
    @Value("${salts.licenses}")
    @NonNull private String licensesSalt;
    
    /**
     * The salt to use for hashing IP addresses.
     */
    @Value("${salts.ips}")
    @NonNull private String ipsSalt;
    
    @Autowired
    public LicenseService(@NonNull LicenseRepository repository, @NonNull DiscordService discordService) {
        this.repository = repository;
        this.discordService = discordService;
    }
    
    /**
     * Create a new license key.
     *
     * @param key            the key of the license
     * @param product        the product the license is for
     * @param description    the optional description of the license
     * @param ownerSnowflake the optional owner snowflake of the license
     * @param ownerName      the optional owner name of the license
     * @param ipLimit        the IP limit of the license
     * @param hwidLimit      the HWID limit of the license
     * @param duration       the duration of the license, -1 for permanent
     * @return the created license
     * @see License for license
     */
    public License create(@NonNull String key, @NonNull String product, String description, long ownerSnowflake,
                          String ownerName, int ipLimit, int hwidLimit, long duration) {
        // Create the new license
        License license = new License();
        license.setKey(BCrypt.hashpw(key, licensesSalt)); // Hash the key
        license.setProduct(product); // Use the given product
        license.setDescription(description); // Use the given description, if any
        license.setOwnerSnowflake(ownerSnowflake);
        license.setOwnerName(ownerName);
        license.setIps(new HashSet<>());
        license.setHwids(new HashSet<>());
        license.setIpLimit(ipLimit); // Use the given IP limit
        license.setHwidLimit(hwidLimit); // Use the given HWID limit
        license.setDuration(duration);
        license.setCreated(new Date());
        repository.insert(license); // Insert the newly created license
        return license;
    }
    
    /**
     * Check the given license.
     *
     * @param key     the key to check
     * @param product the product of the license
     * @param ip      the ip using the license
     * @param hwid    the hwid using the license
     * @return the checked license
     * @throws APIException if there was an error checking the license
     * @see License for license
     */
    @NonNull
    public License check(@NonNull String key, @NonNull String product, @NonNull String ip,
                         @NonNull String hwid) throws APIException {
        Optional<License> optionalLicense = repository.getLicense(BCrypt.hashpw(key, licensesSalt), product); // Get the license
        if (optionalLicense.isEmpty()) { // License key not found
            log.error("License key {} for product {} not found", key, product); // Log the error
            throw new LicenseNotFoundException();
        }
        License license = optionalLicense.get(); // The license found
        
        // Log the license being used, if enabled
        if (discordService.isLogUses()) {
            // god i hate sending discord embeds, it's so big and ugly :(
            long expirationDate = (license.getCreated().getTime() + license.getDuration()) / 1000L;
            discordService.sendLog(new EmbedBuilder()
                                       .setColor(Color.BLUE)
                                       .setTitle("License Used")
                                       .addField("License",
                                           "`" + MiscUtils.obfuscateKey(key) + "`",
                                           true
                                       )
                                       .addField("Product",
                                           license.getProduct(),
                                           true
                                       )
                                       .addField("Description",
                                           license.getDescription(),
                                           true
                                       )
                                       .addField("Owner ID",
                                           license.getOwnerSnowflake() <= 0L ? "N/A" : String.valueOf(license.getOwnerSnowflake()),
                                           true
                                       )
                                       .addField("Owner Name",
                                           license.getOwnerName() == null ? "N/A" : license.getOwnerName(),
                                           true
                                       )
                                       .addField("Expires",
                                           license.isPermanent() ? "Never" : "<t:" + expirationDate + ":R>",
                                           true
                                       )
                                       .addField("IP",
                                           ip,
                                           true
                                       )
                                       .addField("HWID",
                                           "```" + hwid + "```",
                                           false
                                       )
                                       .addField("IPs",
                                           license.getIps().size() + "/" + license.getIpLimit(),
                                           true
                                       )
                                       .addField("HWIDs",
                                           license.getHwids().size() + "/" + license.getHwidLimit(),
                                           true
                                       )
            );
        }
        // The license has expired
        if (license.hasExpired()) {
            // Log the expired license
            if (discordService.isLogExpired()) {
                discordService.sendLog(new EmbedBuilder()
                                           .setColor(Color.RED)
                                           .setTitle("License Expired")
                                           .setDescription("License `%s` is expired".formatted(MiscUtils.obfuscateKey(key)))
                );
            }
            throw new LicenseExpiredException();
        }
        try {
            license.use(ip, ipsSalt, hwid); // Use the license
            repository.save(license); // Save the used license
            log.info("License key {} for product {} was used by {} ({})", key, product, ip, hwid);
            return license;
        } catch (APIException ex) {
            // Log that the license has reached it's IP limit
            if (ex instanceof LicenseIpLimitExceededException && discordService.isLogIpLimitExceeded()) {
                discordService.sendLog(new EmbedBuilder()
                                           .setColor(Color.RED)
                                           .setTitle("License IP Limit Reached")
                                           .setDescription("License `%s` has reached it's IP limit: **%s**".formatted(
                                               MiscUtils.obfuscateKey(key),
                                               license.getIpLimit()
                                           ))
                );
            } else if (ex instanceof LicenseHwidLimitExceededException && discordService.isLogHwidLimitExceeded()) {
                discordService.sendLog(new EmbedBuilder()
                                           .setColor(Color.RED)
                                           .setTitle("License HWID Limit Reached")
                                           .setDescription("License `%s` has reached it's HWID limit: **%s**".formatted(
                                               MiscUtils.obfuscateKey(key),
                                               license.getHwidLimit()
                                           ))
                );
            }
            throw ex; // Rethrow to handle where this method was invoked
        }
    }
}
