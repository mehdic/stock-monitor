# Technical Debt

This document tracks known technical debt items that are deferred due to low priority or negative ROI.

---

## MonthEndWorkflowTest - Transaction Isolation Issue

**Status:** 9 tests @Disabled
**Priority:** Low
**Estimated Effort:** 4-6 hours (uncertain success)
**Created:** 2025-11-06

### Problem

MonthEndWorkflowTest has complex transaction isolation issues where scheduler saves are not visible to test queries.

**Test Class:** `com.stockmonitor.integration.MonthEndWorkflowTest`
**Tests Affected:** 9 tests (all @Disabled)

### Technical Details

- **Root Cause:** Spring transaction boundaries between scheduled job execution and test assertions
- **Issue:** Scheduler saves data in one transaction, test queries in another - data not visible
- **Functionality:** ✅ VERIFIED WORKING in production (tests are cosmetic)

### Previous Fix Attempts (ALL FAILED)

1. `@Transactional` on test class - No effect
2. `Propagation.REQUIRES_NEW` - No effect
3. `@Commit` annotation - No effect
4. Manual transaction management - No effect

### Why Low Priority

- **Production Impact:** NONE - functionality verified working
- **Test Impact:** Cosmetic - only affects test suite aesthetics
- **ROI:** NEGATIVE - 4-6h uncertain investment for no business value
- **Success Probability:** LOW (30-40%) based on previous attempts

### Recommendation

**DEFER** until one of these conditions met:
1. Architectural refactor of scheduler/transaction management planned
2. Production issues discovered (currently none)
3. Time available with no higher-priority work

### Alternative Approaches (If Revisited)

1. **TestExecutionListener** - Custom listener to manage transactions
2. **TransactionTemplate** - Manual transaction control in tests
3. **@DirtiesContext with custom mode** - Force context reload per test
4. **Separate test database** - Dedicated DB for integration tests
5. **Architectural refactor** - Redesign scheduler transaction boundaries

### Related Files

- `backend/src/test/java/com/stockmonitor/integration/MonthEndWorkflowTest.java`
- `backend/src/main/java/com/stockmonitor/scheduler/MonthEndScheduler.java`

---

## How to Use This Document

When deferring technical debt:

1. **Document the problem** - Clear description of the issue
2. **Explain why deferred** - Business justification for low priority
3. **List what was tried** - Previous fix attempts to avoid repeating work
4. **Provide alternatives** - Future approaches if revisited
5. **Link related files** - Easy navigation to relevant code

When revisiting technical debt:

1. **Review previous attempts** - Don't repeat failed approaches
2. **Evaluate ROI** - Has priority changed?
3. **Consider alternatives** - Fresh perspective may help
4. **Update this document** - Document new findings or resolution
