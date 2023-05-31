package me.braydon.license.service;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.braydon.license.exception.APIException;
import me.braydon.license.exception.LicenseExpiredException;
import me.braydon.license.exception.LicenseNotFoundException;
import me.braydon.license.model.License;
import me.braydon.license.repository.LicenseRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

/**
 * The service for managing {@link License}'s.
 *
 * @author Braydon
 */
@Service
@Slf4j
public final class LicenseService {
    /**
     * The {@link LicenseRepository} to use.
     */
    @NonNull private final LicenseRepository repository;
    
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
    public LicenseService(@NonNull LicenseRepository repository) {
        this.repository = repository;
    }
    
    @PostConstruct
    public void onInitialize() {
        // TODO: remove this and make it either
        //  a test, or a route to gen a license
        System.out.println("SALT - " + BCrypt.gensalt());
        //        String key = RandomUtils.generateLicenseKey();
        //        log.info(create(key,
        //            "CloudSpigot",
        //            "Testing " + Math.random(), Integer.MAX_VALUE, Integer.MAX_VALUE).toString());
        //        System.out.println("key = " + key);
    }
    
    /**
     * Create a new license key.
     *
     * @param key         the key of the license
     * @param product     the product the license is for
     * @param description the optional description of the license
     * @param ipLimit     the IP limit of the license
     * @param hwidLimit   the HWID limit of the license
     * @param duration    the duration of the license, -1 for permanent
     * @return the created license
     * @see License for license
     */
    public License create(@NonNull String key, @NonNull String product, String description,
                          int ipLimit, int hwidLimit, long duration) {
        // Create the new license
        License license = new License();
        license.setKey(BCrypt.hashpw(key, licensesSalt)); // Hash the key
        license.setProduct(product); // Use the given product
        license.setDescription(description); // Use the given description, if any
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
     * @throws APIException if there was an error checking the license
     * @see License for license
     */
    public void check(@NonNull String key, @NonNull String product,
                      @NonNull String ip, @NonNull String hwid) throws APIException {
        Optional<License> optionalLicense = repository.getLicense(BCrypt.hashpw(key, licensesSalt), product); // Get the license
        if (optionalLicense.isEmpty()) { // License key not found
            log.error("License key {} for product {} not found", key, product); // Log the error
            throw new LicenseNotFoundException();
        }
        License license = optionalLicense.get(); // The license found
        if (license.hasExpired()) { // The license has expired
            throw new LicenseExpiredException();
        }
        license.use(ip, ipsSalt, hwid); // Use the license
        repository.save(license); // Save the used license
        log.info("License key {} for product {} was used by {} ({})", key, product, ip, hwid);
    }
}
