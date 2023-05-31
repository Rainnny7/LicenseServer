package me.braydon.license.repository;

import lombok.NonNull;
import me.braydon.license.model.License;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The repository for {@link License}'s.
 *
 * @author Braydon
 */
@Repository
public interface LicenseRepository extends MongoRepository<License, String> {
    /**
     * Get the license that has the given
     * key and is for the given product.
     *
     * @param key     the key to get
     * @param product the product the key is for
     * @return the optional license
     * @see License for license
     */
    @Query("{ key: ?0, product: ?1 }")
    Optional<License> getLicense(@NonNull String key, @NonNull String product);
}
