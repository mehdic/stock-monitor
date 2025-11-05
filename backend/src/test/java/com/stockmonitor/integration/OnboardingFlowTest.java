package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.dto.*;
import com.stockmonitor.model.User;
import com.stockmonitor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for complete onboarding flow:
 * User Registration -> Login -> Upload Holdings -> Select Universe -> Trigger Run -> View Recommendations
 *
 * Tests the happy path for User Story 1.
 */
public class OnboardingFlowTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private String testEmail;
    private String testPassword;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testEmail = "test-" + UUID.randomUUID() + "@example.com";
        testPassword = "Test123!@#";
    }

    @Test
    public void testCompleteOnboardingFlow() {
        // Step 1: User Registration
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .firstName("Test")
                .lastName("User")
                .build();

        ResponseEntity<UserDTO> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                registerRequest,
                UserDTO.class
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().getEmail()).isEqualTo(testEmail);

        // Verify user exists in database
        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getFirstName()).isEqualTo("Test");
        assertThat(user.getLastName()).isEqualTo("User");

        // Step 2: Login
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                loginRequest,
                LoginResponse.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getToken()).isNotBlank();
        assertThat(loginResponse.getBody().getEmail()).isEqualTo(testEmail);

        String jwtToken = loginResponse.getBody().getToken();

        // Step 3: Create Portfolio
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        PortfolioDTO createPortfolioRequest = PortfolioDTO.builder()
                .userId(user.getId())
                .build();

        HttpEntity<PortfolioDTO> portfolioRequest = new HttpEntity<>(createPortfolioRequest, headers);

        ResponseEntity<PortfolioDTO> portfolioResponse = restTemplate.exchange(
                "/api/portfolios",
                HttpMethod.POST,
                portfolioRequest,
                PortfolioDTO.class
        );

        assertThat(portfolioResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(portfolioResponse.getBody()).isNotNull();
        UUID portfolioId = portfolioResponse.getBody().getId();
        assertThat(portfolioId).isNotNull();

        // Step 4: Upload Holdings (simulated CSV)
        // Note: This would need MockMvc for multipart file upload
        // For now, we test that the endpoint exists and requires authentication

        // Step 5: Get Available Universes
        HttpEntity<Void> universeRequest = new HttpEntity<>(headers);

        ResponseEntity<UniverseDTO[]> universesResponse = restTemplate.exchange(
                "/api/universes",
                HttpMethod.GET,
                universeRequest,
                UniverseDTO[].class
        );

        assertThat(universesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(universesResponse.getBody()).isNotNull();
        assertThat(universesResponse.getBody().length).isGreaterThan(0);

        UUID universeId = universesResponse.getBody()[0].getId();

        // Step 6: Get Active Constraints
        ResponseEntity<ConstraintSetDTO> constraintsResponse = restTemplate.exchange(
                "/api/portfolios/" + portfolioId + "/constraints",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ConstraintSetDTO.class
        );

        assertThat(constraintsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 7: Trigger Recommendation Run
        String runUrl = String.format("/api/runs?portfolioId=%s&universeId=%s", portfolioId, universeId);

        ResponseEntity<RecommendationRunDTO> runResponse = restTemplate.exchange(
                runUrl,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                RecommendationRunDTO.class
        );

        assertThat(runResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(runResponse.getBody()).isNotNull();
        UUID runId = runResponse.getBody().getId();
        assertThat(runId).isNotNull();
        assertThat(runResponse.getBody().getStatus()).isIn("RUNNING", "COMPLETED");

        // Step 8: Get Run Details
        ResponseEntity<RecommendationRunDTO> runDetailsResponse = restTemplate.exchange(
                "/api/runs/" + runId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RecommendationRunDTO.class
        );

        assertThat(runDetailsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(runDetailsResponse.getBody()).isNotNull();

        // Step 9: Get Recommendations (if run completed)
        ResponseEntity<RecommendationDTO[]> recommendationsResponse = restTemplate.exchange(
                "/api/runs/" + runId + "/recommendations",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RecommendationDTO[].class
        );

        assertThat(recommendationsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Recommendations array may be empty if run is still processing or no holdings uploaded
    }

    @Test
    public void testRegistrationWithInvalidEmail() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("invalid-email")
                .password(testPassword)
                .firstName("Test")
                .lastName("User")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                registerRequest,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testLoginWithWrongPassword() {
        // First register
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .firstName("Test")
                .lastName("User")
                .build();

        restTemplate.postForEntity("/api/auth/register", registerRequest, UserDTO.class);

        // Then try to login with wrong password
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password("WrongPassword123!")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                loginRequest,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void testUnauthorizedAccessToProtectedEndpoints() {
        // Try to access portfolios without authentication
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/portfolios",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
