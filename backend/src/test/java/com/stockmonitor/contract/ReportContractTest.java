package com.stockmonitor.contract;
import com.stockmonitor.BaseIntegrationTest;

import com.stockmonitor.dto.ReportDTO;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.repository.RecommendationRunRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for GET /api/runs/{id}/report endpoint per FR-041, FR-042, FR-043.
 *
 * Verifies:
 * - Report generation for completed runs
 * - Report structure includes summary, recommendations, explanations, disclaimers
 * - PDF download functionality
 * - Report sections: top picks, exclusions, factor scores, constraint compliance
 *
 * Test-First: This test should FAIL until ReportController and ReportGenerationService are implemented.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ReportContractTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RecommendationRunRepository runRepository;

    private UUID testRunId;

    @BeforeEach
    public void setup() {
        // Create a completed recommendation run for testing
        RecommendationRun run = RecommendationRun.builder()
                .userId(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .universeId(UUID.randomUUID())
                .constraintSetId(UUID.randomUUID())
                .runType("SCHEDULED")
                .status("COMPLETED")
                .scheduledDate(LocalDate.now())
                .startedAt(LocalDateTime.now().minusHours(1))
                .completedAt(LocalDateTime.now())
                .dataFreshnessCheckPassed(true)
                .build();

        run = runRepository.save(run);
        testRunId = run.getId();
    }

    @AfterEach
    public void cleanupTestData() {
        runRepository.deleteAll();
    }

    @Test
    public void testGetReportForCompletedRun_ReturnsReport() {
        // Act
        ResponseEntity<ReportDTO> response = restTemplate.exchange(
                "/api/runs/{id}/report",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                ReportDTO.class,
                testRunId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ReportDTO report = response.getBody();
        assertThat(report.getRunId()).isEqualTo(testRunId);
        assertThat(report.getGeneratedAt()).isNotNull();
        assertThat(report.getReportVersion()).isEqualTo("1.0");
    }

    @Test
    public void testGetReport_ContainsSummarySection() {
        // Act
        ResponseEntity<ReportDTO> response = restTemplate.exchange(
                "/api/runs/{id}/report",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                ReportDTO.class,
                testRunId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReportDTO report = response.getBody();
        assertThat(report).isNotNull();

        // Verify summary section
        assertThat(report.getSummary()).isNotNull();
        assertThat(report.getSummary().getTotalRecommendations()).isGreaterThanOrEqualTo(0);
        assertThat(report.getSummary().getBuyCount()).isGreaterThanOrEqualTo(0);
        assertThat(report.getSummary().getSellCount()).isGreaterThanOrEqualTo(0);
        assertThat(report.getSummary().getHoldCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testGetReport_ContainsTopPicks() {
        // Act
        ResponseEntity<ReportDTO> response = restTemplate.exchange(
                "/api/runs/{id}/report",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                ReportDTO.class,
                testRunId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReportDTO report = response.getBody();
        assertThat(report).isNotNull();

        // Verify top picks section (should have up to 10 top recommendations)
        assertThat(report.getTopPicks()).isNotNull();
        assertThat(report.getTopPicks().size()).isLessThanOrEqualTo(10);
    }

    @Test
    public void testGetReport_ContainsExclusions() {
        // Act
        ResponseEntity<ReportDTO> response = restTemplate.exchange(
                "/api/runs/{id}/report",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                ReportDTO.class,
                testRunId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReportDTO report = response.getBody();
        assertThat(report).isNotNull();

        // Verify exclusions section
        assertThat(report.getExclusions()).isNotNull();
    }

    @Test
    public void testGetReport_ContainsDisclaimers() {
        // Act
        ResponseEntity<ReportDTO> response = restTemplate.exchange(
                "/api/runs/{id}/report",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                ReportDTO.class,
                testRunId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReportDTO report = response.getBody();
        assertThat(report).isNotNull();

        // Verify disclaimers section
        assertThat(report.getDisclaimers()).isNotNull();
        assertThat(report.getDisclaimers()).isNotEmpty();
        assertThat(report.getDisclaimers()).contains("NOT FINANCIAL ADVICE");
    }

    @Test
    public void testGetReport_ContainsFactorScores() {
        // Act
        ResponseEntity<ReportDTO> response = restTemplate.exchange(
                "/api/runs/{id}/report",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                ReportDTO.class,
                testRunId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReportDTO report = response.getBody();
        assertThat(report).isNotNull();

        // Verify factor scores are included for each recommendation
        if (!report.getTopPicks().isEmpty()) {
            ReportDTO.RecommendationSummary pick = report.getTopPicks().get(0);
            assertThat(pick.getFactorScores()).isNotNull();
        }
    }

    @Test
    public void testGetReport_ContainsConstraintCompliance() {
        // Act
        ResponseEntity<ReportDTO> response = restTemplate.exchange(
                "/api/runs/{id}/report",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                ReportDTO.class,
                testRunId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReportDTO report = response.getBody();
        assertThat(report).isNotNull();

        // Verify constraint compliance section
        assertThat(report.getConstraintCompliance()).isNotNull();
        assertThat(report.getConstraintCompliance().getAllConstraintsMet()).isNotNull();
    }

    @Test
    public void testGetReport_ForNonExistentRun_Returns404() {
        // Arrange
        UUID nonExistentRunId = UUID.randomUUID();

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/runs/{id}/report",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                String.class,
                nonExistentRunId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void testGetReport_ForRunningStatus_Returns400() {
        // Arrange - Create a run with RUNNING status
        RecommendationRun runningRun = RecommendationRun.builder()
                .userId(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .universeId(UUID.randomUUID())
                .constraintSetId(UUID.randomUUID())
                .runType("SCHEDULED")
                .status("RUNNING")
                .scheduledDate(LocalDate.now())
                .startedAt(LocalDateTime.now())
                .dataFreshnessCheckPassed(true)
                .build();

        runningRun = runRepository.save(runningRun);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/runs/{id}/report",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                String.class,
                runningRun.getId()
        );

        // Assert - Report should only be available for COMPLETED runs
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testDownloadReportPDF_ReturnsApplicationPdf() {
        // Act
        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/runs/{id}/report/pdf",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                byte[].class,
                testRunId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);

        // Verify PDF header (starts with %PDF-)
        byte[] body = response.getBody();
        String pdfHeader = new String(body, 0, Math.min(5, body.length));
        assertThat(pdfHeader).startsWith("%PDF-");
    }

    @Test
    public void testGetReport_ContainsMetadata() {
        // Act
        ResponseEntity<ReportDTO> response = restTemplate.exchange(
                "/api/runs/{id}/report",
                HttpMethod.GET,
                createAuthEntity("test@example.com"),
                ReportDTO.class,
                testRunId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReportDTO report = response.getBody();
        assertThat(report).isNotNull();

        // Verify metadata
        assertThat(report.getRunDate()).isNotNull();
        assertThat(report.getPortfolioName()).isNotNull();
        assertThat(report.getUniverseName()).isNotNull();
        assertThat(report.getDataAsOfDate()).isNotNull();
    }
}
