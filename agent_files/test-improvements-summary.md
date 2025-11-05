# Contract Test Data Setup and Error Handling Improvements

**Date**: 2025-11-05
**Task**: Improve test data setup and error handling for contract tests

---

## Problems Identified

### 1. Missing Test Data
Contract tests were creating incomplete entity graphs:
- **ReportContractTest**: Created RecommendationRun but NO recommendations
- **RecommendationContractTest**: Used hardcoded UUIDs assuming portfolios/universes existed
- **BacktestContractTest**: Used hardcoded portfolio UUIDs
- **PortfolioContractTest**: Used hardcoded portfolio UUIDs

**Result**: Tests failed with 404 errors or received empty data responses.

### 2. Service Error Handling Gaps
- **ReportGenerationService.buildSummary()**: Risk of divide-by-zero when recommendations list is empty
- **BacktestService.getBacktest()**: Returned null instead of throwing exception for missing backtests

---

## Solutions Implemented

### 1. Created TestDataHelper Utility Class

**File**: `backend/src/test/java/com/stockmonitor/util/TestDataHelper.java`

**Purpose**: Centralized, reusable test data creation for contract tests

**Key Methods**:

```java
public TestDataContext createCompleteTestSetup(UUID userId)
```
Creates complete entity graph in correct order:
- Universe (with 10 test stocks: AAPL, MSFT, GOOGL, AMZN, TSLA, NVDA, META, BRK.B, JPM, JNJ)
- ConstraintSet (with realistic constraints)
- Portfolio (linked to universe and constraints)
- Holdings (3 sample positions with realistic prices)

```java
public RecommendationRun createCompletedRunWithRecommendations(TestDataContext context)
```
Creates completed run with 5 ranked recommendations:
- Status: COMPLETED
- 5 recommendations with realistic factor scores
- Proper ranking (1-5)
- Realistic alpha/cost estimates

**Benefits**:
- Single method call creates all required test data
- Consistent data across all contract tests
- Realistic sample data (not empty/stub data)
- Proper entity relationships maintained

---

### 2. Updated Contract Tests

#### ReportContractTest
**Before**:
```java
RecommendationRun run = RecommendationRun.builder()
    .userId(UUID.randomUUID())  // Random, no actual user
    .universeId(UUID.randomUUID())  // Random, no actual universe
    .constraintSetId(UUID.randomUUID())  // Random, no actual constraints
    .build();
// No recommendations created!
```

**After**:
```java
TestDataContext context = testDataHelper.createCompleteTestSetup(userId);
RecommendationRun run = testDataHelper.createCompletedRunWithRecommendations(context);
// Now has: portfolio, universe, constraints, 5 recommendations
```

**Result**: Tests now have real data to generate reports from.

---

#### RecommendationContractTest
**Before**:
```java
portfolioId = "00000000-0000-0000-0000-000000000001";  // Hardcoded, doesn't exist
// Tests assumed portfolio existed
```

**After**:
```java
testContext = testDataHelper.createCompleteTestSetup(testUserId);
portfolioId = testContext.getPortfolio().getId().toString();
completedRunId = completedRun.getId().toString();
// Tests use actual created entities
```

**Result**: Tests operate on real portfolios that exist in test database.

---

#### BacktestContractTest
**Before**:
```java
String requestBody = """
{
  "portfolioId": "00000000-0000-0000-0000-000000000001",  // Hardcoded
  ...
}
""";
```

**After**:
```java
String requestBody = String.format("""
{
  "portfolioId": "%s",  // Actual created portfolio
  ...
}
""", portfolioId);
```

**Result**: Backtest requests reference real portfolios.

---

#### PortfolioContractTest
**Before**:
```java
portfolioId = "00000000-0000-0000-0000-000000000001";  // Hardcoded
```

**After**:
```java
testContext = testDataHelper.createCompleteTestSetup(testUserId);
portfolioId = testContext.getPortfolio().getId().toString();
```

**Result**: Portfolio tests operate on real entities.

---

### 3. Added Defensive Error Handling

#### ReportGenerationService.buildSummary()
**Issue**: Divide-by-zero risk when recommendations list is empty

**Fix**:
```java
private ReportDTO.SummarySection buildSummary(List<Recommendation> recommendations) {
    // Handle empty recommendations gracefully
    if (recommendations == null || recommendations.isEmpty()) {
        log.warn("Building summary with empty recommendations list");
        return ReportDTO.SummarySection.builder()
                .totalRecommendations(0)
                .buyCount(0)
                .sellCount(0)
                .holdCount(0)
                .expectedAlphaBps(BigDecimal.ZERO)
                .expectedCostBps(BigDecimal.ZERO)
                .edgeOverCostBps(BigDecimal.ZERO)
                .constraintViolations(0)
                .build();
    }
    // ... normal processing
}
```

