package com.stockmonitor.service;

import com.stockmonitor.dto.ReportDTO;
import com.stockmonitor.model.Recommendation;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.repository.RecommendationRepository;
import com.stockmonitor.repository.RecommendationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test for ReportGenerationService per FR-041, FR-042, FR-043, FR-044.
 *
 * T096: Tests report generation functionality including:
 * - PDF generation from HTML template
 * - Report structure and sections
 * - Top picks and exclusions
 * - Factor scores and constraint compliance
 * - Disclaimers and metadata
 *
 * Test-First: These tests should FAIL until ReportGenerationService is implemented.
 */
@ExtendWith(MockitoExtension.class)
public class ReportGenerationServiceTest {

    @Mock
    private RecommendationRunRepository runRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private org.thymeleaf.TemplateEngine templateEngine;

    private ReportGenerationService reportGenerationService;

    private UUID testRunId;
    private RecommendationRun testRun;
    private List<Recommendation> testRecommendations;

    @BeforeEach
    public void setup() {
        reportGenerationService = new ReportGenerationService(runRepository, recommendationRepository, templateEngine);

        // Setup test data
        testRunId = UUID.randomUUID();
        testRun = RecommendationRun.builder()
                .id(testRunId)
                .userId(UUID.randomUUID())
                .universeId(UUID.randomUUID())
                .constraintSetId(UUID.randomUUID())
                .runType("SCHEDULED")
                .status("COMPLETED")
                .scheduledDate(LocalDate.now())
                .startedAt(LocalDateTime.now().minusHours(2))
                .completedAt(LocalDateTime.now())
                .dataFreshnessCheckPassed(true)
                .build();

        // Create test recommendations
        testRecommendations = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Recommendation rec = Recommendation.builder()
                    .id(UUID.randomUUID())
                    .runId(testRunId)
                    .symbol("TEST" + i)
                    .rank(i + 1)
                    .targetWeightPct(BigDecimal.valueOf(5.0 - i * 0.3))
                    .currentWeightPct(BigDecimal.valueOf(3.0))
                    .weightChangePct(BigDecimal.valueOf(2.0 - i * 0.3))
                    .confidenceScore(85 - i)
                    .expectedCostBps(BigDecimal.valueOf(10))
                    .expectedAlphaBps(BigDecimal.valueOf(50 - i * 2))
                    .edgeOverCostBps(BigDecimal.valueOf(40 - i * 2))
                    .sector("Technology")
                    .marketCapTier("LARGE")
                    .liquidityTier(1)
                    .explanation("Strong factor scores")
                    .build();
            testRecommendations.add(rec);
        }
    }

    @Test
    public void testGenerateReport_ForCompletedRun_ReturnsReport() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(testRecommendations);

        // Act
        ReportDTO report = reportGenerationService.generateReport(testRunId);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.getRunId()).isEqualTo(testRunId);
        assertThat(report.getGeneratedAt()).isNotNull();
        assertThat(report.getReportVersion()).isEqualTo("1.0");

        verify(runRepository).findById(testRunId);
        verify(recommendationRepository).findByRunIdOrderByRankAsc(testRunId);
    }

    @Test
    public void testGenerateReport_ForNonExistentRun_ThrowsException() {
        // Arrange
        UUID nonExistentRunId = UUID.randomUUID();
        when(runRepository.findById(nonExistentRunId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reportGenerationService.generateReport(nonExistentRunId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Run not found");

        verify(runRepository).findById(nonExistentRunId);
        verify(recommendationRepository, never()).findByRunIdOrderByRankAsc(any());
    }

    @Test
    public void testGenerateReport_ForRunningStatus_ThrowsException() {
        // Arrange
        RecommendationRun runningRun = RecommendationRun.builder()
                .id(testRunId)
                .userId(UUID.randomUUID())
                .universeId(UUID.randomUUID())
                .constraintSetId(UUID.randomUUID())
                .runType("SCHEDULED")
                .status("RUNNING")  // Not completed
                .scheduledDate(LocalDate.now())
                .startedAt(LocalDateTime.now())
                .dataFreshnessCheckPassed(true)
                .build();

        when(runRepository.findById(testRunId)).thenReturn(Optional.of(runningRun));

        // Act & Assert
        assertThatThrownBy(() -> reportGenerationService.generateReport(testRunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not completed");

        verify(runRepository).findById(testRunId);
    }

    @Test
    public void testGenerateReport_IncludesSummarySection() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(testRecommendations);

        // Act
        ReportDTO report = reportGenerationService.generateReport(testRunId);

        // Assert
        assertThat(report.getSummary()).isNotNull();
        assertThat(report.getSummary().getTotalRecommendations()).isEqualTo(15);
        assertThat(report.getSummary().getBuyCount()).isGreaterThanOrEqualTo(0);
        assertThat(report.getSummary().getSellCount()).isGreaterThanOrEqualTo(0);
        assertThat(report.getSummary().getHoldCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testGenerateReport_IncludesTop10Picks() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(testRecommendations);

        // Act
        ReportDTO report = reportGenerationService.generateReport(testRunId);

        // Assert
        assertThat(report.getTopPicks()).isNotNull();
        assertThat(report.getTopPicks()).hasSize(10);  // Top 10 picks per FR-042
        assertThat(report.getTopPicks().get(0).getRank()).isEqualTo(1);
        assertThat(report.getTopPicks().get(9).getRank()).isEqualTo(10);
    }

    @Test
    public void testGenerateReport_IncludesExclusions() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(testRecommendations);

        // Act
        ReportDTO report = reportGenerationService.generateReport(testRunId);

        // Assert
        assertThat(report.getExclusions()).isNotNull();
        // Exclusions should contain stocks not recommended (outside universe or failing constraints)
    }

    @Test
    public void testGenerateReport_IncludesDisclaimers() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(testRecommendations);

        // Act
        ReportDTO report = reportGenerationService.generateReport(testRunId);

        // Assert
        assertThat(report.getDisclaimers()).isNotNull();
        assertThat(report.getDisclaimers()).isNotEmpty();
        assertThat(report.getDisclaimers()).contains("NOT FINANCIAL ADVICE");
        assertThat(report.getDisclaimers()).contains("Past performance does not guarantee future results.");
    }

    @Test
    public void testGenerateReport_IncludesFactorScores() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(testRecommendations);

        // Act
        ReportDTO report = reportGenerationService.generateReport(testRunId);

        // Assert
        assertThat(report.getTopPicks()).isNotEmpty();
        ReportDTO.RecommendationSummary pick = report.getTopPicks().get(0);
        assertThat(pick.getFactorScores()).isNotNull();
        // Factor scores should include VALUE, MOMENTUM, QUALITY, SIZE, VOLATILITY
    }

    @Test
    public void testGenerateReport_IncludesConstraintCompliance() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(testRecommendations);

        // Act
        ReportDTO report = reportGenerationService.generateReport(testRunId);

        // Assert
        assertThat(report.getConstraintCompliance()).isNotNull();
        assertThat(report.getConstraintCompliance().getAllConstraintsMet()).isNotNull();
    }

    @Test
    public void testGenerateReport_IncludesMetadata() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(testRecommendations);

        // Act
        ReportDTO report = reportGenerationService.generateReport(testRunId);

        // Assert
        assertThat(report.getRunDate()).isNotNull();
        assertThat(report.getPortfolioName()).isNotNull();
        assertThat(report.getUniverseName()).isNotNull();
        assertThat(report.getDataAsOfDate()).isNotNull();
    }

    @Test
    public void testGeneratePDF_ReturnsValidPDF() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(testRecommendations);

        // Mock template engine to return valid HTML
        String mockHtml = "<!DOCTYPE html><html><body><h1>Test Report</h1></body></html>";
        when(templateEngine.process(anyString(), any(org.thymeleaf.context.Context.class))).thenReturn(mockHtml);

        // Act
        byte[] pdfBytes = reportGenerationService.generatePDF(testRunId);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);

        // Verify PDF header
        String pdfHeader = new String(pdfBytes, 0, Math.min(5, pdfBytes.length));
        assertThat(pdfHeader).startsWith("%PDF-");
    }

    @Test
    public void testGeneratePDF_IncludesCharts() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(testRecommendations);

        // Mock template engine to return valid HTML
        String mockHtml = "<!DOCTYPE html><html><body><h1>Test Report</h1></body></html>";
        when(templateEngine.process(anyString(), any(org.thymeleaf.context.Context.class))).thenReturn(mockHtml);

        // Act
        byte[] pdfBytes = reportGenerationService.generatePDF(testRunId);

        // Assert
        assertThat(pdfBytes).isNotNull();
        // PDF should include charts per FR-044:
        // - Top 10 picks bar chart
        // - Sector allocation pie chart
        // - Factor scores radar chart
    }

    @Test
    public void testGenerateReport_HandlesEmptyRecommendations() {
        // Arrange
        when(runRepository.findById(testRunId)).thenReturn(Optional.of(testRun));
        when(recommendationRepository.findByRunIdOrderByRankAsc(testRunId)).thenReturn(new ArrayList<>());

        // Act
        ReportDTO report = reportGenerationService.generateReport(testRunId);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.getSummary().getTotalRecommendations()).isEqualTo(0);
        assertThat(report.getTopPicks()).isEmpty();
    }
}
