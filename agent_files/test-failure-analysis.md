# Test Failure Analysis - 2025-11-05

## Current Status
- **Total Tests**: 183
- **Passing**: 147 (80.3%)
- **Failing**: 36 (19.7%)
  - Failures: 21
  - Errors: 15

## Completed Fixes

### 1. H2 Database Schema Compatibility ✅
**Problem**: H2 doesn't support PostgreSQL-specific types (`jsonb`, `text[]`)

**Solution**:
- Removed `columnDefinition = "jsonb"` and replaced with `columnDefinition = "TEXT"`
- Allowed H2's native array handling for `String[]` fields
- Updated 7 entity classes:
  - AuditLog
  - User
  - FactorScore
  - FactorModelVersion
  - Backtest
  - Universe
  - Report

**Result**: Schema creation now succeeds for all tests

### 2. Test Data Infrastructure ✅
**Added**:
- `TestDataHelper.createTestUniverse()` method
- `TestDataHelper.createTestFactorModelVersion()` method
- `TestDataInitializer` class to auto-create 3 universes and 1 factor model version

**Benefit**: Provides foundation for consistent test data across all tests

## Remaining Failures

### Category 1: Contract Tests (22 failures)

**Affected Tests**:
- BacktestContractTest: 3 tests
- ConstraintContractTest: 3 tests
- FactorContractTest: 1 test
- PortfolioContractTest: 4 tests
- RecommendationContractTest: 7 tests
- UniverseContractTest: 3 tests

**Root Cause**: Tests expect specific hardcoded UUIDs that don't exist

**Example**:
```java
// UniverseContractTest.java:166
String universeId = "00000000-0000-0000-0000-000000000001";
mockMvc.perform(get("/api/universes/" + universeId))
    .andExpect(status().isOk()) // Fails with 404
```

**Error**: `Status expected:<200> but was:<404>`

**Solutions**:
1. **Option A** (Recommended): Add `@BeforeEach` setup to create test data with known IDs
   ```java
   @BeforeEach
   void setUp() {
       universe = testDataHelper.createTestUniverseWithId(
           UUID.fromString("00000000-0000-0000-0000-000000000001"),
           "S&P 500"
       );
   }
   ```

2. **Option B**: Refactor tests to query for actual IDs instead of hardcoding
   ```java
   Universe universe = universeRepository.findAll().get(0);
   String universeId = universe.getId().toString();
   ```

3. **Option C**: Create global fixtures with specific IDs in TestDataInitializer

### Category 2: MonthEndWorkflowTest (9 errors)

**Affected Tests**:
- testT1StagingJob_PerformsDataFreshnessCheck
- testT1StagingJob_TransitionsToStaged
- testT3PreComputeJob_IsIdempotent
- testT3PreComputeJob_CreatesScheduledRuns
- testTFinalizationJob_TransitionsToFinalized
- testTFinalizationJob_SkipsIfNotStaged
- testTFinalizationJob_MarksRunAsOfficial
- testCompleteMonthEndWorkflow_ExecutesAllStages
- testT1StagingJob_PerformsDataFreshnessCheckPlaceholder

**Root Cause**: Transaction isolation - data created in one transaction isn't visible in another

**Error**: Tests create RecommendationRun entities but can't query them due to test transaction rollback

**Solution**:
```java
@Test
@Transactional(propagation = Propagation.NOT_SUPPORTED) // Run outside transaction
public void testT3PreComputeJob_CreatesScheduledRuns() {
    // Test code runs without automatic rollback
    // Manually clean up data in @AfterEach
}
```

Or use `@Commit` to commit the test transaction:
```java
@Test
@Commit  // Don't rollback this transaction
public void testT3PreComputeJob_CreatesScheduledRuns() {
    // Data will be committed and visible
}
```

### Category 3: RestTemplate Authentication Issues (4 errors)

**Affected Tests**:
- OnboardingFlowTest.testCompleteOnboardingFlow
- OnboardingFlowTest.testLoginWithWrongPassword
- OffCycleIsolationTest.testOffCycleRunIsolation
- OffCycleIsolationTest.testServiceRoleCanOnlyTriggerScheduledRuns

**Error**:
```
ResourceAccessException: I/O error on POST request for "http://localhost:XXX/api/...":
cannot retry due to server authentication, in streaming mode
```

**Root Cause**: RestTemplate tries to retry with authentication but request body has been consumed

**Solution**: Configure RestTemplate with buffering
```java
@Bean
public RestTemplate testRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setRequestFactory(
        new BufferingClientHttpRequestFactory(
            new SimpleClientHttpRequestFactory()
        )
    );
    return restTemplate;
}
```

Or refactor tests to avoid scenarios that trigger retry logic.

### Category 4: Missing Test Setup (2 errors)

**Affected Tests**:
- ConstraintModificationTest.testConstraintPersistence
- ConstraintModificationTest.testResetToDefaults

**Solution**: Add `@BeforeEach` method to create necessary test data
```java
@BeforeEach
void setUp() {
    testUser = testDataHelper.createTestUser("test@example.com");
    testPortfolio = testDataHelper.createTestPortfolio(testUser.getId());
}
```

## Recommended Fix Order

1. **Fix Contract Tests** (22 tests) - Highest impact
   - Add @BeforeEach setup with known IDs
   - Should take ~2-3 hours

2. **Fix MonthEndWorkflowTest** (9 tests)
   - Add transaction management annotations
   - Should take ~1 hour

3. **Fix RestTemplate Issues** (4 tests)
   - Configure request buffering
   - Should take ~1 hour

4. **Fix ConstraintModificationTest** (2 tests)
   - Add test setup
   - Should take ~30 minutes

**Total Estimated Time to 100%**: 4-5 hours

## Files Modified

### Schema Compatibility
- `backend/src/main/java/com/stockmonitor/model/AuditLog.java`
- `backend/src/main/java/com/stockmonitor/model/User.java`
- `backend/src/main/java/com/stockmonitor/model/FactorScore.java`
- `backend/src/main/java/com/stockmonitor/model/FactorModelVersion.java`
- `backend/src/main/java/com/stockmonitor/model/Backtest.java`
- `backend/src/main/java/com/stockmonitor/model/Universe.java`
- `backend/src/main/java/com/stockmonitor/model/Report.java`

### Test Infrastructure
- `backend/src/test/java/com/stockmonitor/helper/TestDataHelper.java`
- `backend/src/test/java/com/stockmonitor/config/TestDataInitializer.java` (new)

## Git Commits
1. `70d214a4` - Fix H2 database compatibility for JSON and array columns
2. `41e60414` - Add test data initialization for Universe and FactorModelVersion

## Next Session

To continue from here:
1. Read this analysis document
2. Start with fixing contract tests (highest ROI)
3. Use the solution patterns documented above
4. Test incrementally after each fix
