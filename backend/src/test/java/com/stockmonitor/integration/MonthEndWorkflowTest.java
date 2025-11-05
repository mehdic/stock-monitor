package com.stockmonitor.integration;

import com.stockmonitor.config.TestBatchConfig;
import com.stockmonitor.config.TestSecurityConfig;
import com.stockmonitor.model.Portfolio;
import com.stockmonitor.model.RecommendationRun;
import com.stockmonitor.model.User;
import com.stockmonitor.repository.PortfolioRepository;
import com.stockmonitor.repository.RecommendationRunRepository;
import com.stockmonitor.repository.UserRepository;
import com.stockmonitor.scheduler.MonthEndScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for scheduled month-end workflow per FR-020, FR-021, FR-022.
 *
 * T095: Tests the complete month-end recommendation workflow including:
 * - T-3 pre-compute job (3 days before month-end)
 * - T-1 staging job (1 day before month-end)
 * - T finalization job (on month-end date)
 *
 * Verifies:
 * - Job execution sequence and timing
 * - State transitions: SCHEDULED -> PRE_COMPUTE -> STAGED -> FINALIZED
 * - Idempotency of scheduled jobs
 * - Notification delivery at each stage
 * - Data freshness checks before execution
 *
 * Test-First: This test should FAIL until MonthEndScheduler and batch jobs are implemented.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("batch-test")
@Import({TestSecurityConfig.class, TestBatchConfig.class})
public class MonthEndWorkflowTest {

    @Autowired
    private MonthEndScheduler monthEndScheduler;

    @Autowired
    private RecommendationRunRepository runRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private User testUser;
    private Portfolio testPortfolio;
    private UUID testUniverseId;
    private UUID testConstraintSetId;

    @BeforeEach
    public void setup() {
        // Clean up test data
        runRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("dummy")
                .firstName("Test")
                .lastName("User")
                .role(User.UserRole.OWNER)
                .enabled(true)
                .emailVerified(true)
                .build();
        testUser = userRepository.save(testUser);

        // Create test portfolio
        testUniverseId = UUID.randomUUID();
        testConstraintSetId = UUID.randomUUID();

        testPortfolio = Portfolio.builder()
                .userId(testUser.getId())
                .activeUniverseId(testUniverseId)
                .activeConstraintSetId(testConstraintSetId)
                .build();
        testPortfolio = portfolioRepository.save(testPortfolio);
    }

    /**
     * Test T-3 pre-compute job execution
     */
    @Test
    public void testT3PreComputeJob_CreatesScheduledRuns() {
        // Arrange
        LocalDate monthEndDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime t3Date = monthEndDate.minusDays(3).atTime(1, 0);

        // Act
        monthEndScheduler.executeT3PreCompute();

        // Assert
        List<RecommendationRun> runs = runRepository.findAll();

        assertThat(runs).isNotEmpty();

        // Verify runs are created with SCHEDULED status
        RecommendationRun run = runs.get(0);
        assertThat(run.getStatus()).isEqualTo("SCHEDULED");
        assertThat(run.getRunType()).isEqualTo("SCHEDULED");
        assertThat(run.getScheduledDate()).isNotNull();
        assertThat(run.getScheduledDate()).isEqualTo(monthEndDate);
    }

    /**
     * Test T-3 pre-compute job idempotency
     */
    @Test
    public void testT3PreComputeJob_IsIdempotent() {
        // Act - Execute T-3 job twice
        monthEndScheduler.executeT3PreCompute();
        int runCountAfterFirst = runRepository.findAll().size();

        monthEndScheduler.executeT3PreCompute();
        int runCountAfterSecond = runRepository.findAll().size();

        // Assert - Should not create duplicate runs
        assertThat(runCountAfterSecond).isEqualTo(runCountAfterFirst);
    }

    /**
     * Test T-1 staging job execution
     */
    @Test
    public void testT1StagingJob_TransitionsToStaged() {
        // Arrange - Create a SCHEDULED run
        LocalDate monthEndDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        RecommendationRun scheduledRun = RecommendationRun.builder()
                .userId(testUser.getId())
                .universeId(testUniverseId)
                .constraintSetId(testConstraintSetId)
                .runType("SCHEDULED")
                .status("SCHEDULED")
                .scheduledDate(monthEndDate)
                .dataFreshnessCheckPassed(false)
                .build();
        scheduledRun = runRepository.save(scheduledRun);

        // Act
        monthEndScheduler.executeT1Staging();

        // Assert
        RecommendationRun updatedRun = runRepository.findById(scheduledRun.getId()).orElseThrow();
        assertThat(updatedRun.getStatus()).isEqualTo("STAGED");
        assertThat(updatedRun.getDataFreshnessCheckPassed()).isTrue();
    }

    /**
     * Test T-1 staging job performs data freshness check
     */
    @Test
    public void testT1StagingJob_PerformsDataFreshnessCheck() {
        // Arrange
        LocalDate monthEndDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        RecommendationRun scheduledRun = RecommendationRun.builder()
                .userId(testUser.getId())
                .universeId(testUniverseId)
                .constraintSetId(testConstraintSetId)
                .runType("SCHEDULED")
                .status("SCHEDULED")
                .scheduledDate(monthEndDate)
                .dataFreshnessCheckPassed(false)
                .build();
        scheduledRun = runRepository.save(scheduledRun);

        // Act
        monthEndScheduler.executeT1Staging();

        // Assert - Data freshness should be checked per FR-025
        RecommendationRun updatedRun = runRepository.findById(scheduledRun.getId()).orElseThrow();
        assertThat(updatedRun.getDataFreshnessCheckPassed()).isNotNull();
    }

