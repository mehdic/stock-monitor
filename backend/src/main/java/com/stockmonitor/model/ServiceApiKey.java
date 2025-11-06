package com.stockmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service API key entity for authenticating scheduled jobs and background services.
 * Keys are stored encrypted at rest with expiration and usage tracking.
 */
@Entity
@Table(name = "service_api_keys", indexes = {
        @Index(name = "idx_service_api_keys_key_hash", columnList = "key_hash"),
        @Index(name = "idx_service_api_keys_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name; // Friendly name for the key (e.g., "Monthly Scheduler")

    @Column(name = "key_hash", nullable = false, unique = true, length = 512)
    private String keyHash; // BCrypt hash of the API key

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_by")
    private String createdBy; // Admin user who created the key

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if API key is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if API key is valid (active and not expired).
     */
    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) && !isExpired();
    }
}
