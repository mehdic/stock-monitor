package com.stockmonitor.service;

import com.stockmonitor.model.ServiceApiKey;
import com.stockmonitor.repository.ServiceApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing service API keys for scheduled jobs and background processes.
 * Keys are stored encrypted at rest with last_used_at tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceApiKeyService {

    private final ServiceApiKeyRepository serviceApiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new service API key.
     *
     * @param name Friendly name for the key
     * @param description Description of the key's purpose
     * @param expiresInDays Number of days until key expires (null for no expiration)
     * @param createdBy Admin user creating the key
     * @return API key response with plaintext key (only shown once)
     */
    @Transactional
    public ApiKeyResponse generate(String name, String description, Integer expiresInDays, String createdBy) {
        // Generate secure random API key (32 bytes = 256 bits)
        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        String plaintextKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);

        // Hash the key for storage
        String keyHash = passwordEncoder.encode(plaintextKey);

        // Calculate expiration
        LocalDateTime expiresAt = expiresInDays != null
                ? LocalDateTime.now().plusDays(expiresInDays)
                : null;

        // Create and save entity
        ServiceApiKey serviceApiKey = ServiceApiKey.builder()
                .name(name)
                .description(description)
                .keyHash(keyHash)
                .isActive(true)
                .expiresAt(expiresAt)
                .createdBy(createdBy)
                .build();

        ServiceApiKey saved = serviceApiKeyRepository.save(serviceApiKey);
        log.info("Generated new service API key: {} (expires: {})", name, expiresAt);

        return new ApiKeyResponse(saved.getId(), plaintextKey, expiresAt);
    }

    /**
     * Rotate an existing API key - creates new key and revokes old one.
     *
     * @param oldKeyId ID of key to rotate
     * @param newExpiresInDays Expiration for new key
     * @param rotatedBy Admin performing rotation
     * @return New API key response
     */
    @Transactional
    public ApiKeyResponse rotate(UUID oldKeyId, Integer newExpiresInDays, String rotatedBy) {
        ServiceApiKey oldKey = serviceApiKeyRepository.findById(oldKeyId)
                .orElseThrow(() -> new IllegalArgumentException("Service API key not found: " + oldKeyId));

        // Generate new key with same name + "-rotated"
        String newName = oldKey.getName() + "-rotated-" + LocalDateTime.now().toLocalDate();
        ApiKeyResponse newKey = generate(newName, oldKey.getDescription(), newExpiresInDays, rotatedBy);

        // Revoke old key
        revoke(oldKeyId);

        log.info("Rotated service API key {} to {}", oldKey.getName(), newName);
        return newKey;
    }

    /**
     * Revoke a service API key (mark as inactive).
     *
     * @param keyId ID of key to revoke
     */
    @Transactional
    public void revoke(UUID keyId) {
        ServiceApiKey key = serviceApiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("Service API key not found: " + keyId));

        key.setIsActive(false);
        serviceApiKeyRepository.save(key);
        log.info("Revoked service API key: {}", key.getName());
    }

    /**
     * List all active service API keys.
     *
     * @return List of active keys (without plaintext values)
     */
    public List<ServiceApiKey> listActive() {
        return serviceApiKeyRepository.findByIsActiveTrue();
    }

    /**
     * Response object for API key generation.
     * Plaintext key is only returned once at creation time.
     */
    public record ApiKeyResponse(UUID id, String plaintextKey, LocalDateTime expiresAt) {}
}
