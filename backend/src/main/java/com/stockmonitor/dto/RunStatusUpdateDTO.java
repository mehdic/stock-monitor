package com.stockmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for run status updates broadcast via WebSocket per FR-046, FR-047.
 *
 * Used for real-time progress tracking of recommendation runs.
 *
 * Status lifecycle per FR-022:
 * SCHEDULED -> PRE_COMPUTE -> STAGED -> RUNNING -> FINALIZED/FAILED/ARCHIVED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunStatusUpdateDTO {

    /**
     * Recommendation run ID
     */
    private UUID runId;

    /**
     * Current run status
     */
    private String status;

    /**
     * Progress percentage (0-100)
     */
    private Integer progress;

    /**
     * Current stage/step description
     */
    private String stage;

    /**
     * Timestamp of this update
     */
    private LocalDateTime timestamp;

    /**
     * Error message if status is FAILED
     */
    private String errorMessage;

    /**
     * Estimated completion time (nullable)
     */
    private LocalDateTime estimatedCompletion;
}
