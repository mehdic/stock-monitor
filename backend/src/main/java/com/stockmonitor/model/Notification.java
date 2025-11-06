package com.stockmonitor.model;

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

@Entity
@Table(name = "notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "run_id")
  private UUID runId;

  @NotBlank
  @Column(name = "notification_type", nullable = false, length = 30)
  private String notificationType;

  /**
   * Notification category per FR-045:
   * T-3_PRECOMPUTE, T-1_STAGED, T_FINALIZED, DATA_STALE, RUN_FAILED, CONSTRAINT_VIOLATED
   */
  @Column(name = "category", length = 50)
  private String category;

  @Column(nullable = false, length = 20)
  private String channel = "IN_APP";

  @Column(nullable = false, length = 20)
  private String priority = "NORMAL";

  @NotBlank
  @Column(nullable = false, length = 200)
  private String subject;

  /**
   * Short title for notification (displayed in notification bell)
   */
  @Column(name = "title", length = 255)
  private String title;

  @NotBlank
  @Column(nullable = false, length = 2000)
  private String message;

  @Column(name = "action_url", length = 500)
  private String actionUrl;

  @Column(name = "action_label", length = 50)
  private String actionLabel;

  @Column(name = "is_read", nullable = false)
  private Boolean isRead = false;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Column(name = "sent_at", nullable = false)
  private LocalDateTime sentAt = LocalDateTime.now();

  @Column(name = "delivery_status", nullable = false, length = 20)
  private String deliveryStatus = "SENT";

  @Column(name = "delivery_error", length = 500)
  private String deliveryError;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
