/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.service;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.braydon.license.common.CryptographyUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Base64;

/**
 * @author Braydon
 */
@Service
@Slf4j(topic = "Cryptography")
@Getter
public final class CryptographyService {
    /**
     * Our {@link KeyPair}.
     */
    @NonNull private final KeyPair keyPair;
    
    @SneakyThrows
    public CryptographyService() {
        File publicKeyFile = new File("public.key"); // The private key
        File privateKeyFile = new File("private.key"); // The private key
        if (!publicKeyFile.exists() || !privateKeyFile.exists()) { // Missing private key, generate new key pair.
            keyPair = CryptographyUtils.generateKeyPair(); // Generate new key pair
            writeKey(keyPair.getPublic().getEncoded(), publicKeyFile); // Write our public key
            writeKey(keyPair.getPrivate().getEncoded(), privateKeyFile); // Write our private key
            log.info("New key pair has been generated");
            log.info(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            return;
        }
        // Load our private key from the file
        keyPair = new KeyPair(CryptographyUtils.readPublicKey(publicKeyFile), CryptographyUtils.readPrivateKey(privateKeyFile));
        log.info("Loaded private key from file " + privateKeyFile.getPath());
    }
    
    /**
     * Write the given contents to the provided file.
     *
     * @param contents the content bytes to write
     * @param file     the file to write to
     */
    private void writeKey(byte[] contents, @NonNull File file) {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(contents);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}