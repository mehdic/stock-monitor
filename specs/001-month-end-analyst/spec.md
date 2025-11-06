# Feature Specification: Month-End Market Analyst

**Feature Branch**: `001-month-end-analyst`
**Created**: 2025-10-30
**Status**: Draft
**Input**: User description: Full-featured stock portfolio analysis and recommendation system with month-end buy recommendations, backtesting, and constraint management

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Initial Portfolio Setup and First Recommendation Run (Priority: P1)

An individual investor signs up, uploads their current holdings, selects their investment universe, and receives their first set of ranked buy recommendations with explanations.

**Why this priority**: This is the core value proposition - getting from zero to actionable recommendations. Without this, the application provides no value. This story represents the minimum viable product.

**Independent Test**: Can be fully tested by creating a new account, uploading a sample portfolio CSV, selecting S&P 500 universe, triggering a manual run, and verifying that ranked recommendations appear with confidence scores and explanations. Success is measured by the user being able to understand and act on at least one recommendation.

**Acceptance Scenarios**:

1. **Given** a new user visits the application, **When** they sign up with valid credentials and confirm email, **Then** they are redirected to the "Add holdings" onboarding step with session persisted
2. **Given** user is on holdings upload page, **When** they upload a CSV with valid holdings (symbol, quantity, cost basis, currency), **Then** portfolio summary appears showing market value, P&L, and holdings coverage percentage for selected universe
3. **Given** user has uploaded holdings, **When** they select investment universe (e.g., S&P 500) and accept default constraints, **Then** dashboard displays with "Run now" button enabled and portfolio benchmarked against selected index
4. **Given** user triggers a manual run, **When** the analysis completes, **Then** recommendations page shows ranked list with target weights, confidence scores, expected costs, and plain-language explanations for each pick
5. **Given** recommendations are displayed, **When** user clicks on a recommendation, **Then** disclosure panel expands showing top 3 drivers, constraint impacts, risk contribution, and cost breakdown

---

### User Story 2 - Understanding and Acting on Month-End Recommendations (Priority: P2)

An existing user receives their scheduled month-end recommendations, reviews the explanations and rationale, downloads the comprehensive report, and understands what changed since last month.

**Why this priority**: This represents the recurring value of the application. Users need to trust and understand recommendations before acting. The download report provides documentation for their investment decisions.

**Independent Test**: Can be tested by triggering a scheduled month-end run for an existing portfolio, verifying notification delivery, checking that recommendations show what changed from previous month, and confirming report downloads with all required sections (summary, picks, exclusions, disclaimers).

**Acceptance Scenarios**:

1. **Given** it is T-3 business days before month-end, **When** pre-compute runs, **Then** user receives notification "Scores calculated for Month-End. Review Analysis for any red flags" and can view factor diagnostics
2. **Given** it is T-1 before month-end, **When** recommendations are staged, **Then** user receives notification "Orders and constraints staged. You can review picks now" and recommendations page shows draft state
3. **Given** it is month-end (T), **When** recommendations are finalized, **Then** user receives notification "Month-End recommendations are ready. Download the report" and recommendations show final state with countdown to next run
4. **Given** recommendations are displayed, **When** user views each pick, **Then** system shows "changed since last run" indicator highlighting new additions, removed positions, and weight changes
5. **Given** user clicks "Download report", **When** report generates, **Then** HTML/PDF includes summary, ranked picks table, exclusions list with reasons, constraint settings, cost estimates, and legal disclaimer with timestamp

---

### User Story 3 - Tuning Constraints and Previewing Impact (Priority: P3)

A user wants to customize their risk parameters by adjusting constraints (turnover cap, sector limits, position sizes) and previewing the impact before committing changes.

**Why this priority**: Power users need flexibility to align recommendations with their risk tolerance and preferences. Preview capability prevents unwanted surprises and builds trust.

**Independent Test**: Can be tested by navigating to Settings, modifying a constraint (e.g., reducing turnover cap from 25% to 20%), clicking "Preview impact", verifying preview shows expected changes to pick count and turnover, then saving changes and confirming next run uses updated constraints.

**Acceptance Scenarios**:

