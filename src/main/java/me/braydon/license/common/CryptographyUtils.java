/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.common;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.crypto.Cipher;
import java.io.File;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author Braydon
 */
@UtilityClass
public final class CryptographyUtils {
    private static final String ALGORITHM = "RSA"; // The algorithm to use
    
    /**
     * Generate a new key pair.
     *
     * @return the key pair
     * @see KeyPair for key pair
     */
    @NonNull @SneakyThrows
    public static KeyPair generateKeyPair() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM); // Create a generator
        generator.initialize(2048); // Set the key size
        return generator.generateKeyPair(); // Return our generated key pair
    }
    
    /**
     * Read the public key from the given file.
     *
     * @param keyFile the key file to read
     * @return the public key
     * @see PrivateKey for public key
     */
    @SneakyThrows
    public static PublicKey readPublicKey(@NonNull File keyFile) {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Files.readAllBytes(keyFile.toPath())); // Get the key spec
        return KeyFactory.getInstance(ALGORITHM).generatePublic(keySpec); // Return the public key from the key spec
    }
    
    /**
     * Read the private key from the given file.
     *
     * @param keyFile the key file to read
     * @return the private key
     * @see PrivateKey for private key
     */
    @SneakyThrows
    public static PrivateKey readPrivateKey(@NonNull File keyFile) {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Files.readAllBytes(keyFile.toPath())); // Get the key spec
        return KeyFactory.getInstance(ALGORITHM).generatePrivate(keySpec); // Return the private key from the key spec
    }
    
    /**
     * Decrypt the given input with
     * the provided private key.
     *
     * @param input      the encrypted input
     * @param privateKey the private key
     * @return the decrypted result
     * @see PrivateKey for private key
     */
    @SneakyThrows @NonNull
    public static String decryptMessage(@NonNull String input, @NonNull PrivateKey privateKey) {
        Cipher cipher = Cipher.getInstance(ALGORITHM); // Create the cipher
        cipher.init(Cipher.DECRYPT_MODE, privateKey); // Set our mode and private key
        return new String(cipher.doFinal(Base64.getDecoder().decode(input))); // Return our decrypted result
    }
}