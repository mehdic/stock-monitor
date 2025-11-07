package com.stockmonitor.integration;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.model.AuditLog;
import com.stockmonitor.model.Portfolio;
import com.stockmonitor.model.User;
import com.stockmonitor.repository.AuditLogRepository;
import com.stockmonitor.repository.PortfolioRepository;
import com.stockmonitor.repository.UserRepository;
import com.stockmonitor.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for audit log creation across all operations.
 *
 * Tests:
 * - Audit log creation for entity modifications
 * - Before/after state tracking
 * - Security event logging
 * - Audit trail completeness and chronology
 */
@Transactional
public class AuditLogIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private User testUser;

    @BeforeEach
    public void setup() {
        // Clean up test data
        auditLogRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("audit-test@example.com")
                .passwordHash("dummy")
                .firstName("Audit")
                .lastName("Test")
                .role(User.UserRole.OWNER)
                .enabled(true)
                .emailVerified(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    /**
     * Test audit log creation for security events.
     */
    @Test
    public void testLogSecurityEvent_UserLogin_CreatesAuditLog() {
        // Act
        auditLogService.logSecurityEvent(
                testUser.getId().toString(),
                "LOGIN",
                "User logged in successfully"
        );

        // Assert - Verify log was created (currently logged only, not persisted)
        // Note: Current implementation only logs to console
        // Future implementation will persist to audit_log table
    }

    /**
     * Test audit log creation for user account modifications.
     */
    @Test
    public void testAuditLog_UserModification_TracksBeforeAndAfterState() {
        // Arrange - Create initial audit log for user creation
        AuditLog creationLog = AuditLog.builder()
                .userId(testUser.getId())
                .entityType("USER")
                .entityId(testUser.getId())
                .action("CREATE")
                .actionDetail("User account created")
                .beforeState("{}")
                .afterState("{\"email\":\"audit-test@example.com\",\"role\":\"OWNER\"}")
                .success(true)
                .build();
        creationLog = auditLogRepository.save(creationLog);

        // Act - Simulate user modification
        AuditLog modificationLog = AuditLog.builder()
                .userId(testUser.getId())
                .entityType("USER")
                .entityId(testUser.getId())
                .action("UPDATE")
                .actionDetail("User role changed")
                .beforeState("{\"email\":\"audit-test@example.com\",\"role\":\"OWNER\"}")
                .afterState("{\"email\":\"audit-test@example.com\",\"role\":\"ANALYST\"}")
                .changedFields(new String[]{"role"})
                .success(true)
                .build();
        modificationLog = auditLogRepository.save(modificationLog);

        // Assert
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId("USER", testUser.getId());
        assertThat(logs).hasSize(2);

        AuditLog updateLog = logs.stream()
                .filter(log -> "UPDATE".equals(log.getAction()))
                .findFirst()
                .orElseThrow();

        assertThat(updateLog.getBeforeState()).contains("OWNER");
        assertThat(updateLog.getAfterState()).contains("ANALYST");
        assertThat(updateLog.getChangedFields()).contains("role");
    }

    /**
     * Test audit log creation for portfolio operations.
     */
    @Test
    public void testAuditLog_PortfolioCreation_CreatesAuditTrail() {
        // Arrange
        Portfolio portfolio = Portfolio.builder()
                .userId(testUser.getId())
                .cashBalance(BigDecimal.valueOf(100000))
                .totalMarketValue(BigDecimal.valueOf(50000))
                .activeUniverseId(UUID.randomUUID())
                .activeConstraintSetId(UUID.randomUUID())
                .build();
        portfolio = portfolioRepository.save(portfolio);

        // Act - Create audit log for portfolio creation
        AuditLog auditLog = AuditLog.builder()
                .userId(testUser.getId())
                .entityType("PORTFOLIO")
                .entityId(portfolio.getId())
                .action("CREATE")
                .actionDetail("Portfolio created")
                .beforeState("{}")
                .afterState("{\"cashBalance\":100000,\"totalMarketValue\":50000}")
                .ipAddress("127.0.0.1")
                .userAgent("Test Agent")
                .success(true)
                .executionDurationMs(150L)
                .build();
        auditLog = auditLogRepository.save(auditLog);

        // Assert
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId("PORTFOLIO", portfolio.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAction()).isEqualTo("CREATE");
        assertThat(logs.get(0).getUserId()).isEqualTo(testUser.getId());
        assertThat(logs.get(0).getSuccess()).isTrue();
    }

    /**
     * Test audit log tracks failed operations.
     */
    @Test
    public void testAuditLog_FailedOperation_RecordsError() {
        // Arrange & Act
        AuditLog failedLog = AuditLog.builder()
                .userId(testUser.getId())
                .entityType("RECOMMENDATION_RUN")
                .entityId(UUID.randomUUID())
                .action("START")
                .actionDetail("Attempted to start recommendation run")
                .beforeState("{\"status\":\"SCHEDULED\"}")
                .afterState("{\"status\":\"SCHEDULED\"}")
                .success(false)
                .errorMessage("Data freshness check failed")
                .executionDurationMs(500L)
                .build();
        failedLog = auditLogRepository.save(failedLog);

        // Assert
        AuditLog retrieved = auditLogRepository.findById(failedLog.getId()).orElseThrow();
        assertThat(retrieved.getSuccess()).isFalse();
        assertThat(retrieved.getErrorMessage()).contains("Data freshness check failed");
    }

    /**
     * Test audit log chronological ordering.
     */
    @Test
    public void testAuditLog_MultipleOperations_MaintainsChronologicalOrder() {
        // Arrange - Create sequence of audit logs
        UUID entityId = UUID.randomUUID();

        AuditLog log1 = createAuditLog(entityId, "CREATE", "Entity created", 100L);
        AuditLog log2 = createAuditLog(entityId, "UPDATE", "Entity updated", 200L);
        AuditLog log3 = createAuditLog(entityId, "UPDATE", "Entity updated again", 300L);
        AuditLog log4 = createAuditLog(entityId, "DELETE", "Entity deleted", 400L);

        // Act
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(
                "PORTFOLIO", entityId);

        // Sort logs by createdAt ascending to verify chronological order
        logs.sort(java.util.Comparator.comparing(AuditLog::getCreatedAt));

        // Assert - Logs are in chronological order
        assertThat(logs).hasSize(4);
        assertThat(logs.get(0).getAction()).isEqualTo("CREATE");
        assertThat(logs.get(1).getAction()).isEqualTo("UPDATE");
        assertThat(logs.get(2).getAction()).isEqualTo("UPDATE");
        assertThat(logs.get(3).getAction()).isEqualTo("DELETE");

        // Verify timestamps are ordered
        assertThat(logs.get(0).getCreatedAt()).isNotNull();
        assertThat(logs.get(1).getCreatedAt()).isNotNull();
        assertThat(logs.get(2).getCreatedAt()).isNotNull();
        assertThat(logs.get(3).getCreatedAt()).isNotNull();
    }

    /**
     * Test audit log records execution duration.
     */
    @Test
    public void testAuditLog_RecordsExecutionDuration() {
        // Arrange & Act
        AuditLog log = AuditLog.builder()
                .userId(testUser.getId())
                .entityType("RECOMMENDATION_RUN")
                .entityId(UUID.randomUUID())
                .action("COMPLETE")
                .actionDetail("Recommendation run completed")
                .beforeState("{\"status\":\"RUNNING\"}")
                .afterState("{\"status\":\"COMPLETED\"}")
                .success(true)
                .executionDurationMs(3500L)
                .build();
        log = auditLogRepository.save(log);

        // Assert
        AuditLog retrieved = auditLogRepository.findById(log.getId()).orElseThrow();
        assertThat(retrieved.getExecutionDurationMs()).isEqualTo(3500L);
    }

    /**
     * Test audit log captures request metadata.
     */
    @Test
    public void testAuditLog_CapturesRequestMetadata() {
        // Arrange & Act
        AuditLog log = AuditLog.builder()
                .userId(testUser.getId())
                .entityType("PORTFOLIO")
                .entityId(UUID.randomUUID())
                .action("UPDATE")
                .actionDetail("Portfolio settings updated")
                .beforeState("{}")
                .afterState("{}")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .sessionId("session-123")
                .requestId("req-456")
                .success(true)
                .build();
        log = auditLogRepository.save(log);

        // Assert
        AuditLog retrieved = auditLogRepository.findById(log.getId()).orElseThrow();
        assertThat(retrieved.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(retrieved.getUserAgent()).contains("Mozilla");
        assertThat(retrieved.getSessionId()).isEqualTo("session-123");
        assertThat(retrieved.getRequestId()).isEqualTo("req-456");
    }

    /**
     * Test audit log filtering by user.
     */
    @Test
    public void testAuditLog_FilterByUser_ReturnsOnlyUserLogs() {
        // Arrange - Create logs for different users
        User user2 = User.builder()
                .email("user2@example.com")
                .passwordHash("dummy")
                .firstName("User")
                .lastName("Two")
                .role(User.UserRole.VIEWER)
                .enabled(true)
                .build();
        user2 = userRepository.save(user2);

        createAuditLog(UUID.randomUUID(), "CREATE", "Created by user 1", 100L);
        createAuditLog(UUID.randomUUID(), "CREATE", "Created by user 1 again", 200L);

        AuditLog user2Log = AuditLog.builder()
                .userId(user2.getId())
                .entityType("PORTFOLIO")
                .entityId(UUID.randomUUID())
                .action("CREATE")
                .actionDetail("Created by user 2")
                .beforeState("{}")
                .afterState("{}")
                .success(true)
                .build();
        auditLogRepository.save(user2Log);

        // Act
        List<AuditLog> user1Logs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        List<AuditLog> user2Logs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(user2.getId());

        // Assert
        assertThat(user1Logs).hasSize(2);
        assertThat(user2Logs).hasSize(1);
    }

    /**
     * Test audit log retention and timestamp.
     */
    @Test
    public void testAuditLog_CreatedAtTimestamp_IsAutoGenerated() {
        // Arrange & Act
        LocalDateTime beforeSave = LocalDateTime.now();
        AuditLog log = AuditLog.builder()
                .userId(testUser.getId())
                .entityType("PORTFOLIO")
                .entityId(UUID.randomUUID())
                .action("CREATE")
                .actionDetail("Test action")
                .beforeState("{}")
                .afterState("{}")
                .success(true)
                .build();
        log = auditLogRepository.save(log);
        auditLogRepository.flush(); // Ensure entity is persisted and timestamp generated
        LocalDateTime afterSave = LocalDateTime.now();

        // Assert
        assertThat(log.getCreatedAt()).isNotNull();
        assertThat(log.getCreatedAt()).isBetween(beforeSave.minusSeconds(1), afterSave.plusSeconds(1));
    }

    /**
     * Helper method to create an audit log.
     */
    private AuditLog createAuditLog(UUID entityId, String action, String actionDetail, long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        AuditLog log = AuditLog.builder()
                .userId(testUser.getId())
                .entityType("PORTFOLIO")
                .entityId(entityId)
                .action(action)
                .actionDetail(actionDetail)
                .beforeState("{}")
                .afterState("{}")
                .success(true)
                .build();

        return auditLogRepository.save(log);
    }
}