1. **Given** user is in Settings > Constraints, **When** they adjust turnover cap from 25% to 20%, **Then** "Preview impact" shows expected reduction in number of trades and marginal cost change without affecting current recommendations
2. **Given** user modifies sector cap to 15%, **When** they preview, **Then** system shows which current positions would be excluded or reduced due to new cap
3. **Given** user is satisfied with preview, **When** they click Save, **Then** changes are persisted and next run will use updated constraints with previous runs remaining intact for historical comparison
4. **Given** user sets an invalid constraint (e.g., negative turnover), **When** they attempt to save, **Then** validation error displays explaining the issue with "Restore defaults" option
5. **Given** user has customized constraints, **When** they click "Restore defaults", **Then** all constraints reset to recommended values with confirmation prompt

---

### User Story 4 - Portfolio Monitoring and Factor Analysis (Priority: P4)

A user reviews their current portfolio performance, analyzes factor exposures (Value, Momentum, Quality, Revisions) for each holding, and identifies contributors and detractors versus their benchmark.

**Why this priority**: Understanding current portfolio health and factor exposures helps users make informed decisions about recommendations. This provides context for why certain picks are suggested.

**Independent Test**: Can be tested by viewing Dashboard and Analysis pages, verifying performance metrics (P&L, benchmark comparison), checking factor heatmap displays for all holdings, and confirming data freshness indicators show last update times.

**Acceptance Scenarios**:

1. **Given** user opens Dashboard, **When** portfolio loads, **Then** display shows current value, cash balance, P&L vs selected benchmark, and top 5 contributors/detractors
2. **Given** user navigates to Analysis page, **When** factor diagnostics load, **Then** heatmap displays Value, Momentum, Quality, and Revisions scores for each holding with color coding
3. **Given** user hovers over a factor block, **When** tooltip appears, **Then** explanation describes the factor in one sentence and indicates whether "Higher is better" or "Lower is better"
4. **Given** market data is current, **When** data freshness indicators display, **Then** badges show last update time for prices, fundamentals, and estimates with all sources green/current
5. **Given** any data source is stale, **When** user attempts to trigger a run, **Then** "Run now" button is disabled with clear message indicating which source is blocking and when refresh is expected

---

### User Story 5 - Quick Backtesting and Sensitivity Analysis (Priority: P5)

A curious user runs quick backtests to evaluate how their current constraints would have performed historically and adjusts constraints to see sensitivity of recommendations without affecting production settings.

**Why this priority**: Backtesting builds confidence in the strategy and helps users understand trade-offs. This is valuable for power users but not essential for core functionality.

**Independent Test**: Can be tested by selecting a universe preset, date range, and current constraints, running backtest, and verifying results show performance vs benchmark with turnover stats and yes/no verdict on "beat equal weight after costs."

**Acceptance Scenarios**:

1. **Given** user opens Backtests page, **When** they select universe (S&P 500), date range (2 years), and current constraints, **Then** backtest executes and displays equity curve vs benchmark
2. **Given** backtest completes, **When** results display, **Then** statistics show CAGR, volatility, max drawdown, Sharpe ratio, hit rate, and turnover (all net of assumed costs)
3. **Given** backtest results are shown, **When** user views verdict, **Then** one-line answer states "Beat equal weight of selected names after costs? Yes/No" with percentage outperformance or underperformance
4. **Given** user opens Sensitivity preview, **When** they adjust one constraint (e.g., increase name cap from 5% to 7%), **Then** preview shows expected impact on turnover and number of picks without saving
5. **Given** user likes sensitivity results, **When** they click "Apply to constraints", **Then** changes transfer to Settings for review and saving

---

### User Story 6 - Managing Exclusions and Understanding "No Trade" Decisions (Priority: P6)

A user reviews why certain stocks were excluded from recommendations and understands when the system decides to issue "No trade" guidance instead of recommendations.

**Why this priority**: Transparency about exclusions and non-recommendations builds trust. Users need to understand the "why not" as much as the "why" to trust the system's judgment.

**Independent Test**: Can be tested by reviewing a recommendations run where stocks were excluded, clicking "Why not this one?" for excluded symbols, and verifying explanations cite specific rules (liquidity floor, sector cap, earnings proximity, spread thresholds). Also test scenario where expected advantage doesn't cover costs and verify "No trade" guidance appears with explanation.

**Acceptance Scenarios**:

