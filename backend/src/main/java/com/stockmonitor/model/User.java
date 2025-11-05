package com.stockmonitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import io.hypersistence.utils.hibernate.type.json.JsonType;

@Entity
@Table(name = "app_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Email
  @NotBlank
  @Column(nullable = false, unique = true)
  private String email;

  @NotBlank
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Size(max = 100)
  @Column(name = "first_name")
  private String firstName;

  @Size(max = 100)
  @Column(name = "last_name")
  private String lastName;

  @Builder.Default
  @Column(name = "base_currency", length = 3, nullable = false)
  private String baseCurrency = "USD";

  @Column(name = "selected_universe_id")
  private UUID selectedUniverseId;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private UserRole role = UserRole.OWNER;

  @Builder.Default
  @Column(name = "email_verified", nullable = false)
  private Boolean emailVerified = false;

  @Builder.Default
  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @Column(name = "disclaimer_accepted_at")
  private LocalDateTime disclaimerAcceptedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Builder.Default
  @Column(name = "timezone", nullable = false)
  private String timezone = "America/New_York";

  @Type(JsonType.class)
  @Column(name = "notification_preferences", columnDefinition = "jsonb")
  private String notificationPreferences = "{}";

  public enum UserRole {
    OWNER,
    VIEWER,
    SERVICE
  }
}
