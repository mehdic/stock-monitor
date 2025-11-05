package com.stockmonitor.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id")
  private UUID userId;

  @NotBlank
  @Column(name = "entity_type", nullable = false, length = 50)
  private String entityType;

  @NotNull
  @Column(name = "entity_id", nullable = false)
  private UUID entityId;

  @NotBlank
  @Column(nullable = false, length = 20)
  private String action;

  @Column(name = "action_detail", length = 200)
  private String actionDetail;

  @Type(JsonType.class)
  @Column(name = "before_state", columnDefinition = "TEXT")
  private String beforeState = "{}";

  @Type(JsonType.class)
  @Column(name = "after_state", columnDefinition = "TEXT")
  private String afterState = "{}";

  @Column(name = "changed_fields")
  private String[] changedFields;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", length = 500)
  private String userAgent;

  @Column(name = "session_id", length = 100)
  private String sessionId;

  @Column(name = "request_id", length = 100)
  private String requestId;

  @Column(nullable = false)
  private Boolean success = true;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "execution_duration_ms")
  private Long executionDurationMs;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