1. **Given** recommendations run completes, **When** user views recommendations page, **Then** "Exclusions" tab shows list of symbols that were analyzed but not recommended
2. **Given** user clicks on an excluded symbol, **When** exclusion details display, **Then** human-readable reason explains which rule blocked it (e.g., "Excluded: Liquidity below floor" or "Excluded: Earnings announcement in 36 hours")
3. **Given** user wants to review exclusions later, **When** they click "Export exclusions", **Then** CSV downloads with symbol, exclusion reason, and date
4. **Given** expected alpha does not exceed estimated cost by safe margin, **When** month-end run completes, **Then** "No trade" message displays explaining conviction is below threshold with suggestion to review constraint settings or wait for next month
5. **Given** constraints make valid portfolio impossible (e.g., caps too restrictive), **When** run attempts to execute, **Then** single banner message states "Constraints prevent a feasible recommendation set. Try relaxing turnover or liquidity floor" with link to Settings

---

### Edge Cases

- What happens when user uploads portfolio with symbols not in selected universe? System shows coverage percentage and lists uncovered symbols with option to expand universe or remove holdings.
- What happens when data source fails during scheduled run? Run is blocked, user receives notification identifying stale source, and manual run remains disabled until data refreshes.
- What happens when user modifies constraints during staged recommendations (T-1)? Changes apply to next run only; current staged run preserves original constraint snapshot.
- What happens when market is closed or during trading halt? System clearly indicates last market close time; recommendations remain valid until next scheduled run.
- What happens when user has insufficient data for reliable factor scoring? System requires minimum historical data (e.g., 12 months) and displays warning if holdings lack adequate history.
- What happens during earnings blackout period (48 hours before or after announcement)? Stocks are automatically excluded from recommendations with clear "Earnings proximity" reason.
- What happens when portfolio is 100% cash with no holdings? Dashboard shows "Add holdings to unlock analysis" empty state with upload CTA.
- What happens when recommended trades would exceed participation caps? System automatically scales position sizes to respect participation cap = 10% of 20-day ADV per Constraint Set entity (tier 1: 15%, tier 2: 10%, tier 3: 5%). If recommendation exceeds cap, scale position size to cap and flag with warning icon in UI.
- What happens when user's base currency differs from holding currencies? All figures are converted to base currency with FX rate note and conversion timestamp.
- What happens if user tries to run backtest with date range lacking sufficient data? System displays error indicating minimum data requirements and suggests valid date range.

## Requirements *(mandatory)*

### Functional Requirements

**Account & Onboarding**

- **FR-001**: System MUST allow users to create accounts with email and password authentication
- **FR-002**: System MUST send email confirmation and require verification before granting access
- **FR-003**: System MUST persist user sessions across browser restarts
- **FR-004**: System MUST guide new users through onboarding flow: holdings upload → universe selection → constraint configuration → dashboard

**Portfolio Management**

- **FR-005**: System MUST accept portfolio holdings via CSV upload with columns: symbol, quantity, cost basis, currency
- **FR-006**: System MUST validate uploaded holdings and display inline errors for failed rows with specific reasons (invalid symbol, negative quantity, missing data)
- **FR-007**: System MUST allow users to manually edit holdings inline after upload and re-validate
- **FR-008**: System MUST calculate and display current market value, P&L, and benchmark comparison for portfolio
- **FR-009**: System MUST support multiple currency holdings and convert all values to user's base currency with FX rate transparency. Transparency requires: display FX rate (e.g., "1 EUR = 1.0850 USD"), source (ECB), and last update timestamp (e.g., "Updated: 2025-10-31 16:00 UTC") on portfolio summary. Rates updated daily at 16:00 UTC.
- **FR-010**: System MUST show universe coverage percentage indicating how many holdings are included in selected universe (e.g., "85% of holdings in S&P 500 universe")

**Universe & Benchmark Selection**

- **FR-011**: System MUST provide system-defined universe presets (S&P 500, S&P 500 + mid-caps, Russell 2000, regional indexes) seeded in database via migration V1.0.3 (see plan.md:L312). Seed data includes: (1) S&P 500 (500 large-cap US stocks with sector classifications per GICS, liquidity tiers assigned), (2) S&P 500 + Mid-caps (adds ~400 mid-cap stocks), (3) Russell 2000 (2000 small-cap US stocks). Each universe includes constituent symbols, sector mappings, market cap ranges, and liquidity tier assignments (tier 1: >$10B cap + >5M ADV, tier 2: $2-10B cap + >1M ADV, tier 3: <$2B cap or <1M ADV). Full constituent lists defined in database/seeds/universes.sql. V1 does NOT support user-created custom universes; all universes are predefined by system.
- **FR-012**: System MUST persist user's universe selection and use it for all analysis and recommendations
- **FR-013**: System MUST display which holdings are covered vs not covered by selected universe. Complements FR-010 by listing uncovered symbols (e.g., "AAPL, GOOGL not in S&P 500") with option to expand universe or remove holdings.
- **FR-014**: System MUST benchmark portfolio performance against selected universe index