**Result**: Service returns zero-filled summary instead of crashing.

---

#### BacktestService.getBacktest()
**Issue**: Returned null when backtest not found, causing NPEs

**Fix**:
```java
public BacktestDTO getBacktest(UUID backtestId) {
    BacktestDTO result = backtestStore.get(backtestId);
    if (result == null) {
        log.error("Backtest not found: {}", backtestId);
        throw new IllegalArgumentException("Backtest not found: " + backtestId);
    }
    return result;
}
```

**Result**: Clear error message instead of null pointer exceptions.

---

## Files Modified

1. **Created**:
   - `backend/src/test/java/com/stockmonitor/util/TestDataHelper.java` (NEW)

2. **Updated**:
   - `backend/src/test/java/com/stockmonitor/contract/ReportContractTest.java`
   - `backend/src/test/java/com/stockmonitor/contract/RecommendationContractTest.java`
   - `backend/src/test/java/com/stockmonitor/contract/BacktestContractTest.java`
   - `backend/src/test/java/com/stockmonitor/contract/PortfolioContractTest.java`
   - `backend/src/main/java/com/stockmonitor/service/ReportGenerationService.java`
   - `backend/src/main/java/com/stockmonitor/service/BacktestService.java`

3. **Documentation**:
   - `agent_files/changelog.log` (updated)
   - `agent_files/test-improvements-summary.md` (this file)

---

## Expected Improvements

### Test Reliability
- ✅ **Fewer 404 errors**: Tests create entities before referencing them
- ✅ **Real data assertions**: Tests verify actual functionality, not stubs
- ✅ **Proper entity relationships**: All foreign keys reference existing entities

### Error Handling
- ✅ **Graceful degradation**: Services handle empty data without crashes
- ✅ **Clear error messages**: Exceptions describe what went wrong
- ✅ **Proper logging**: All error paths include diagnostic logs

### Test Maintainability
- ✅ **Centralized data creation**: One helper class instead of duplicated setup code
- ✅ **Consistent test data**: All tests use same realistic sample data
- ✅ **Easy to extend**: Add new test scenarios by extending TestDataHelper

---

## Known Remaining Issues

These issues were identified but NOT fixed in this change:

1. **UniverseContractTest**: Still uses hardcoded UUIDs (found via grep)
2. **Service stubs**: Some services may return empty lists (e.g., RecommendationEngine stub)
3. **PDF generation**: May fail if template files missing
4. **WebSocket tests**: May need additional setup for real-time testing

**Note**: User requested focus on Priority 1-3 tasks, which have been completed. Remaining issues can be addressed in future work.

---

## Testing Status

**Cannot run tests**: Network/proxy issues prevent Maven test execution.

**Expected when tests run**:
- Contract tests should have fewer failures
- Error messages should be more descriptive
- Tests should create complete, realistic data

**Verification Steps** (when network available):
```bash
# Run all contract tests
mvn test -Dtest=*ContractTest

# Run specific test suites
mvn test -Dtest=ReportContractTest
mvn test -Dtest=RecommendationContractTest
mvn test -Dtest=BacktestContractTest
mvn test -Dtest=PortfolioContractTest
```

---

## Lessons Learned

1. **Test data matters**: Tests can't validate functionality without realistic data
2. **Complete entity graphs**: Create all related entities, not just the target entity
3. **Defensive programming**: Always handle null/empty cases, even in "should never happen" scenarios
4. **Centralized test utilities**: Reusable test helpers improve maintainability
5. **Hardcoded UUIDs are technical debt**: Always create test data dynamically

---

## Next Steps (Future Work)

1. **Run tests** when network available to verify improvements
2. **Update UniverseContractTest** to use TestDataHelper
3. **Implement service stubs** for full test coverage
4. **Add edge case tests** for error handling paths
5. **Performance optimization**: Consider @BeforeAll for shared test data
6. **Add more sample data** as tests reveal additional needs

---

**Summary**: Successfully improved test data setup and error handling across 4 contract test files and 2 service files. Tests now create complete, realistic data instead of using hardcoded UUIDs. Services handle edge cases gracefully instead of crashing. All changes documented and ready for verification when network is available.
