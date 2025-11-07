package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.dto.ReportDTO;
import com.stockmonitor.model.Recommendation;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.model.User;
import com.stockmonitor.repository.RecommendationRepository;
import com.stockmonitor.repository.RecommendationRunRepository;
import com.stockmonitor.repository.UserRepository;
import com.stockmonitor.service.ReportGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for end-to-end PDF report generation.
 *
 * Tests:
 * - Report DTO generation from completed runs
 * - PDF byte generation from report DTOs
 * - Report content validation (summary, recommendations, disclaimers)
 * - Error handling for incomplete runs
 */
@Transactional
public class ReportGenerationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ReportGenerationService reportGenerationService;

    @Autowired
    private RecommendationRunRepository runRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private UUID testUniverseId;
    private UUID testConstraintSetId;

    @BeforeEach
    public void setup() {
        // Clean up test data
        recommendationRepository.deleteAll();
        runRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("report-test@example.com")
                .passwordHash("dummy")
                .firstName("Report")
                .lastName("Test")
                .role(User.UserRole.OWNER)
                .enabled(true)
                .emailVerified(true)
                .build();
        testUser = userRepository.save(testUser);

        testUniverseId = UUID.randomUUID();
        testConstraintSetId = UUID.randomUUID();
    }

    /**
     * Test report generation for completed run.
     */
    @Test
    public void testGenerateReport_CompletedRun_ReturnsValidReportDTO() {
        // Arrange
        RecommendationRun run = createCompletedRun();
        createRecommendation(run.getId(), "AAPL", 1, "Technology");
        createRecommendation(run.getId(), "MSFT", 2, "Technology");
        createRecommendation(run.getId(), "JPM", 3, "Financials");

        // Act
        ReportDTO report = reportGenerationService.generateReport(run.getId());

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.getRunId()).isEqualTo(run.getId());
        assertThat(report.getReportVersion()).isEqualTo("1.0");
        assertThat(report.getGeneratedAt()).isNotNull();
        assertThat(report.getRunDate()).isNotNull();
    }

    /**
     * Test report summary section contains correct data.
     */
    @Test
    public void testGenerateReport_SummarySection_ContainsCorrectData() {
        // Arrange
        RecommendationRun run = createCompletedRun();
        createRecommendation(run.getId(), "AAPL", 1, "Technology");
        createRecommendation(run.getId(), "MSFT", 2, "Technology");

        // Act
        ReportDTO report = reportGenerationService.generateReport(run.getId());

        // Assert
        assertThat(report.getSummary()).isNotNull();
        assertThat(report.getSummary().getTotalRecommendations()).isEqualTo(2);
        assertThat(report.getSummary().getExpectedAlphaBps()).isNotNull();
        assertThat(report.getSummary().getExpectedCostBps()).isNotNull();
        assertThat(report.getSummary().getEdgeOverCostBps()).isNotNull();
    }

    /**
     * Test report top picks section is populated.
     */
    @Test
    public void testGenerateReport_TopPicksSection_IsPopulated() {
        // Arrange
        RecommendationRun run = createCompletedRun();
        for (int i = 1; i <= 12; i++) {
            createRecommendation(run.getId(), "STOCK" + i, i, "Technology");
        }

        // Act
        ReportDTO report = reportGenerationService.generateReport(run.getId());

        // Assert - Top picks limited to 10
        assertThat(report.getTopPicks()).isNotNull();
        assertThat(report.getTopPicks()).hasSize(10);
        assertThat(report.getTopPicks().get(0).getRank()).isEqualTo(1);
        assertThat(report.getTopPicks().get(0).getSymbol()).isEqualTo("STOCK1");
    }

    /**
     * Test report includes legal disclaimers.
     */
    @Test
    public void testGenerateReport_DisclaimersSection_IsPresent() {
        // Arrange
        RecommendationRun run = createCompletedRun();
        createRecommendation(run.getId(), "AAPL", 1, "Technology");

        // Act
        ReportDTO report = reportGenerationService.generateReport(run.getId());

        // Assert
        assertThat(report.getDisclaimers()).isNotEmpty();
        assertThat(report.getDisclaimers()).contains("NOT FINANCIAL ADVICE");
        assertThat(report.getDisclaimers().size()).isGreaterThan(5);
    }

    /**
     * Test report constraint compliance section is populated.
     */
    @Test
    public void testGenerateReport_ConstraintComplianceSection_IsPopulated() {
        // Arrange
        RecommendationRun run = createCompletedRun();
        createRecommendation(run.getId(), "AAPL", 1, "Technology");

        // Act
        ReportDTO report = reportGenerationService.generateReport(run.getId());

        // Assert
        assertThat(report.getConstraintCompliance()).isNotNull();
        assertThat(report.getConstraintCompliance().getAllConstraintsMet()).isTrue();
        assertThat(report.getConstraintCompliance().getConstraintSummary()).isNotEmpty();
    }

    /**
     * Test PDF generation produces valid bytes.
     */
    @Test
    public void testGeneratePDF_CompletedRun_ReturnsNonEmptyBytes() {
        // Arrange
        RecommendationRun run = createCompletedRun();
        createRecommendation(run.getId(), "AAPL", 1, "Technology");

        // Act
        byte[] pdfBytes = reportGenerationService.generatePDF(run.getId());

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);

        // Verify PDF header (PDF files start with "%PDF")
        assertThat(pdfBytes[0]).isEqualTo((byte) '%');
        assertThat(pdfBytes[1]).isEqualTo((byte) 'P');
        assertThat(pdfBytes[2]).isEqualTo((byte) 'D');
        assertThat(pdfBytes[3]).isEqualTo((byte) 'F');
    }

    /**
     * Test PDF generation for finalized run.
     */
    @Test
    public void testGeneratePDF_FinalizedRun_Succeeds() {
        // Arrange
        RecommendationRun run = createFinalizedRun();
        createRecommendation(run.getId(), "AAPL", 1, "Technology");

        // Act
        byte[] pdfBytes = reportGenerationService.generatePDF(run.getId());

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }

    /**
     * Test report generation fails for non-existent run.
     */
    @Test
    public void testGenerateReport_NonExistentRun_ThrowsException() {
        // Arrange
        UUID nonExistentRunId = UUID.randomUUID();

        // Act & Assert
        assertThatThrownBy(() -> reportGenerationService.generateReport(nonExistentRunId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Run not found");
    }

    /**
     * Test report generation fails for incomplete run.
     */
    @Test
    public void testGenerateReport_IncompleteRun_ThrowsException() {
        // Arrange - Create run with RUNNING status
        RecommendationRun run = RecommendationRun.builder()
                .userId(testUser.getId())
                .portfolioId(UUID.randomUUID())
                .universeId(testUniverseId)
                .constraintSetId(testConstraintSetId)
                .runType("SCHEDULED")
                .status("RUNNING")
                .scheduledDate(LocalDate.now())
                .build();
        run = runRepository.save(run);

        // Act & Assert
        UUID finalRunId = run.getId();
        assertThatThrownBy(() -> reportGenerationService.generateReport(finalRunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not completed");
    }

    /**
     * Test report generation with no recommendations.
     */
    @Test
    public void testGenerateReport_NoRecommendations_ReturnsValidReport() {
        // Arrange
        RecommendationRun run = createCompletedRun();

        // Act
        ReportDTO report = reportGenerationService.generateReport(run.getId());

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.getSummary().getTotalRecommendations()).isEqualTo(0);
        assertThat(report.getTopPicks()).isEmpty();
    }

    /**
     * Helper method to create a completed recommendation run.
     */
    private RecommendationRun createCompletedRun() {
        RecommendationRun run = RecommendationRun.builder()
                .userId(testUser.getId())
                .portfolioId(UUID.randomUUID())
                .universeId(testUniverseId)
                .constraintSetId(testConstraintSetId)
                .runType("SCHEDULED")
                .status("COMPLETED")
                .scheduledDate(LocalDate.now())
                .startedAt(LocalDateTime.now().minusHours(1))
                .completedAt(LocalDateTime.now().minusMinutes(30))
                .executionDurationMs(1800000L)
                .build();

        return runRepository.save(run);
    }

    /**
     * Helper method to create a finalized recommendation run.
     */
    private RecommendationRun createFinalizedRun() {
        RecommendationRun run = RecommendationRun.builder()
                .userId(testUser.getId())
                .portfolioId(UUID.randomUUID())
                .universeId(testUniverseId)
                .constraintSetId(testConstraintSetId)
                .runType("SCHEDULED")
                .status("FINALIZED")
                .scheduledDate(LocalDate.now())
                .startedAt(LocalDateTime.now().minusHours(1))
                .completedAt(LocalDateTime.now().minusMinutes(30))
                .executionDurationMs(1800000L)
                .build();

        return runRepository.save(run);
    }

    /**
     * Helper method to create a recommendation.
     */
    private Recommendation createRecommendation(UUID runId, String symbol, int rank, String sector) {
        Recommendation rec = Recommendation.builder()
                .runId(runId)
                .symbol(symbol)
                .rank(rank)
                .targetWeightPct(BigDecimal.valueOf(3.5))
                .currentWeightPct(BigDecimal.valueOf(2.0))
                .weightChangePct(BigDecimal.valueOf(1.5))
                .confidenceScore(85)
                .expectedAlphaBps(BigDecimal.valueOf(50.0))
                .expectedCostBps(BigDecimal.valueOf(15.0))
                .edgeOverCostBps(BigDecimal.valueOf(35.0))
                .driver1Name("Value")
                .driver1Score(BigDecimal.valueOf(0.8))
                .driver2Name("Momentum")
                .driver2Score(BigDecimal.valueOf(0.7))
                .driver3Name("Quality")
                .driver3Score(BigDecimal.valueOf(0.9))
                .explanation("Strong fundamental indicators")
                .changeIndicator("NEW")
                .sector(sector)
                .marketCapTier("LARGE")
                .liquidityTier(1)
                .currentPrice(BigDecimal.valueOf(150.0))
                .build();

        return recommendationRepository.save(rec);
    }
}