**Constraint Management**

- **FR-015**: System MUST provide default constraints with explicit values and valid ranges:
  - **Max name weight**: 5% for large cap (range 1-15%), 2% for mid cap (range 0.5-10%)
  - **Max sector exposure**: 20% (range 10-50%)
  - **Turnover cap**: 25% (range 10-50%)
  - **Weight deadband**: 30 bps (range 10-100 bps)
  - **Participation caps**: 10% of 20-day ADV by liquidity tier (tier 1: 15%, tier 2: 10%, tier 3: 5%; range 5-25% per tier)

  All defaults seeded in database migration V1.0.3 per T012. Ranges enforced by validation per T141.
- **FR-016**: System MUST allow users to customize all constraint values with validation preventing absurd entries
- **FR-017**: System MUST provide "Preview impact" functionality showing how constraint changes would affect recommendations without saving. Preview estimates: (1) pick count with ±10% accuracy (absolute count deviation; e.g., 20 picks → estimate 18-22 picks), (2) turnover with ±15% accuracy (relative percentage deviation; e.g., 25% turnover → estimate 21.25%-28.75%), and (3) affected positions (stocks added/removed due to constraint change). Calculations use last completed run's factor scores and portfolio state. If no historical run exists, preview is disabled with message "No historical data available for preview; save constraints and trigger a run to see results." Preview calculations MUST complete within 5 seconds.
- **FR-018**: System MUST provide "Restore defaults" button to reset all constraints to recommended values
- **FR-019**: System MUST apply constraint changes only to future runs, preserving historical run integrity

**Month-End Engine & Recommendations**

- **FR-020**: System MUST execute scheduled month-end workflow: T-3 (pre-compute factor blocks) → T-1 (stage recommendations) → T (finalize) → post-trade analysis
- **FR-021**: System MUST allow users to trigger manual "off-cycle" runs at any time, clearly labeled as off-cycle
- **FR-022**: System MUST display run state badge with countdown to next state transition. States: SCHEDULED (created, awaiting T-3), PRE_COMPUTE (T-3 running factor calculations), STAGED (T-1 complete, recommendations ready for review), FINALIZED (T complete, recommendations published), ARCHIVED (30 days post-finalization, historical record). Transitions: SCHEDULED → PRE_COMPUTE (T-3) → STAGED (T-1) → FINALIZED (T) → ARCHIVED (T+30 days).
- **FR-023**: System MUST generate ranked recommendations list sorted by conviction with target weight, expected cost, confidence score (0-100 integer calculated as MIN(factor_strength_percentile, liquidity_score) * 100, where score ≥70 = high confidence, 40-69 = medium, <40 = low confidence flagged in UI), and explanation for each pick
- **FR-024**: System MUST calculate factor scores (Value, Momentum, Quality, Revisions) normalized within industry sectors to avoid accidental sector bets
- **FR-025**: System MUST exclude stocks with: extreme bid-ask spreads, earnings announcements within 48 hours (before or after current time), liquidity below floor, positions that would breach sector caps or turnover limits
- **FR-026**: System MUST display "No trade" guidance when expected alpha does not exceed estimated cost by safe margin (safe margin = 1.5x estimated cost; configurable in admin settings, range 1.2-3.0), with clear explanation. Example: If estimated cost=10bps, expected alpha must exceed 15bps to recommend trade.
- **FR-027**: System MUST show "changed since last run" indicators for each recommendation highlighting additions, removals, and weight changes
- **FR-028**: Off-cycle runs MUST NOT overwrite month-end scheduled results

**Explainability**

