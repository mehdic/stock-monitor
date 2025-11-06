# 404 Error Analysis

## Critical (Blocking Main Workflow)
These prevent basic user journeys from working:

1. **POST /api/runs** (2 failures)
   - Error: "Portfolio not found"
   - Tests: `testTriggerRecommendationRun_Success`, `testTriggerRecommendationRun_DataNotFresh`
   - Impact: Cannot trigger recommendation runs

2. **GET /api/runs/{id}** (1 failure)
   - Error: "Recommendation run not found"
   - Test: `testGetRunStatus_Success`
   - Impact: Cannot check run status

3. **GET /api/runs/{id}/recommendations** (6 failures)
   - Error: "Recommendation run not found"
   - Tests: Multiple in RecommendationContractTest
   - Impact: Cannot retrieve recommendations

4. **GET /api/portfolios/{id}** (1 failure)
   - Error: "Portfolio not found"
   - Test: `testGetPortfolio_Success`
   - Impact: Cannot view portfolio details

5. **GET /api/portfolios/{id}/performance** (2 failures)
   - Error: "Portfolio not found" OR missing data
   - Tests: `testGetPortfolioPerformance_Success`, `testGetPortfolioPerformance_ContributorsIncludeDetails`
   - Impact: Cannot see portfolio performance metrics

## Defer-able (Advanced Features)
These are nice-to-have but not blocking:

1. **GET /api/backtests/{id}** - Backtest results viewing
2. **GET /api/holdings/{id}/factors** - Factor breakdown for holdings
3. **POST /api/portfolios/{id}/constraints/preview** - Constraint preview before save
4. **POST /api/portfolios/{id}/constraints/reset** - Reset constraints to defaults
5. **POST /api/portfolios/{id}/constraints** - Save constraint modifications
6. **GET /api/universes/{id}** - Universe details
7. **POST /api/portfolios/{id}/universe** - Select universe for portfolio

## Root Causes

Most 404s stem from:
1. **Missing portfolio auto-creation** - Tests expect portfolio to exist for authenticated user
2. **Missing endpoints** - Some controller methods not implemented
3. **Test data not properly seeded** - Tests use fake UUIDs that don't exist

## Recommendation for Step 1.3

Implement these 3 endpoints to fix the most critical failures:

1. **GET /api/portfolios/{id}/performance** - Fix 2-3 PortfolioContractTest failures
2. **POST /api/runs** - Fix 2 RecommendationContractTest failures (trigger run)
3. **GET /api/runs/{id}/recommendations** - Fix 6 RecommendationContractTest failures

This will take us from ~70% passing to ~80%+ passing.
