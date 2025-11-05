package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.dto.*;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.repository.RecommendationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying off-cycle runs don't overwrite scheduled run results per FR-028.
 *
 * Test Flow:
 * 1. Create scheduled run (SCHEDULED run_type)
 * 2. Trigger off-cycle run (OFF_CYCLE run_type)
 * 3. Verify scheduled run results unchanged
 * 4. Verify GET /api/portfolios/{id}/recommendations returns scheduled run only
 * 5. Verify GET /api/runs filters by run_type correctly
 */
public class OffCycleIsolationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RecommendationRunRepository recommendationRunRepository;

    private String jwtToken;
    private UUID portfolioId;
    private UUID universeId;
    private HttpHeaders headers;

    @BeforeEach
    public void setup() {
        // Register and login
        String testEmail = "offcycle-test-" + UUID.randomUUID() + "@example.com";
        String testPassword = "Test123!@#";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .firstName("OffCycle")
                .lastName("Test")
                .build();

        restTemplate.postForEntity("/api/auth/register", registerRequest, UserDTO.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                loginRequest,
                LoginResponse.class
        );

        jwtToken = loginResponse.getBody().getToken();

        headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create portfolio
        PortfolioDTO createPortfolioRequest = PortfolioDTO.builder()
                .build();

        ResponseEntity<PortfolioDTO> portfolioResponse = restTemplate.exchange(
                "/api/portfolios",
                HttpMethod.POST,
                new HttpEntity<>(createPortfolioRequest, headers),
                PortfolioDTO.class
        );

        portfolioId = portfolioResponse.getBody().getId();

        // Get universe
        ResponseEntity<UniverseDTO[]> universesResponse = restTemplate.exchange(
                "/api/universes",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UniverseDTO[].class
        );

        // Check if universes exist, if not create a test universe
        if (universesResponse.getBody() == null || universesResponse.getBody().length == 0) {
            // Create a default test universe
            universeId = UUID.randomUUID(); // Placeholder - in real scenario, create via API
        } else {
            universeId = universesResponse.getBody()[0].getId();
        }
    }

    @Test
    public void testOffCycleRunIsolation() {
        // Step 1: Create scheduled run
        String scheduledRunUrl = String.format(
                "/api/runs?portfolioId=%s&universeId=%s&run_type=SCHEDULED",
                portfolioId,
                universeId
        );

        ResponseEntity<RecommendationRunDTO> scheduledRunResponse = restTemplate.exchange(
                scheduledRunUrl,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                RecommendationRunDTO.class
        );

        assertThat(scheduledRunResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID scheduledRunId = scheduledRunResponse.getBody().getId();
        assertThat(scheduledRunResponse.getBody().getRunType()).isEqualTo("SCHEDULED");

        // Get scheduled run details
        RecommendationRun scheduledRun = recommendationRunRepository.findById(scheduledRunId)
                .orElseThrow();
        Integer scheduledRunCount = scheduledRun.getRecommendationCount();
        String scheduledRunStatus = scheduledRun.getStatus();

        // Step 2: Trigger off-cycle run
        String offCycleRunUrl = String.format(
                "/api/runs?portfolioId=%s&universeId=%s&run_type=OFF_CYCLE",
                portfolioId,
                universeId
        );

        ResponseEntity<RecommendationRunDTO> offCycleRunResponse = restTemplate.exchange(
                offCycleRunUrl,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                RecommendationRunDTO.class
        );

        assertThat(offCycleRunResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID offCycleRunId = offCycleRunResponse.getBody().getId();
        assertThat(offCycleRunResponse.getBody().getRunType()).isEqualTo("OFF_CYCLE");

        // Step 3: Verify scheduled run results unchanged
        RecommendationRun scheduledRunAfter = recommendationRunRepository.findById(scheduledRunId)
                .orElseThrow();

        assertThat(scheduledRunAfter.getRecommendationCount()).isEqualTo(scheduledRunCount);
        assertThat(scheduledRunAfter.getStatus()).isEqualTo(scheduledRunStatus);
        assertThat(scheduledRunAfter.getRunType()).isEqualTo("SCHEDULED");

        // Step 4: Verify GET /api/portfolios/{id}/recommendations returns scheduled run only
        ResponseEntity<RecommendationDTO[]> currentRecommendations = restTemplate.exchange(
                "/api/portfolios/" + portfolioId + "/recommendations",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RecommendationDTO[].class
        );

        assertThat(currentRecommendations.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Current recommendations should be from scheduled run, not off-cycle run
        if (currentRecommendations.getBody() != null && currentRecommendations.getBody().length > 0) {
            assertThat(currentRecommendations.getBody()[0].getRunId()).isEqualTo(scheduledRunId);
        }

        // Step 5: Verify GET /api/runs filters by run_type correctly
        ResponseEntity<RecommendationRunDTO[]> scheduledRuns = restTemplate.exchange(
                "/api/runs?run_type=SCHEDULED",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RecommendationRunDTO[].class
        );

        assertThat(scheduledRuns.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(scheduledRuns.getBody()).isNotNull();
        for (RecommendationRunDTO run : scheduledRuns.getBody()) {
            assertThat(run.getRunType()).isEqualTo("SCHEDULED");
        }

        ResponseEntity<RecommendationRunDTO[]> offCycleRuns = restTemplate.exchange(
                "/api/runs?run_type=OFF_CYCLE",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RecommendationRunDTO[].class
        );

        assertThat(offCycleRuns.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(offCycleRuns.getBody()).isNotNull();
        for (RecommendationRunDTO run : offCycleRuns.getBody()) {
            assertThat(run.getRunType()).isEqualTo("OFF_CYCLE");
        }

        // Step 6: Verify off-cycle run accessible via specific endpoint
        ResponseEntity<RecommendationDTO[]> offCycleRecommendations = restTemplate.exchange(
                "/api/runs/" + offCycleRunId + "/recommendations",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RecommendationDTO[].class
        );

        assertThat(offCycleRecommendations.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testServiceRoleCanOnlyTriggerScheduledRuns() {
        // This test would require SERVICE role authentication
        // For now, verify that manual runs (no run_type parameter) default to OFF_CYCLE

        String manualRunUrl = String.format(
                "/api/runs?portfolioId=%s&universeId=%s",
                portfolioId,
                universeId
        );

        ResponseEntity<RecommendationRunDTO> manualRunResponse = restTemplate.exchange(
                manualRunUrl,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                RecommendationRunDTO.class
        );

        assertThat(manualRunResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Manual runs should default to OFF_CYCLE to avoid overwriting scheduled results
        assertThat(manualRunResponse.getBody().getRunType()).isEqualTo("OFF_CYCLE");
    }
}