- **FR-029**: Each recommendation MUST display plain-language explanation with top 3 drivers, constraint impacts, and risk callouts
- **FR-030**: System MUST show cost vs expected alpha calculation for each recommendation
- **FR-031**: Each exclusion MUST display human-readable reason citing specific rule that blocked it
- **FR-032**: Users MUST be able to export exclusions list as CSV for record-keeping
- **FR-033**: All explanation copy MUST be deterministic and consistent for same input data. "Same input" defined as: identical factor scores, constraint settings, and cost estimates. If any input changes (e.g., factor scores recalculated, constraints modified), explanation text regenerates accordingly.

**Factor Analysis**

- **FR-034**: System MUST display factor heatmap for all holdings showing Value, Momentum, Quality, and Revisions scores with color coding
- **FR-035**: System MUST provide tooltip explanations for each factor block describing what it measures and directionality (Higher/Lower is better)
- **FR-036**: Factor scores MUST be calculated using: Value (earnings yield, B/M, FCF yield), Momentum (12mo-1mo, 6mo-1mo), Quality (ROE/ROIC, gross margin, low accruals), Revisions (1-3mo analyst changes, surprises)

**Data Freshness & Reliability**

- **FR-037**: System MUST display data freshness indicators showing last update time for: prices, fundamentals, estimates
- **FR-038**: System MUST disable "Run now" button when any required data source is stale, displaying specific message identifying blocking source and staleness duration (e.g., "Prices stale; last update 2 hours ago, expected <1 hour. Refresh expected at 17:00 UTC."). Button remains disabled until all required sources are current per FR-037 freshness thresholds.
- **FR-039**: System MUST ingest end-of-day prices, fundamentals with realistic publication delays, analyst estimates, and calendar data (trading days, earnings dates)
- **FR-040**: System MUST validate all ingested data for accuracy (range checks, anomaly detection) before storage

**Reports & Documentation**

- **FR-041**: System MUST generate downloadable month-end report (HTML and PDF formats) with one-click download (single button click after authentication; no intermediate modal or confirmation required)
- **FR-042**: Report MUST include: summary of changes from previous month, ranked picks table, exclusions list, constraint settings snapshot, cost estimates, attribution analysis, and legal disclaimer
- **FR-043**: Report MUST display timestamp of generation and version of constraint rules used
- **FR-044**: All reports and exports MUST automatically include disclaimer: "Educational use; not investment advice" with user's acceptance timestamp (e.g., "Disclaimer acknowledged 2025-01-15 14:32 UTC by user@example.com") for audit trail per FR-058

**Notifications**

- **FR-045**: System MUST send notification at T-3: "Scores calculated for Month-End. Review Analysis for any red flags"
- **FR-046**: System MUST send notification at T-1: "Orders and constraints staged. You can review picks now"
- **FR-047**: System MUST send notification at T (completion): "Month-End recommendations are ready. Download the report"
- **FR-048**: System MUST send notification when data source is stale: "[Source] is stale; month-end scheduled run blocked until refreshed"
- **FR-049**: Users MUST be able to opt out of notification categories individually
- **FR-050**: Notifications MUST NOT contain marketing content or non-essential messages. "Marketing content" defined as: content not directly related to run status or data health (e.g., no feature announcements, upgrade prompts, third-party ads, or promotional messaging in notification emails).

**Backtesting**

- **FR-051**: System MUST provide quick backtest functionality allowing users to select universe preset, date range, and current constraints
- **FR-052**: Backtest results MUST display: equity curve vs benchmark, turnover chart, CAGR, volatility, max drawdown, Sharpe ratio, hit rate (all net of assumed costs)
- **FR-053**: Backtest MUST provide one-line verdict: "Beat equal weight of selected names after costs? Yes/No" with percentage difference
- **FR-054**: System MUST provide sensitivity preview allowing users to adjust one constraint and see expected impact on turnover and pick count
- **FR-055**: Sensitivity preview changes MUST NOT affect saved constraints until explicitly applied by user

**Legal & Compliance**

- **FR-056**: System MUST display "Educational use; not investment advice" banner on first visit to Recommendations page
- **FR-057**: User MUST acknowledge disclaimer before viewing recommendations; access to recommendations page is blocked until acknowledgment is provided per FR-056 banner display
- **FR-058**: System MUST store timestamp of disclaimer acceptance in user profile
- **FR-060**: Settings MUST display user's disclaimer acceptance timestamp for audit purposes

**Permissions & Roles**

