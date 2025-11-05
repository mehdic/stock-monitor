package com.stockmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for notifications per FR-045, FR-046, FR-047, FR-048.
 *
 * T103: Notification DTO with category, priority, and action fields.
 *
 * Categories per FR-045:
 * - T-3_PRECOMPUTE: Pre-compute job started
 * - T-1_STAGED: Staging completed, data checked
 * - T_FINALIZED: Month-end recommendations ready (HIGH priority)
 * - DATA_STALE: Data freshness check failed (HIGH priority)
 * - RUN_FAILED: Recommendation run failed
 * - CONSTRAINT_VIOLATED: Constraint violation detected
 *
 * Priorities:
 * - HIGH: Requires immediate attention (T_FINALIZED, DATA_STALE, RUN_FAILED)
 * - MEDIUM: Informational (T-3_PRECOMPUTE, T-1_STAGED)
 * - LOW: Nice to know
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    /**
     * Notification ID
     */
    private UUID id;

    /**
     * User ID receiving the notification
     */
    private UUID userId;

    /**
     * Notification category per FR-045
     */
    private String category;

    /**
     * Priority level: HIGH, MEDIUM, LOW
     */
    private String priority;

    /**
     * Notification title (short summary)
     */
    private String title;

    /**
     * Notification message (detailed description)
     */
    private String message;

    /**
     * Action button label (e.g., "View Recommendations", "Review Data")
     */
    private String actionLabel;

    /**
     * Action URL to navigate when user clicks action button
     */
    private String actionUrl;

    /**
     * Whether notification has been read
     */
    private Boolean isRead;

    /**
     * When notification was read (nullable)
     */
    private LocalDateTime readAt;

    /**
     * When notification was created
     */
    private LocalDateTime createdAt;
}
