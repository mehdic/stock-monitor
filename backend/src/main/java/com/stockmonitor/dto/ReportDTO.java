package com.stockmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for recommendation reports per FR-041, FR-042, FR-043, FR-044.
 *
 * T108: Report DTO containing all sections for downloadable reports.
 *
 * Sections:
 * - Summary: Overview statistics
 * - Top Picks: Top 10 recommendations
 * - Exclusions: Stocks excluded from recommendations
 * - Disclaimers: Legal disclaimers
 * - Constraint Compliance: Constraint validation summary
 * - Metadata: Run information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    /**
     * Run ID for this report
     */
    private UUID runId;

    /**
     * Report version (e.g., "1.0")
     */
    private String reportVersion;

    /**
     * When report was generated
     */
    private LocalDateTime generatedAt;

    /**
     * Run date (month-end date)
     */
    private LocalDate runDate;

    /**
     * Portfolio name
     */
    private String portfolioName;

    /**
     * Universe name (e.g., "S&P 500")
     */
    private String universeName;

    /**
     * Data as-of date
     */
    private LocalDate dataAsOfDate;

    /**
     * Summary section
     */
    private SummarySection summary;

    /**
     * Top picks (up to 10)
     */
    private List<RecommendationSummary> topPicks;

    /**
     * Exclusions (stocks not recommended)
     */
    private List<ExclusionSummary> exclusions;

    /**
     * Legal disclaimers
     */
    private List<String> disclaimers;

    /**
     * Constraint compliance summary
     */
    private ConstraintComplianceSection constraintCompliance;

    /**
     * Summary statistics section
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummarySection {
        private Integer totalRecommendations;
        private Integer buyCount;
        private Integer sellCount;
        private Integer holdCount;
        private BigDecimal expectedAlphaBps;
        private BigDecimal expectedCostBps;
        private BigDecimal edgeOverCostBps;
        private Integer constraintViolations;
    }

    /**
     * Individual recommendation summary for report
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationSummary {
        private Integer rank;
        private String symbol;
        private String action;
        private BigDecimal targetWeightPct;
        private BigDecimal currentWeightPct;
        private BigDecimal weightChangePct;
        private Integer confidenceScore;
        private BigDecimal expectedAlphaBps;
        private String sector;
        private String marketCapTier;
        private Integer liquidityTier;
        private String explanation;
        private Map<String, BigDecimal> factorScores;
        private String changeIndicator;
    }

    /**
     * Exclusion summary (stocks excluded from recommendations)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExclusionSummary {
        private String symbol;
        private String reason;
        private String category;
    }

    /**
     * Constraint compliance section
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConstraintComplianceSection {
        private Boolean allConstraintsMet;
        private List<ConstraintViolation> violations;
        private Map<String, String> constraintSummary;
    }

    /**
     * Individual constraint violation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConstraintViolation {
        private String constraintName;
        private String severity;
        private String description;
        private List<String> affectedSymbols;
    }
}