- **FR-061**: System MUST support three roles: Owner (full edit, run, configure), Viewer (read-only), Service (scheduled run execution only)
- **FR-062**: Owner role MUST have full access to all features including constraint editing and manual runs
- **FR-063**: Viewer role MUST have read-only access to dashboard, portfolio, analysis, and recommendations but NOT Settings
- **FR-064**: Service role MUST only trigger scheduled runs and write run artifacts without user interface access
- **FR-064.1**: Service role MUST authenticate using dedicated service API key (X-Service-Api-Key header) or JWT with role claim "SERVICE". Service role is restricted to endpoints: POST /api/runs (with run_type=SCHEDULED only), GET /api/runs/{id}, GET /api/data-sources (for health checks). All other endpoints (Settings, manual runs with run_type=OFF_CYCLE, constraint modifications) MUST return 403 Forbidden with message "Service role has restricted access. Manual runs and configuration changes require Owner role." Service API keys MUST be rotatable without application downtime and expire after 90 days with automated renewal notifications.

**Error Handling & Guardrails**

- **FR-066**: When constraints make feasible portfolio impossible, system MUST display single banner: "Constraints prevent a feasible recommendation set. Try relaxing turnover or liquidity floor". Infeasible defined as: (1) turnover cap prevents any trades, (2) sector/name caps exclude all candidates, or (3) optimizer fails to converge after 60 seconds. Display specific blocking constraint(s) in banner message.
- **FR-067**: When run fails, system MUST surface human-readable error cause and preserve partial diagnostics for troubleshooting
- **FR-068**: Empty states MUST provide actionable guidance: "Add holdings to unlock analysis" with upload CTA, "Some symbols aren't in chosen universe" with list
- **FR-069**: When recommendations would show "No trade" decision, system MUST offer quick backtest with relaxed constraints as suggestion. Relaxation heuristic: increase turnover cap by 10%, OR increase sector cap by 5%, OR decrease liquidity floor by 20% (whichever binding constraint is closest to feasibility). User can accept or reject suggested relaxation before running backtest.

**Performance & Scalability**

- **FR-070**: Portfolio calculations MUST complete within 2 seconds for 95th percentile of requests (portfolios with 20-100 holdings). Portfolios >100 holdings supported but may experience degraded performance; warn user if portfolio exceeds 150 holdings.
- **FR-071**: Manual run MUST complete within 10 minutes for 95th percentile of executions (standard universe 500-1000 stocks) performing batch monthly rebalancing analysis. **Clarification**: This 10-minute target applies to monthly batch processing that analyzes entire universe for portfolio optimization (computationally intensive). Real-time intraday prediction recalculations for individual stocks (if added in future versions) MUST meet <10 seconds per constitution Principle II. Batch analysis prioritizes accuracy and completeness over speed while remaining practical for monthly cadence. See plan.md:L63-70 for detailed justification.
- **FR-072**: Dashboard page load MUST complete within 3 seconds for 95th percentile of page loads (measured from navigation click to full data display including portfolio value, P&L, benchmark chart). This includes API response time and rendering.
- **FR-073**: Report generation MUST complete within 30 seconds for 95th percentile of requests (standard monthly report)

### Key Entities

- **User**: Represents an investor account with email, password, base currency, selected universe, custom constraints, portfolio holdings, notification preferences, disclaimer acceptance timestamp, and role (Owner/Viewer/Service)

- **Portfolio**: Collection of holdings with current market value, cash balance, P&L calculation, benchmark comparison, currency breakdown, and coverage percentage relative to selected universe

- **Holding**: Individual stock position with symbol, quantity, cost basis, acquisition date, currency, current market value, unrealized P&L, factor scores, and universe inclusion flag

- **Universe**: System-defined stock universe preset (e.g., S&P 500, S&P 500 + mid-caps, Russell 2000) with constituent symbols, sector classifications, market cap ranges, and liquidity tiers. V1 supports only predefined universes seeded in database; user-created custom universes not supported.

- **Constraint Set**: User-configurable risk parameters including max name weight (by cap size), max sector exposure, turnover cap, weight deadband, participation cap by liquidity tier, spread threshold, and earnings blackout window (default 48 hours, configurable 24-72 hours)

