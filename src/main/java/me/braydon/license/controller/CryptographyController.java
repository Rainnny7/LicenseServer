/*
 * Copyright (c) 2023 Braydon (Rainnny). All rights reserved.
 *
 * For inquiries, please contact braydonrainnny@gmail.com
 */
package me.braydon.license.controller;

import lombok.NonNull;
import me.braydon.license.model.License;
import me.braydon.license.service.CryptographyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.PublicKey;

/**
 * @author Braydon
 */
@RestController
@RequestMapping(value = "/crypto", produces = MediaType.APPLICATION_JSON_VALUE)
public final class CryptographyController {
    /**
     * The {@link CryptographyService} to use.
     */
    @NonNull private final CryptographyService service;
    
    @Autowired
    public CryptographyController(@NonNull CryptographyService service) {
        this.service = service;
    }
    
    /**
     * Downloads the public key file.
     *
     * @return the response entity
     * @see PublicKey for public key
     * @see License for license
     * @see ResponseEntity for response entity
     */
    @GetMapping("/pub")
    @ResponseBody
    public ResponseEntity<Resource> publicKey() {
        byte[] publicKey = service.getKeyPair().getPublic().getEncoded(); // Get the public key
        String fileName = "public.key"; // The name of the file to download
        return ResponseEntity.ok()
                   .contentType(MediaType.APPLICATION_OCTET_STREAM)
                   .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                   .contentLength(publicKey.length)
                   .body(new ByteArrayResource(publicKey));
    }
}
