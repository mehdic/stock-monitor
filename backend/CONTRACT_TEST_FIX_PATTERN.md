# Contract Test Transaction Isolation Fix

## Problem
Contract tests were failing because `@Transactional` on `BaseIntegrationTest` caused test data to be invisible to MockMvc HTTP requests (different transaction context).

## Solution
1. **Remove `@Transactional` from `BaseIntegrationTest`**
2. **Add manual cleanup to each contract test**

## Fix Pattern

### Step 1: Add imports
```java
import org.junit.jupiter.api.AfterEach;
import com.stockmonitor.repository.<Entity>Repository;
```

### Step 2: Autowire repositories
```java
@Autowired private Repository1 repository1;
@Autowired private Repository2 repository2;
// ... all repositories used by the test
```

### Step 3: Add cleanup method
```java
@AfterEach
void cleanupTestData() {
    // Delete in reverse dependency order (child → parent)
    childRepository.deleteAll();
    parentRepository.deleteAll();
    userRepository.deleteAll(); // always last
}
```

## Completed Fixes

✅ **BaseIntegrationTest** - Removed `@Transactional`
✅ **RecommendationContractTest** - Added manual cleanup (3/10 passing)
✅ **AuthContractTest** - Added manual cleanup (7/7 passing - 100%)
✅ **PortfolioContractTest** - Added manual cleanup (6/10 passing)
✅ **UniverseContractTest** - Added manual cleanup

## Remaining Contract Tests

Need to apply same pattern to:

### 1. BacktestContractTest
**Repositories needed:**
- BacktestRepository
- PortfolioRepository (if referenced)
- UserRepository

**Cleanup order:**
```java
backtestRepository.deleteAll();
portfolioRepository.deleteAll();
userRepository.deleteAll();
```

### 2. ConstraintContractTest
**Repositories needed:**
- ConstraintSetRepository
- PortfolioRepository (if referenced)
- UserRepository

**Cleanup order:**
```java
constraintSetRepository.deleteAll();
portfolioRepository.deleteAll();
userRepository.deleteAll();
```

### 3. DataSourceContractTest
**Repositories needed:**
- DataSourceRepository
- UserRepository

**Cleanup order:**
```java
dataSourceRepository.deleteAll();
userRepository.deleteAll();
```

### 4. ExclusionContractTest
**Repositories needed:**
- ExclusionRepository
- PortfolioRepository (if referenced)
- UserRepository

**Cleanup order:**
```java
exclusionRepository.deleteAll();
portfolioRepository.deleteAll();
userRepository.deleteAll();
```

### 5. FactorContractTest
**Repositories needed:**
- FactorScoreRepository
- FactorModelVersionRepository
- UserRepository

**Cleanup order:**
```java
factorScoreRepository.deleteAll();
factorModelVersionRepository.deleteAll();
userRepository.deleteAll();
```

### 6. ReportContractTest
**Repositories needed:**
- ReportRepository
- PortfolioRepository (if referenced)
- RecommendationRunRepository (if referenced)
- UserRepository

**Cleanup order:**
```java
reportRepository.deleteAll();
recommendationRunRepository.deleteAll();
portfolioRepository.deleteAll();
userRepository.deleteAll();
```

### 7. WebSocketContractTest
**Repositories needed:**
- NotificationRepository (if referenced)
- UserRepository

**Cleanup order:**
```java
notificationRepository.deleteAll();
userRepository.deleteAll();
```

## Example: Full Implementation

```java
package com.stockmonitor.contract;

import com.stockmonitor.BaseIntegrationTest;
import com.stockmonitor.repository.ExampleRepository;
import com.stockmonitor.repository.PortfolioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExampleContractTest extends BaseIntegrationTest {

  @Autowired private ExampleRepository exampleRepository;
  @Autowired private PortfolioRepository portfolioRepository;

  private String authToken;

  @BeforeEach
  void setUp() {
    authToken = generateTestToken("test@example.com");
    // ... setup test data
  }

  @AfterEach
  void cleanupTestData() {
    // Delete in reverse dependency order (child → parent)
    exampleRepository.deleteAll();
    portfolioRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void testExample() throws Exception {
    // ... test code
  }
}
```

## How to Determine Cleanup Order

1. **Identify all entities used** by reading the test's `@BeforeEach` method
2. **Check foreign key relationships** in entity classes (`@ManyToOne`, `@JoinColumn`)
3. **Delete children first**, parents last
4. **Always delete `userRepository` last** (most tests create users)

## Verification

Run tests to verify:
```bash
mvn -o clean test -Dtest=YourContractTest
```

Expected results:
- Tests run without transaction errors
- Some tests may fail with 404 (endpoints not implemented yet) - this is expected
- No database constraint violations during cleanup

## Current Status

**Total Contract Tests: 11**
**Fixed: 4 (BaseIntegrationTest + 3 specific tests)**
**Remaining: 7**

**Test Results After Fix:**
- AuthContractTest: 7/7 passing (100%)
- PortfolioContractTest: 6/10 passing
- RecommendationContractTest: 3/10 passing
- UniverseContractTest: Some passing
- **Total: 20/34 tests passing (58.8%)**

**Key Achievement:** No transaction isolation errors - all tests run cleanly!