- **Recommendation Run**: Scheduled or manual analysis execution with state (SCHEDULED/PRE_COMPUTE/STAGED/FINALIZED/ARCHIVED per FR-022 state machine), execution timestamp, constraint snapshot, ranked picks list, exclusions list, confidence scores, cost estimates, and change indicators from previous run

- **Recommendation**: Single buy recommendation with stock symbol, target weight, rank/priority, confidence score (0-100), expected cost, top 3 factor drivers, constraint impact notes, risk contribution, and change indicator

- **Exclusion**: Stock that was analyzed but not recommended, with symbol, exclusion reason code (liquidity/earnings/spread/cap breach), human-readable explanation, and timestamp

- **Factor Score**: Calculated metric for stock with type (Value/Momentum/Quality/Revisions), raw score, sector-normalized score (z-score), percentile rank within sector (0-100), component breakdown (JSON object with sub-factor values per FR-036), calculation timestamp, and factor_model_version_id (foreign key to FactorModelVersion entity per plan.md:354-356). See data-model.md for complete schema definition including indexes and partitioning strategy.

- **Backtest**: Historical performance simulation with selected universe, date range, constraint snapshot, equity curve data, benchmark comparison, turnover history, performance statistics (CAGR/volatility/drawdown/Sharpe/hit rate), cost assumptions, and yes/no verdict on beating equal weight

- **Report**: Downloadable document with generation timestamp, recommendations summary, exclusions list, constraint settings, cost analysis, attribution breakdown, performance metrics, and legal disclaimer

- **Notification**: User alert with type (T-3/T-1/T/stale data), message content, delivery timestamp, delivery channel (email/in-app), and read status

- **Data Source**: External feed for prices, fundamentals, estimates, or calendar data with source identifier, last update timestamp, refresh frequency, staleness threshold, and health status

## Success Criteria *(mandatory)*

### Measurable Outcomes

**User Onboarding & Adoption**

- **SC-001**: 80% of new users complete onboarding flow (signup → holdings upload → universe selection) within 15 minutes of account creation
- **SC-002**: Median time from holdings upload to first recommendation set is under 10 minutes
- **SC-003**: 90% of users successfully upload holdings on first attempt without validation errors

**Recommendation Quality & Trust**

- **SC-004**: At least 80% of month-end runs generate a valid recommendation set (not "No trade")
- **SC-005**: 90% of users view "Why this pick?" explanations before taking any action on recommendations
- **SC-006**: Realized turnover stays under user-configured turnover cap for at least 95% of month-end scheduled runs. Example: If cap=25%, then realized turnover ≤25% in 95%+ of runs.
- **SC-007**: Each recommendation displays confidence score, expected cost, and top 3 drivers with 100% consistency

**System Performance**

- **SC-008**: Manual runs complete within 10 minutes for 95% of executions
- **SC-009**: Dashboard page loads with full portfolio data within 3 seconds for 95% of visits
- **SC-010**: Report downloads complete within 30 seconds for 95% of requests
- **SC-011**: System maintains 99% uptime during market hours (excluding planned maintenance)

**User Engagement**

- **SC-012**: 70% of users with month-end scheduled runs download the month-end report within 48 hours of generation
- **SC-013**: 40% of users customize at least one constraint from defaults within first month
- **SC-014**: 30% of users run at least one quick backtest within first 3 months
- **SC-015**: Average user logs in at least 4 times per month to review portfolio and recommendations

**Data Quality & Freshness**

- **SC-016**: Data freshness indicators show all sources current (green) for at least 95% of trading days
- **SC-017**: Zero recommendations generated with stale data (hard constraint enforced)
- **SC-018**: Price data updates within 1 hour of market close for 99% of trading days
- **SC-019**: Factor score calculations complete with sector-normalized values for 100% of stocks in universe

**Transparency & Explainability**

- **SC-020**: 100% of recommendations include plain-language explanation with top 3 drivers
- **SC-021**: 100% of exclusions cite specific rule that blocked the stock
- **SC-022**: Every report includes legal disclaimer, timestamp, and constraint settings snapshot
- **SC-023**: Users can trace every recommendation back to input data and calculation methodology

**Risk Management**

- **SC-024**: Zero recommendations violate user-configured constraints (hard validation enforced)
- **SC-025**: Stocks with earnings within 48 hours are excluded from 100% of recommendation runs
- **SC-026**: Participation caps prevent recommendations exceeding 10% of ADV for 100% of picks
- **SC-027**: "No trade" guidance appears when expected alpha doesn't cover costs by safe margin (conservative threshold enforced)

