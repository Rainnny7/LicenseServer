package me.braydon.license.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import me.braydon.license.exception.APIException;
import me.braydon.license.exception.LicenseHwidLimitExceededException;
import me.braydon.license.exception.LicenseIpLimitExceededException;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Set;

/**
 * Represents a license key.
 *
 * @author Braydon
 */
@Document("keys")
@Setter
@Getter
@ToString
public class License {
    /**
     * The key of this license.
     */
    @Id @NonNull private String key;
    
    /**
     * The product this license is for.
     */
    @NonNull private String product;
    
    /**
     * The optional description of this license.
     */
    private String description;
    
    /**
     * The Discord snowflake of the owner of this license.
     * <p>
     * If this is -1, the license is not owned by anyone.
     * </p>
     */
    private long ownerSnowflake;
    
    /**
     * The Discord name of the owner of this license.
     * <p>
     * If this is null, the license is not owned by anyone.
     * </p>
     */
    private String ownerName;
    
    /**
     * The amount of uses this license has.
     */
    private int uses;
    
    /**
     * The IPs used on this license.
     */
    private Set<String> ips;
    
    /**
     * The hardware IDs that were used on this license.
     */
    private Set<String> hwids;
    
    /**
     * The limit of IPs that can be used on this license.
     */
    private int ipLimit;
    
    /**
     * The limit of HWIDs that can be used on this license.
     */
    private int hwidLimit;
    
    /**
     * The duration that this licensee is valid for.
     * <p>
     * If -1, the license will be permanent.
     * </p>
     */
    private long duration;
    
    /**
     * The {@link Date} this license was last used.
     */
    private Date lastUsed;
    
    /**
     * The {@link Date} this license was created.
     */
    @NonNull private Date created;
    
    /**
     * Check if this license has expired.
     * <p>
     * If this license has no
     * expiration, this will
     * always return false.
     * </p>
     *
     * @return true if expired, otherwise false
     */
    public boolean hasExpired() {
        // License is permanent, not expired
        if (isPermanent()) {
            return false;
        }
        // Check if the license has expired
        return System.currentTimeMillis() - created.getTime() >= duration;
    }
    
    /**
     * Check if this license has no expiration.
     *
     * @return true if permanent, otherwise false
     */
    public boolean isPermanent() {
        return duration == -1L;
    }
    
    /**
     * Invoked when this license is used.
     *
     * @param ip     the ip used
     * @param ipSalt the IP salt to use
     * @param hwid   the hardware id used
     */
    public void use(@NonNull String ip, @NonNull String ipSalt, @NonNull String hwid) throws APIException {
        String hashedIp = BCrypt.hashpw(ip, ipSalt); // Hash the IP
        
        // IP limit has been exceeded
        if (!ips.contains(hashedIp) && ips.size() >= ipLimit) {
            throw new LicenseIpLimitExceededException();
        }
        // HWID limit has been exceeded
        if (!hwids.contains(hwid) && hwids.size() >= hwidLimit) {
            throw new LicenseHwidLimitExceededException();
        }
        // The license was used
        uses++; // Increment uses
        ips.add(hashedIp); // Add the used IP
        hwids.add(hwid); // Add the used HWID
        lastUsed = new Date(); // Last used now
    }
}