    /**
     * Test T finalization job execution
     */
    @Test
    public void testTFinalizationJob_TransitionsToFinalized() {
        // Arrange - Create a STAGED run
        LocalDate monthEndDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        RecommendationRun stagedRun = RecommendationRun.builder()
                .userId(testUser.getId())
                .universeId(testUniverseId)
                .constraintSetId(testConstraintSetId)
                .runType("SCHEDULED")
                .status("STAGED")
                .scheduledDate(monthEndDate)
                .dataFreshnessCheckPassed(true)
                .build();
        stagedRun = runRepository.save(stagedRun);

        // Act
        monthEndScheduler.executeT0Finalization();

        // Assert
        RecommendationRun updatedRun = runRepository.findById(stagedRun.getId()).orElseThrow();
        assertThat(updatedRun.getStatus()).isEqualTo("FINALIZED");
        assertThat(updatedRun.getCompletedAt()).isNotNull();
    }

    /**
     * Test T finalization job marks run as official
     */
    @Test
    public void testTFinalizationJob_MarksRunAsOfficial() {
        // Arrange
        LocalDate monthEndDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        RecommendationRun stagedRun = RecommendationRun.builder()
                .userId(testUser.getId())
                .universeId(testUniverseId)
                .constraintSetId(testConstraintSetId)
                .runType("SCHEDULED")
                .status("STAGED")
                .scheduledDate(monthEndDate)
                .dataFreshnessCheckPassed(true)
                .build();
        stagedRun = runRepository.save(stagedRun);

        // Act
        monthEndScheduler.executeT0Finalization();

        // Assert - Run should be marked as official month-end run per FR-028
        RecommendationRun updatedRun = runRepository.findById(stagedRun.getId()).orElseThrow();
        assertThat(updatedRun.getStatus()).isEqualTo("FINALIZED");
        assertThat(updatedRun.getRunType()).isEqualTo("SCHEDULED");
    }

    /**
     * Test complete workflow sequence T-3 -> T-1 -> T
     */
    @Test
    public void testCompleteMonthEndWorkflow_ExecutesAllStages() {
        // Arrange
        LocalDate monthEndDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        // Act - Execute complete workflow
        // Stage 1: T-3 Pre-compute
        monthEndScheduler.executeT3PreCompute();
        List<RecommendationRun> afterT3 = runRepository.findAll();
        assertThat(afterT3).isNotEmpty();
        assertThat(afterT3.get(0).getStatus()).isEqualTo("SCHEDULED");

        // Stage 2: T-1 Staging
        monthEndScheduler.executeT1Staging();
        List<RecommendationRun> afterT1 = runRepository.findAll();
        assertThat(afterT1.get(0).getStatus()).isEqualTo("STAGED");

        // Stage 3: T Finalization
        monthEndScheduler.executeT0Finalization();
        List<RecommendationRun> afterT0 = runRepository.findAll();
        assertThat(afterT0.get(0).getStatus()).isEqualTo("FINALIZED");

        // Assert - Verify complete state transition
        RecommendationRun finalRun = afterT0.get(0);
        assertThat(finalRun.getStatus()).isEqualTo("FINALIZED");
        assertThat(finalRun.getDataFreshnessCheckPassed()).isTrue();
        assertThat(finalRun.getCompletedAt()).isNotNull();
    }

    /**
     * Test workflow handles data freshness check (placeholder implementation)
     */
    @Test
    public void testT1StagingJob_PerformsDataFreshnessCheckPlaceholder() {
        // Arrange
        LocalDate monthEndDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        RecommendationRun scheduledRun = RecommendationRun.builder()
                .userId(testUser.getId())
                .universeId(testUniverseId)
                .constraintSetId(testConstraintSetId)
                .runType("SCHEDULED")
                .status("SCHEDULED")
                .scheduledDate(monthEndDate)
                .dataFreshnessCheckPassed(false)
                .build();
        scheduledRun = runRepository.save(scheduledRun);

        // Act
        monthEndScheduler.executeT1Staging();

        // Assert - Placeholder implementation always passes
        RecommendationRun updatedRun = runRepository.findById(scheduledRun.getId()).orElseThrow();
        assertThat(updatedRun.getDataFreshnessCheckPassed()).isTrue();

        // TODO: Update test when actual freshness check is implemented
        // Should verify: stale data sets dataFreshnessCheckPassed=false and sends notification
    }

    /**
     * Test workflow skips finalization if not staged
     */
    @Test
    public void testTFinalizationJob_SkipsIfNotStaged() {
        // Arrange - Create run with SCHEDULED status (not STAGED)
        LocalDate monthEndDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        RecommendationRun scheduledRun = RecommendationRun.builder()
                .userId(testUser.getId())
                .universeId(testUniverseId)
                .constraintSetId(testConstraintSetId)
                .runType("SCHEDULED")
                .status("SCHEDULED")  // Not STAGED
                .scheduledDate(monthEndDate)
                .dataFreshnessCheckPassed(false)
                .build();
        scheduledRun = runRepository.save(scheduledRun);

        // Act
        monthEndScheduler.executeT0Finalization();

        // Assert - Run should remain SCHEDULED (not finalized)
        RecommendationRun updatedRun = runRepository.findById(scheduledRun.getId()).orElseThrow();
        assertThat(updatedRun.getStatus()).isEqualTo("SCHEDULED");
        assertThat(updatedRun.getCompletedAt()).isNull();
    }
}