**Compliance & Legal**

- **SC-028**: 100% of users acknowledge disclaimer before viewing first recommendation set
- **SC-029**: All reports and exports automatically include "not investment advice" disclaimer
- **SC-030**: Disclaimer acceptance timestamp is recorded and displayed in Settings for 100% of users
- **SC-031**: Viewer role users are prevented from editing constraints or triggering runs with 100% enforcement

### Assumptions

1. **Market Data**: Assuming access to reliable end-of-day price feeds, fundamental data with realistic publication delays (10-K/10-Q filings), and analyst estimate databases for factor calculations.

2. **Trading Calendars**: Assuming access to accurate trading calendars identifying market holidays, half-days, and earnings announcement schedules for blackout enforcement.

3. **Cost Model**: Default transaction cost model = 10 bps per trade + market impact (0.1% * sqrt(trade_size / ADV)). User-configurable in admin settings (range: 5-50 bps base cost, 0.05-0.5% market impact factor). Backtests MUST display cost assumptions prominently in footnote (e.g., "Results assume 10bps + 0.1% sqrt(size/ADV) costs"). Cost model applies to both live recommendations and historical backtesting.

4. **Factor Methodology**: Assuming established quantitative factor definitions for Value, Momentum, Quality, and Revisions based on academic research and practitioner consensus.

5. **User Base**: Targeting individual investors managing portfolios of $50K-$5M with 20-100 holdings, not institutional investors or day traders.

6. **Update Frequency**: Monthly rebalancing cadence is appropriate for target audience; not designed for intraday or weekly trading strategies.

7. **Benchmark Data**: Assuming access to index constituent lists and historical performance data for popular benchmarks (S&P 500, Russell 2000, etc.).

8. **Email Delivery**: Assuming reliable email service for notifications; in-app notifications are supplementary, not primary delivery mechanism.

9. **Browser Support**: Assuming modern browsers (Chrome, Firefox, Safari, Edge) with JavaScript enabled; no IE11 support required.

10. **Data Retention**: Assuming 7+ years of historical data retention for audit trails, compliance, and backtesting per regulatory guidance for financial applications.

11. **Disclaimer Effectiveness**: Assuming properly worded and acknowledged disclaimer provides reasonable legal protection; subject to legal review before launch.

## Constraints

**Out of Scope for V1**

- Live brokerage integration or automated trade execution
- Intraday trading signals or real-time market monitoring
- Options strategies, derivatives, or leveraged instruments
- Cryptocurrency or alternative asset classes
- Machine learning model experimentation beyond baseline factor strategy
- Social features, forums, or user collaboration
- Mobile apps (native iOS/Android) - web responsive only with desktop-first design; mobile access is view-only for dashboard and recommendations, not full portfolio management (no CSV upload, constraint editing, or manual run triggers on mobile)
- Multi-account management for advisors or institutions
- Tax-loss harvesting or tax optimization features
- Margin calculations or short selling recommendations

**Technical Constraints**

- System must comply with financial data provider terms of service and licensing restrictions
- No automated trading decisions permitted (monitoring and advisory only)
- Must support concurrent access by 1000+ users without degradation
- Database queries for recent data must complete under 100ms; historical aggregations under 2 seconds

**Regulatory Constraints**

- Must display "not investment advice" disclaimer and obtain user acknowledgment
- Must NOT provide personalized investment advice requiring registration as investment advisor
- Must retain audit logs for minimum 7 years for compliance and dispute resolution
- Must comply with data privacy regulations for user financial information (encryption at rest and in transit)

**User Experience Constraints**

- No jargon-heavy language; prefer short, assertive sentences accessible to retail investors
- Every numeric control must have tooltip explaining ranges and risks
- Charts must be keyboard navigable and readable as data tables for accessibility (4.5:1 contrast minimum)
- Empty states must provide actionable guidance, never blank screens

**Business Constraints**

- Performance targets assume reasonable infrastructure budget; not designed for ultra-low-latency (sub-millisecond) requirements
- Factor methodology must be explainable to non-technical users; black-box ML models not acceptable
- Notification frequency must respect user preferences; no marketing spam or inspirational quotes
- Backtest results must include cost assumptions and fairness notes to prevent misleading performance claims
