# Tasks: Month-End Market Analyst

**Input**: Design documents from `/specs/001-month-end-analyst/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

**Tests**: Included per constitution Principle III (Test-First Development) and spec requirement for 80%+ unit test coverage.

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Per plan.md project structure:
- **Backend**: `backend/src/main/java/com/stockmonitor/`
- **Frontend**: `frontend/src/`
- **Database**: `database/migrations/`
- **Tests**: `backend/src/test/java/com/stockmonitor/` and `frontend/tests/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Create backend project structure with Spring Boot 3.2 + Java 17 in backend/
- [x] T002 [P] Create frontend project structure with React + TypeScript + Vite in frontend/
- [x] T003 [P] Configure Maven build with dependencies: Spring Boot, Spring Data JPA, Spring Security, Spring Batch, PostgreSQL driver, Redis, Liquibase in backend/pom.xml
- [x] T004 [P] Configure npm dependencies: React, React Query, Zustand, Axios, React Router in frontend/package.json
- [x] T005 [P] Configure ESLint, Prettier, Google Java Format for code quality
- [x] T006 [P] Setup Docker Compose with PostgreSQL 15 + TimescaleDB, Redis in docker/docker-compose.yml
- [x] T007 [P] Create .env.example with required environment variables per quickstart.md
- [x] T008 [P] Configure Liquibase migrations framework in backend/src/main/resources/application.yml

**Checkpoint**: Project structure ready, dependencies installed, Docker services configured

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Foundation

- [x] T009 Create Liquibase changeset v1.0.0-initial-schema.xml with all 16 entities per data-model.md in backend/src/main/resources/db/changelog/changes/
- [x] T010 [P] Create Liquibase changeset v1.0.1-add-indexes.xml for primary, unique, and composite indexes in backend/src/main/resources/db/changelog/changes/
- [x] T011 [P] Create Liquibase changeset v1.0.2-setup-partitioning.xml for FactorScore and AuditLog hypertables in backend/src/main/resources/db/changelog/changes/
- [x] T011.5 [P] Add AuditLog partitioning strategy with 7-year retention policy in v1.0.2 changeset - Create annual partitions for AuditLog table. Add retention policy dropping partitions older than 7 years per constitution Principle V and Assumption #11. Configure TimescaleDB retention policy: SELECT add_retention_policy('audit_log', INTERVAL '7 years');
- [x] T012 [P] Create Liquibase changeset v1.0.3-seed-reference-data.xml for default universes, factor model v1.0.0, data sources in backend/src/main/resources/db/changelog/changes/

### Backend Foundation

- [x] T013 [P] Create base JPA entities: User, Portfolio, Holding, Universe in backend/src/main/java/com/stockmonitor/model/
- [x] T014 [P] Create base JPA entities: UniverseConstituent, ConstraintSet, RecommendationRun in backend/src/main/java/com/stockmonitor/model/
- [x] T015 [P] Create base JPA entities: Recommendation, Exclusion, FactorScore, Backtest in backend/src/main/java/com/stockmonitor/model/
- [x] T016 [P] Create base JPA entities: Report, Notification, DataSource, FactorModelVersion, AuditLog in backend/src/main/java/com/stockmonitor/model/
- [x] T017 Create Spring Data JPA repositories for all entities in backend/src/main/java/com/stockmonitor/repository/
- [x] T018 [P] Configure Spring Security with JWT authentication in backend/src/main/java/com/stockmonitor/security/
- [x] T019 [P] Implement JWT token generation and validation in backend/src/main/java/com/stockmonitor/security/JwtService.java
- [x] T019.5 [P] Implement role-based access control (RBAC) with @PreAuthorize annotations in backend/src/main/java/com/stockmonitor/security/RoleService.java - Define three roles per FR-061: ROLE_OWNER (full access), ROLE_VIEWER (read-only), ROLE_SERVICE (scheduled jobs only). Add hasRole() checks to service layer. Create custom access denied handler returning 403 with clear message (e.g., "Viewer role cannot edit constraints").
- [x] T019.6 [P] Implement service API key authentication in backend/src/main/java/com/stockmonitor/security/ServiceApiKeyAuthFilter.java - Extract X-Service-Api-Key header, validate against encrypted keys in database (ServiceApiKey entity), check expiration, set authentication with role=SERVICE
- [x] T019.7 [P] Implement service API key management in backend/src/main/java/com/stockmonitor/service/ServiceApiKeyService.java - Methods: generate(expiresInDays), rotate(oldKeyId, newExpiresInDays), revoke(keyId), listActive(). Keys stored encrypted at rest with last_used_at tracking.
- [x] T019.8 [P] Add service role endpoint restrictions in backend/src/main/java/com/stockmonitor/security/ServiceRoleAccessFilter.java - Whitelist: POST /api/runs (only if run_type=SCHEDULED), GET /api/runs/*, GET /api/data-sources/*. Block all PUT/DELETE operations. Return 403 with clear explanation.
- [x] T020 [P] Configure CORS for frontend origin in backend/src/main/java/com/stockmonitor/config/CorsConfig.java
- [x] T021 [P] Configure Redis cache with Caffeine fallback in backend/src/main/java/com/stockmonitor/config/CacheConfig.java
- [x] T022 [P] Implement global exception handler in backend/src/main/java/com/stockmonitor/controller/GlobalExceptionHandler.java
- [x] T023 [P] Configure structured logging with Logback in backend/src/main/resources/logback-spring.xml
- [x] T024 [P] Setup Prometheus metrics with Micrometer in backend/src/main/java/com/stockmonitor/config/MetricsConfig.java
- [x] T025 [P] Configure Spring Batch job infrastructure in backend/src/main/java/com/stockmonitor/config/BatchConfig.java

### Frontend Foundation

- [x] T026 [P] Setup React Router with route configuration in frontend/src/App.tsx
- [x] T027 [P] Setup React Query client with default config in frontend/src/services/api.ts
- [x] T028 [P] Create Zustand store for auth state in frontend/src/stores/authStore.ts
- [x] T029 [P] Create Axios instance with JWT interceptors in frontend/src/services/api.ts
- [x] T030 [P] Create auth context and protected route wrapper in frontend/src/contexts/AuthContext.tsx
- [x] T031 [P] Create base layout component with header/navigation in frontend/src/components/layout/Layout.tsx
- [x] T032 [P] Configure Tailwind CSS or styling solution in frontend/tailwind.config.js

### Testing Foundation

- [x] T033 [P] Configure JUnit 5, Mockito, TestContainers in backend/src/test/resources/application-test.yml
- [x] T034 [P] Configure Jest, React Testing Library in frontend/jest.config.js
- [x] T035 [P] Create base test utilities and fixtures in backend/src/test/java/com/stockmonitor/testutil/

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Initial Portfolio Setup and First Recommendation Run (Priority: P1) 🎯 MVP

**Goal**: Enable investor to sign up, upload holdings, select universe, and receive first ranked recommendations with explanations

**Independent Test**: Create account, upload sample portfolio CSV, select S&P 500 universe, trigger manual run, verify ranked recommendations appear with confidence scores and explanations

### Tests for User Story 1 (Write FIRST, ensure they FAIL before implementation)

- [x] T036 [P] [US1] Contract test for POST /api/auth/register in backend/src/test/java/com/stockmonitor/contract/AuthContractTest.java
- [x] T037 [P] [US1] Contract test for POST /api/auth/login in backend/src/test/java/com/stockmonitor/contract/AuthContractTest.java
- [x] T038 [P] [US1] Contract test for POST /api/portfolios/{id}/holdings/upload in backend/src/test/java/com/stockmonitor/contract/PortfolioContractTest.java
- [x] T039 [P] [US1] Contract test for GET /api/universes in backend/src/test/java/com/stockmonitor/contract/UniverseContractTest.java
- [x] T040 [P] [US1] Contract test for POST /api/runs in backend/src/test/java/com/stockmonitor/contract/RecommendationContractTest.java
- [x] T041 [P] [US1] Contract test for GET /api/runs/{id}/recommendations in backend/src/test/java/com/stockmonitor/contract/RecommendationContractTest.java
- [x] T042 [P] [US1] Integration test for user registration to first recommendation flow in backend/src/test/java/com/stockmonitor/integration/OnboardingFlowTest.java
- [x] T042.5 [P] [US1] Integration test verifying off-cycle run doesn't overwrite scheduled run results per FR-028 in backend/src/test/java/com/stockmonitor/integration/OffCycleIsolationTest.java - Create scheduled run, trigger off-cycle run, verify scheduled run results unchanged. Query GET /api/portfolios/{id}/recommendations returns scheduled run only. Query GET /api/runs filters by run_type correctly.
- [x] T043 [P] [US1] Unit test for CSV holdings parser in backend/src/test/java/com/stockmonitor/service/HoldingsCsvParserTest.java
- [x] T044 [P] [US1] Unit test for portfolio calculation service in backend/src/test/java/com/stockmonitor/service/PortfolioCalculationServiceTest.java
- [x] T045 [P] [US1] Unit test for constraint validation in backend/src/test/java/com/stockmonitor/service/ConstraintValidationServiceTest.java
- [x] T045.5 [US1] **GATE: Verify all User Story 1 tests FAIL (red state)** - Run test suite for T036-T045 and confirm ALL tests fail with expected errors (NotImplementedException, 404 responses, null returns). Document failure output. This gate enforces constitution Principle III (Test-First Development). Do NOT proceed to T046 until all tests show red state.

### Backend Implementation for User Story 1

#### Authentication & Users (FR-001 to FR-004)

- [x] T046 [P] [US1] Create UserDTO and registration request/response DTOs in backend/src/main/java/com/stockmonitor/dto/
- [x] T047 [US1] Implement UserService with registration, email verification, session management in backend/src/main/java/com/stockmonitor/service/UserService.java
- [x] T048 [US1] Implement AuthController with POST /api/auth/register, POST /api/auth/login, POST /api/auth/verify-email in backend/src/main/java/com/stockmonitor/controller/AuthController.java
- [x] T049 [P] [US1] Implement email service with verification templates in backend/src/main/java/com/stockmonitor/service/EmailService.java
- [x] T050 [P] [US1] Add validation for email format and password complexity in backend/src/main/java/com/stockmonitor/validation/

#### Portfolio Management (FR-005 to FR-010)

- [x] T051 [P] [US1] Create PortfolioDTO, HoldingDTO, PortfolioSummaryDTO in backend/src/main/java/com/stockmonitor/dto/
- [x] T052 [US1] Implement HoldingsCsvParser with validation in backend/src/main/java/com/stockmonitor/service/HoldingsCsvParser.java
- [x] T052.5 [US1] Implement holdings validation with specific error codes per FR-006 in backend/src/main/java/com/stockmonitor/validation/HoldingsValidator.java - Error codes: INVALID_SYMBOL, NEGATIVE_QUANTITY, MISSING_DATA, INVALID_CURRENCY. Return validation errors with row number, column, and human-readable message (e.g., "Row 5: Quantity cannot be negative (-10 shares)")
- [x] T053 [US1] Implement PortfolioService with holdings upload, portfolio calculation, universe coverage in backend/src/main/java/com/stockmonitor/service/PortfolioService.java
- [x] T054 [US1] Implement PortfolioController with POST /api/portfolios, GET /api/portfolios/{id}, POST /api/portfolios/{id}/holdings/upload in backend/src/main/java/com/stockmonitor/controller/PortfolioController.java
- [x] T055 [P] [US1] Implement FX rate service for multi-currency conversion in backend/src/main/java/com/stockmonitor/service/FxRateService.java
- [x] T056 [P] [US1] Implement portfolio calculation engine (P&L, benchmark comparison) in backend/src/main/java/com/stockmonitor/engine/PortfolioCalculationEngine.java

#### Universe & Benchmark Selection (FR-011 to FR-014)

- [x] T057 [P] [US1] Create UniverseDTO, UniverseConstituentDTO in backend/src/main/java/com/stockmonitor/dto/
- [x] T058 [US1] Implement UniverseService with preset retrieval, constituent lookup in backend/src/main/java/com/stockmonitor/service/UniverseService.java
- [x] T059 [US1] Implement UniverseController with GET /api/universes, GET /api/universes/{id}, PUT /api/portfolios/{id}/universe in backend/src/main/java/com/stockmonitor/controller/UniverseController.java

#### Constraint Management (FR-015)

- [x] T060 [P] [US1] Create ConstraintSetDTO with default values in backend/src/main/java/com/stockmonitor/dto/ConstraintSetDTO.java
- [x] T061 [US1] Implement ConstraintService with default retrieval, active constraint loading in backend/src/main/java/com/stockmonitor/service/ConstraintService.java
- [x] T062 [US1] Implement ConstraintController with GET /api/constraints/defaults, GET /api/portfolios/{id}/constraints in backend/src/main/java/com/stockmonitor/controller/ConstraintController.java
- [x] T062.5 [US1] Add role validation to constraint endpoints in ConstraintController per FR-062-FR-063 - Enforce @PreAuthorize("hasRole('OWNER')") on PUT /api/portfolios/{id}/constraints and POST /api/portfolios/{id}/constraints/reset. Return 403 Forbidden with explanation "Only portfolio owner can modify constraints" for VIEWER role attempts.

#### Recommendation Engine (FR-020 to FR-028)

- [x] T063 [P] [US1] Create RecommendationRunDTO, RecommendationDTO with driver fields in backend/src/main/java/com/stockmonitor/dto/
- [x] T064 [US1] Implement FactorCalculationService using Apache Commons Math in backend/src/main/java/com/stockmonitor/engine/FactorCalculationService.java
- [x] T065 [US1] Implement ConstraintEvaluationService with sector caps, turnover limits, spread thresholds in backend/src/main/java/com/stockmonitor/engine/ConstraintEvaluationService.java
- [x] T066 [US1] Implement RecommendationEngine with factor scoring, ranking, constraint application in backend/src/main/java/com/stockmonitor/engine/RecommendationEngine.java
- [x] T067 [US1] Implement RecommendationService with run orchestration, state transitions in backend/src/main/java/com/stockmonitor/service/RecommendationService.java
- [x] T068 [US1] Implement RecommendationController with POST /api/runs, GET /api/runs/{id}, GET /api/runs/{id}/recommendations in backend/src/main/java/com/stockmonitor/controller/RecommendationController.java
- [x] T068.5 [US1] Implement run type isolation per FR-028 in RecommendationService - Add run_type enum (SCHEDULED, OFF_CYCLE) to RecommendationRun entity. Off-cycle runs store results in separate table or flag with is_official=false. Query for "current recommendations" returns only most recent SCHEDULED run; off-cycle runs accessible via separate endpoint GET /api/runs/{id}/off-cycle. Prevent off-cycle from overwriting scheduled run results.
- [x] T069 [P] [US1] Implement cost calculation service (transaction costs, market impact) in backend/src/main/java/com/stockmonitor/engine/CostCalculationService.java
- [x] T070 [P] [US1] Implement explanation generator for recommendations in backend/src/main/java/com/stockmonitor/service/ExplanationService.java

#### Data Freshness (FR-037, FR-038)

- [x] T071 [P] [US1] Implement DataSourceHealthService with freshness checks in backend/src/main/java/com/stockmonitor/service/DataSourceHealthService.java
- [x] T072 [US1] Add data freshness validation to RecommendationService blocking logic in backend/src/main/java/com/stockmonitor/service/RecommendationService.java
- [x] T072.5 [US1] Add role validation to manual run trigger per FR-062 - Enforce @PreAuthorize("hasRole('OWNER')") on POST /api/runs. VIEWER and SERVICE roles blocked from manual runs with 403 Forbidden response. SERVICE role can only trigger runs via scheduled jobs (T098-T101).

### Frontend Implementation for User Story 1

#### Authentication Pages

- [x] T073 [P] [US1] Create registration page with email/password form in frontend/src/pages/Register.tsx
- [x] T074 [P] [US1] Create login page with email/password form in frontend/src/pages/Login.tsx
- [x] T075 [P] [US1] Create email verification page in frontend/src/pages/VerifyEmail.tsx
- [x] T076 [P] [US1] Create auth service with register, login, logout methods in frontend/src/services/authService.ts

#### Portfolio Setup Pages

- [x] T077 [P] [US1] Create holdings upload page with CSV upload and validation in frontend/src/pages/Holdings.tsx
- [x] T078 [P] [US1] Create portfolio service with upload, fetch methods in frontend/src/services/portfolioService.ts
- [x] T078.5 [US1] Create holdings inline edit component with validation per FR-007 in frontend/src/components/portfolio/HoldingsEditor.tsx - Display validation errors inline with red highlighting for failed rows, show specific error messages from T052.5 (row number, column, reason). Allow edit symbol, quantity, cost basis, currency with immediate validation feedback.
- [x] T079 [P] [US1] Create universe selection component in frontend/src/components/portfolio/UniverseSelector.tsx
- [x] T080 [P] [US1] Create portfolio summary component showing market value, P&L, coverage in frontend/src/components/portfolio/PortfolioSummary.tsx

#### Dashboard & Recommendations

- [x] T081 [P] [US1] Create dashboard page with portfolio summary and "Run now" button in frontend/src/pages/Dashboard.tsx
- [x] T082 [P] [US1] Create recommendations page with ranked list in frontend/src/pages/Recommendations.tsx
- [x] T083 [P] [US1] Create recommendation card component with confidence, cost, drivers in frontend/src/components/recommendations/RecommendationCard.tsx
- [x] T084 [P] [US1] Create recommendation service with run trigger, fetch methods in frontend/src/services/recommendationService.ts
- [x] T085 [P] [US1] Create explanation panel component for top 3 drivers in frontend/src/components/recommendations/ExplanationPanel.tsx

#### Common Components

- [x] T086 [P] [US1] Create loading spinner component in frontend/src/components/common/LoadingSpinner.tsx
- [x] T087 [P] [US1] Create error message component in frontend/src/components/common/ErrorMessage.tsx
- [x] T088 [P] [US1] Create data freshness badge component in frontend/src/components/common/FreshnessBadge.tsx

### Legal & Compliance (FR-056 to FR-060)

- [x] T089 [P] [US1] Create disclaimer acceptance service in backend/src/main/java/com/stockmonitor/service/DisclaimerService.java
- [x] T090 [P] [US1] Create disclaimer modal component in frontend/src/components/common/DisclaimerModal.tsx
- [x] T091 [US1] Add disclaimer acceptance check to recommendations page in frontend/src/pages/Recommendations.tsx

**⚠️ TEST-FIRST GATE PASSED**: All US1 tests (T036-T045) verified in FAIL state at T045.5. Implementation (T046-T091) can now proceed per TDD red-green-refactor cycle.

**Checkpoint**: User Story 1 should be fully functional and testable independently. User can complete full onboarding and receive first recommendations.

---

## Phase 4: User Story 2 - Understanding and Acting on Month-End Recommendations (Priority: P2)

**Goal**: Enable existing user to receive scheduled month-end recommendations, review explanations, download reports, and see what changed since last month

**Independent Test**: Trigger scheduled month-end run for existing portfolio, verify notification delivery, check recommendations show change indicators, confirm report downloads with all required sections

### Tests for User Story 2

- [x] T092 [P] [US2] Contract test for GET /api/runs/{id}/report in backend/src/test/java/com/stockmonitor/contract/ReportContractTest.java
- [x] T093 [P] [US2] Contract test for WebSocket /ws/runs/{runId}/status in backend/src/test/java/com/stockmonitor/contract/WebSocketContractTest.java
- [x] T094 [P] [US2] Contract test for WebSocket /ws/notifications in backend/src/test/java/com/stockmonitor/contract/WebSocketContractTest.java
- [x] T095 [P] [US2] Integration test for scheduled month-end workflow (T-3, T-1, T) in backend/src/test/java/com/stockmonitor/integration/MonthEndWorkflowTest.java
- [x] T096 [P] [US2] Unit test for report generation service in backend/src/test/java/com/stockmonitor/service/ReportGenerationServiceTest.java
- [x] T097 [P] [US2] Unit test for notification service in backend/src/test/java/com/stockmonitor/service/NotificationServiceTest.java

### Backend Implementation for User Story 2

#### Scheduled Jobs (FR-020)

- [x] T098 [P] [US2] Implement MonthEndScheduler with @Scheduled annotations for T-3, T-1, T in backend/src/main/java/com/stockmonitor/scheduler/MonthEndScheduler.java
- [x] T099 [P] [US2] Implement Spring Batch job for T-3 pre-compute in backend/src/main/java/com/stockmonitor/batch/PreComputeBatchJob.java
- [x] T100 [P] [US2] Implement Spring Batch job for T-1 staging in backend/src/main/java/com/stockmonitor/batch/StagingBatchJob.java
- [x] T101 [P] [US2] Implement Spring Batch job for T finalization in backend/src/main/java/com/stockmonitor/batch/FinalizationBatchJob.java
- [x] T102 [US2] Add idempotency checks to scheduled jobs in backend/src/main/java/com/stockmonitor/scheduler/MonthEndScheduler.java

#### Notifications (FR-045 to FR-050)

- [x] T103 [P] [US2] Create NotificationDTO with category, priority, action fields in backend/src/main/java/com/stockmonitor/dto/NotificationDTO.java
- [x] T104 [US2] Implement NotificationService with delivery, read tracking in backend/src/main/java/com/stockmonitor/service/NotificationService.java
- [x] T105 [US2] Implement NotificationController with GET /api/notifications, POST /api/notifications/{id}/read in backend/src/main/java/com/stockmonitor/controller/NotificationController.java
- [x] T106 [P] [US2] Implement email notification templates for T-3, T-1, T events in backend/src/main/resources/templates/
- [x] T107 [P] [US2] Add notification preferences management per FR-049 in backend/src/main/java/com/stockmonitor/service/UserService.java - Implement per-category opt-out: T-3_PRECOMPUTE, T-1_STAGED, T_FINALIZED, DATA_STALE. Store preferences in User entity as JSON or separate NotificationPreferences table. Default: all categories enabled. API endpoint PUT /api/users/me/notification-preferences

#### Reports (FR-041 to FR-044)

- [x] T108 [P] [US2] Create ReportDTO with summary, picks, exclusions, disclaimers in backend/src/main/java/com/stockmonitor/dto/ReportDTO.java
- [x] T109 [US2] Implement ReportGenerationService with Flying Saucer (OpenHTMLtoPDF) in backend/src/main/java/com/stockmonitor/service/ReportGenerationService.java
- [x] T110 [US2] Implement ReportController with GET /api/runs/{id}/report in backend/src/main/java/com/stockmonitor/controller/ReportController.java
- [x] T111 [P] [US2] Create HTML report template with Thymeleaf in backend/src/main/resources/templates/report.html
- [x] T112 [P] [US2] Add PDF generation with charts/tables in backend/src/main/java/com/stockmonitor/service/ReportGenerationService.java

#### Change Indicators (FR-027)

- [x] T113 [US2] Implement change detection service comparing current vs previous run in backend/src/main/java/com/stockmonitor/service/ChangeDetectionService.java
- [x] T114 [US2] Add change indicators to RecommendationDTO (NEW, INCREASED, DECREASED, UNCHANGED, REMOVED) in backend/src/main/java/com/stockmonitor/dto/RecommendationDTO.java

#### WebSocket Real-Time Updates (FR-046, FR-047)

- [x] T115 [P] [US2] Configure Spring WebFlux for SSE in backend/src/main/java/com/stockmonitor/config/WebSocketConfig.java
- [x] T116 [US2] Implement WebSocket controller for /ws/runs/{runId}/status in backend/src/main/java/com/stockmonitor/controller/RunStatusWebSocketController.java
- [x] T117 [US2] Implement WebSocket controller for /ws/notifications in backend/src/main/java/com/stockmonitor/controller/NotificationWebSocketController.java
- [x] T118 [P] [US2] Add progress tracking to RecommendationEngine with WebSocket broadcasts in backend/src/main/java/com/stockmonitor/engine/RecommendationEngine.java

### Frontend Implementation for User Story 2

#### Notifications

- [x] T119 [P] [US2] Create notification bell component with unread count in frontend/src/components/common/NotificationBell.tsx
- [x] T120 [P] [US2] Create notification dropdown list in frontend/src/components/common/NotificationList.tsx
- [x] T121 [P] [US2] Create toast notification component in frontend/src/components/common/ToastNotification.tsx
- [x] T122 [P] [US2] Create WebSocket hook for notifications in frontend/src/hooks/useNotificationWebSocket.ts
- [x] T123 [P] [US2] Create notification service in frontend/src/services/notificationService.ts

#### Run Status Monitoring

- [x] T124 [P] [US2] Create run status badge component displaying states (SCHEDULED, PRE_COMPUTE, STAGED, FINALIZED, ARCHIVED) per FR-022 in frontend/src/components/recommendations/RunStatusBadge.tsx
- [x] T125 [P] [US2] Create progress bar component with stage labels in frontend/src/components/recommendations/ProgressBar.tsx
- [x] T126 [P] [US2] Create WebSocket hook for run status in frontend/src/hooks/useRunStatusWebSocket.ts
- [x] T127 [US2] Add real-time progress updates to recommendations page in frontend/src/pages/Recommendations.tsx

#### Reports

- [x] T128 [P] [US2] Create report download button component in frontend/src/components/recommendations/ReportDownloadButton.tsx
- [x] T129 [P] [US2] Add report download logic to recommendation service in frontend/src/services/recommendationService.ts

#### Change Indicators

- [x] T130 [P] [US2] Create change indicator badge component in frontend/src/components/recommendations/ChangeBadge.tsx
- [x] T131 [US2] Add change indicators to recommendation cards in frontend/src/components/recommendations/RecommendationCard.tsx

**Checkpoint**: User Stories 1 AND 2 should both work independently. Users can receive scheduled recommendations and monitor progress in real-time.

---

## Phase 5: User Story 3 - Tuning Constraints and Previewing Impact (Priority: P3)

**Goal**: Enable user to customize risk parameters by adjusting constraints and previewing impact before committing changes

**Independent Test**: Navigate to Settings, modify turnover cap from 25% to 20%, click "Preview impact", verify preview shows expected changes, save changes, confirm next run uses updated constraints

### Tests for User Story 3

- [x] T132 [P] [US3] Contract test for POST /api/portfolios/{id}/constraints/preview in backend/src/test/java/com/stockmonitor/contract/ConstraintContractTest.java
- [x] T133 [P] [US3] Contract test for PUT /api/portfolios/{id}/constraints in backend/src/test/java/com/stockmonitor/contract/ConstraintContractTest.java
- [x] T134 [P] [US3] Integration test for constraint modification workflow in backend/src/test/java/com/stockmonitor/integration/ConstraintModificationTest.java
- [x] T135 [P] [US3] Unit test for constraint preview service in backend/src/test/java/com/stockmonitor/service/ConstraintPreviewServiceTest.java

### Backend Implementation for User Story 3

#### Constraint Preview (FR-017)

- [x] T136 [P] [US3] Create ConstraintPreviewDTO with impact estimates in backend/src/main/java/com/stockmonitor/dto/ConstraintPreviewDTO.java
- [x] T137 [US3] Implement ConstraintPreviewService with impact simulation using last run's factor scores per FR-017 in backend/src/main/java/com/stockmonitor/service/ConstraintPreviewService.java - Re-run optimizer with new constraints and compare pick count, turnover, affected positions. Return estimates with accuracy ranges (±10% picks, ±15% turnover). Fail gracefully if no historical run data.
- [x] T138 [US3] Add POST /api/portfolios/{id}/constraints/preview to ConstraintController in backend/src/main/java/com/stockmonitor/controller/ConstraintController.java

#### Constraint Saving & Versioning (FR-016, FR-018, FR-019)

- [x] T139 [US3] Implement constraint versioning in ConstraintService in backend/src/main/java/com/stockmonitor/service/ConstraintService.java
- [x] T140 [US3] Add PUT /api/portfolios/{id}/constraints, POST /api/portfolios/{id}/constraints/reset to ConstraintController in backend/src/main/java/com/stockmonitor/controller/ConstraintController.java
- [x] T141 [P] [US3] Add validation for constraint values (ranges, logical consistency) in backend/src/main/java/com/stockmonitor/validation/ConstraintValidator.java

### Frontend Implementation for User Story 3

#### Settings Page

- [x] T142 [P] [US3] Create settings page with constraint editor in frontend/src/pages/Settings.tsx
- [x] T143 [P] [US3] Create constraint input component with tooltips in frontend/src/components/settings/ConstraintInput.tsx
- [x] T144 [P] [US3] Create constraint preview panel component in frontend/src/components/settings/ConstraintPreview.tsx
- [x] T145 [P] [US3] Create constraint service with preview, save, reset methods in frontend/src/services/constraintService.ts
- [x] T146 [US3] Add "Restore defaults" button with confirmation modal per FR-018 in frontend/src/pages/Settings.tsx - Button displays at bottom of constraint editor. On click, show confirmation modal: "Reset all constraints to default values? This will discard your custom settings." On confirm, call constraintService.reset() and reload default values. Show success toast after reset.

**Checkpoint**: User Stories 1, 2, AND 3 should all work independently. Users can customize constraints and see impact before committing.

---

## Phase 6: User Story 4 - Portfolio Monitoring and Factor Analysis (Priority: P4)

**Goal**: Enable user to review portfolio performance, analyze factor exposures for each holding, and identify contributors/detractors versus benchmark

**Independent Test**: View Dashboard and Analysis pages, verify performance metrics (P&L, benchmark comparison), check factor heatmap displays for all holdings, confirm data freshness indicators show last update times

### Tests for User Story 4

- [x] T147 [P] [US4] Contract test for GET /api/portfolios/{id}/factors in backend/src/test/java/com/stockmonitor/contract/FactorContractTest.java
- [x] T148 [P] [US4] Contract test for GET /api/portfolios/{id}/performance in backend/src/test/java/com/stockmonitor/contract/PortfolioContractTest.java
- [x] T149 [P] [US4] Contract test for GET /api/data-sources in backend/src/test/java/com/stockmonitor/contract/DataSourceContractTest.java
- [x] T150 [P] [US4] Unit test for factor score calculation in backend/src/test/java/com/stockmonitor/engine/FactorCalculationServiceTest.java
- [x] T151 [P] [US4] Unit test for performance attribution service in backend/src/test/java/com/stockmonitor/service/PerformanceAttributionServiceTest.java

### Backend Implementation for User Story 4

#### Factor Analysis (FR-034 to FR-036)

- [x] T152 [P] [US4] Create FactorScoreDTO with Value, Momentum, Quality, Revisions in backend/src/main/java/com/stockmonitor/dto/FactorScoreDTO.java
- [x] T153 [US4] Implement FactorService with heatmap generation, sector normalization in backend/src/main/java/com/stockmonitor/service/FactorService.java
- [x] T154 [US4] Implement FactorController with GET /api/portfolios/{id}/factors, GET /api/holdings/{id}/factors in backend/src/main/java/com/stockmonitor/controller/FactorController.java
- [x] T155 [P] [US4] Implement sector normalization (z-score) in FactorCalculationService in backend/src/main/java/com/stockmonitor/engine/FactorCalculationService.java

#### Performance Metrics (FR-008, FR-014)

- [x] T156 [P] [US4] Create PerformanceMetricsDTO with P&L, benchmark comparison, contributors/detractors in backend/src/main/java/com/stockmonitor/dto/PerformanceMetricsDTO.java
- [x] T157 [US4] Implement PerformanceAttributionService with contribution analysis in backend/src/main/java/com/stockmonitor/service/PerformanceAttributionService.java
- [x] T158 [US4] Add GET /api/portfolios/{id}/performance to PortfolioController in backend/src/main/java/com/stockmonitor/controller/PortfolioController.java

#### Data Freshness Indicators (FR-037, FR-038)

- [x] T159 [P] [US4] Create DataSourceHealthDTO with status, last update time in backend/src/main/java/com/stockmonitor/dto/DataSourceHealthDTO.java
- [x] T160 [US4] Implement DataSourceController with GET /api/data-sources, GET /api/data-sources/{id}/health in backend/src/main/java/com/stockmonitor/controller/DataSourceController.java
- [x] T161 [P] [US4] Add health status calculation logic to DataSourceHealthService in backend/src/main/java/com/stockmonitor/service/DataSourceHealthService.java

### Frontend Implementation for User Story 4

#### Dashboard Page Enhancements

- [x] T162 [US4] Add performance metrics to dashboard page in frontend/src/pages/Dashboard.tsx
- [x] T163 [P] [US4] Create top contributors/detractors component in frontend/src/components/portfolio/ContributorsTable.tsx
- [x] T164 [P] [US4] Create benchmark comparison chart in frontend/src/components/portfolio/BenchmarkChart.tsx

#### Analysis Page

- [x] T165 [P] [US4] Create analysis page with factor heatmap in frontend/src/pages/Analysis.tsx
- [x] T166 [P] [US4] Create factor heatmap component in frontend/src/components/analysis/FactorHeatmap.tsx
- [x] T167 [P] [US4] Create factor tooltip component with explanations in frontend/src/components/analysis/FactorTooltip.tsx
- [x] T168 [P] [US4] Create data freshness indicators in frontend/src/components/analysis/FreshnessIndicators.tsx
- [x] T169 [P] [US4] Create factor service in frontend/src/services/factorService.ts

**Checkpoint**: User Stories 1-4 should all work independently. Users can analyze portfolio health and factor exposures.

---

## Phase 7: User Story 5 - Quick Backtesting and Sensitivity Analysis (Priority: P5)

**Goal**: Enable user to run quick backtests to evaluate how current constraints would have performed historically and adjust constraints to see sensitivity

**Independent Test**: Select S&P 500 universe, date range (2 years), current constraints, run backtest, verify results show performance vs benchmark with turnover stats and yes/no verdict on "beat equal weight after costs"

### Tests for User Story 5

- [x] T170 [P] [US5] Contract test for POST /api/backtests in backend/src/test/java/com/stockmonitor/contract/BacktestContractTest.java
- [x] T171 [P] [US5] Contract test for GET /api/backtests/{id} in backend/src/test/java/com/stockmonitor/contract/BacktestContractTest.java
- [x] T172 [P] [US5] Contract test for POST /api/constraints/sensitivity in backend/src/test/java/com/stockmonitor/contract/ConstraintContractTest.java
- [x] T173 [P] [US5] Integration test for backtest execution in backend/src/test/java/com/stockmonitor/integration/BacktestExecutionTest.java
- [x] T174 [P] [US5] Unit test for backtest engine in backend/src/test/java/com/stockmonitor/engine/BacktestEngineTest.java

### Backend Implementation for User Story 5

#### Backtesting (FR-051 to FR-053)

- [x] T175 [P] [US5] Create BacktestDTO with performance metrics (CAGR, Sharpe, drawdown) in backend/src/main/java/com/stockmonitor/dto/BacktestDTO.java
- [x] T176 [US5] Implement BacktestEngine with equity curve calculation, performance metrics in backend/src/main/java/com/stockmonitor/engine/BacktestEngine.java
- [x] T177 [US5] Implement BacktestService with execution orchestration, result storage in backend/src/main/java/com/stockmonitor/service/BacktestService.java
- [x] T178 [US5] Implement BacktestController with POST /api/backtests, GET /api/backtests/{id} in backend/src/main/java/com/stockmonitor/controller/BacktestController.java
- [x] T179 [P] [US5] Implement cost model for backtesting in backend/src/main/java/com/stockmonitor/engine/CostModelService.java

#### Sensitivity Analysis (FR-054, FR-055)

- [x] T180 [P] [US5] Create SensitivityPreviewDTO with impact estimates in backend/src/main/java/com/stockmonitor/dto/SensitivityPreviewDTO.java
- [x] T181 [US5] Implement SensitivityAnalysisService in backend/src/main/java/com/stockmonitor/service/SensitivityAnalysisService.java
- [x] T182 [US5] Add POST /api/constraints/sensitivity to ConstraintController in backend/src/main/java/com/stockmonitor/controller/ConstraintController.java

### Frontend Implementation for User Story 5

#### Backtests Page

- [x] T183 [P] [US5] Create backtests page with parameter selection in frontend/src/pages/Backtests.tsx
- [x] T184 [P] [US5] Create backtest results component with equity curve in frontend/src/components/backtesting/BacktestResults.tsx
- [x] T185 [P] [US5] Create equity curve chart component in frontend/src/components/backtesting/EquityCurveChart.tsx
- [x] T186 [P] [US5] Create performance metrics table component in frontend/src/components/backtesting/PerformanceTable.tsx
- [x] T187 [P] [US5] Create backtest service in frontend/src/services/backtestService.ts

#### Sensitivity Analysis

- [x] T188 [P] [US5] Create sensitivity preview component in frontend/src/components/settings/SensitivityPreview.tsx
- [x] T189 [P] [US5] Add "Apply to constraints" button to sensitivity preview in frontend/src/components/settings/SensitivityPreview.tsx

**Checkpoint**: User Stories 1-5 should all work independently. Users can backtest strategies and understand sensitivity to constraint changes.

---

## Phase 8: User Story 6 - Managing Exclusions and Understanding "No Trade" Decisions (Priority: P6)

**Goal**: Enable user to review why certain stocks were excluded from recommendations and understand when system decides to issue "No trade" guidance

**Independent Test**: Review recommendations run where stocks were excluded, click "Why not this one?" for excluded symbols, verify explanations cite specific rules (liquidity floor, sector cap, earnings proximity, spread thresholds). Test scenario where expected advantage doesn't cover costs and verify "No trade" guidance appears.

### Tests for User Story 6

- [x] T190 [P] [US6] Contract test for GET /api/runs/{id}/exclusions in backend/src/test/java/com/stockmonitor/contract/ExclusionContractTest.java
- [x] T191 [P] [US6] Integration test for "No trade" decision logic in backend/src/test/java/com/stockmonitor/integration/NoTradeDecisionTest.java
- [x] T192 [P] [US6] Unit test for exclusion reason generation in backend/src/test/java/com/stockmonitor/service/ExclusionReasonServiceTest.java

### Backend Implementation for User Story 6

#### Exclusions (FR-031, FR-032)

- [x] T193 [P] [US6] Create ExclusionDTO with reason code and explanation in backend/src/main/java/com/stockmonitor/dto/ExclusionDTO.java
- [x] T194 [US6] Implement ExclusionReasonService with human-readable explanations in backend/src/main/java/com/stockmonitor/service/ExclusionReasonService.java
- [x] T195 [US6] Add GET /api/runs/{id}/exclusions to RecommendationController in backend/src/main/java/com/stockmonitor/controller/RecommendationController.java
- [x] T196 [P] [US6] Implement exclusion export (CSV) per FR-032 in backend/src/main/java/com/stockmonitor/service/ExclusionExportService.java - CSV schema: columns = symbol, company_name, exclusion_reason_code, explanation, run_date. UTF-8 encoding, RFC 4180 compliant (quoted fields, CRLF line endings). Endpoint: GET /api/runs/{id}/exclusions/export returns Content-Type: text/csv with Content-Disposition: attachment.

#### "No Trade" Logic (FR-026, FR-069)

- [x] T197 [US6] Implement "No trade" decision logic in RecommendationEngine in backend/src/main/java/com/stockmonitor/engine/RecommendationEngine.java
- [x] T198 [P] [US6] Add edge-over-cost calculation with safe margin check in backend/src/main/java/com/stockmonitor/engine/CostCalculationService.java

### Frontend Implementation for User Story 6

#### Exclusions Tab

- [x] T199 [P] [US6] Create exclusions tab on recommendations page in frontend/src/pages/Recommendations.tsx
- [x] T200 [P] [US6] Create exclusions list component in frontend/src/components/recommendations/ExclusionsList.tsx
- [x] T201 [P] [US6] Create exclusion detail modal with reason in frontend/src/components/recommendations/ExclusionDetailModal.tsx
- [x] T202 [P] [US6] Add "Export exclusions" button in frontend/src/components/recommendations/ExclusionsList.tsx

#### "No Trade" Guidance

- [x] T203 [P] [US6] Create "No trade" banner component in frontend/src/components/recommendations/NoTradeBanner.tsx
- [x] T204 [US6] Add "No trade" handling to recommendations page in frontend/src/pages/Recommendations.tsx

**Checkpoint**: All user stories should now be independently functional. Users can understand all exclusions and "no trade" decisions.

---

## Phase 9: Data Integration & External Feeds

**Purpose**: Integrate external market data providers per contracts/data-feeds.md

### Data Feed Infrastructure

- [x] T205 [P] Implement AlphaVantageClient for end-of-day prices in backend/src/main/java/com/stockmonitor/integration/AlphaVantageClient.java
- [x] T206 [P] Implement IEXCloudClient for fallback prices in backend/src/main/java/com/stockmonitor/integration/IEXCloudClient.java
- [x] T207 [P] Implement ECB FX rate client in backend/src/main/java/com/stockmonitor/integration/ECBFxRateClient.java
- [x] T208 [P] Implement data feed retry handler with exponential backoff in backend/src/main/java/com/stockmonitor/integration/DataFeedRetryHandler.java
- [x] T209 [P] Implement circuit breaker for data feeds in backend/src/main/java/com/stockmonitor/integration/DataFeedCircuitBreaker.java
- [x] T210 [P] Implement rate limiters for Alpha Vantage and IEX Cloud in backend/src/main/java/com/stockmonitor/integration/RateLimiter.java

### Data Ingestion Jobs

- [x] T211 [P] Implement daily price ingestion job in backend/src/main/java/com/stockmonitor/batch/PriceIngestionJob.java
- [x] T212 [P] Implement fundamental data ingestion job in backend/src/main/java/com/stockmonitor/batch/FundamentalIngestionJob.java
- [x] T213 [P] Implement analyst estimates ingestion job in backend/src/main/java/com/stockmonitor/batch/EstimatesIngestionJob.java
- [x] T214 [P] Implement corporate calendar ingestion job in backend/src/main/java/com/stockmonitor/batch/CalendarIngestionJob.java
- [x] T215 [P] Implement FX rate ingestion job in backend/src/main/java/com/stockmonitor/batch/FxRateIngestionJob.java

### Data Validation

- [x] T216 [P] Implement price data validator in backend/src/main/java/com/stockmonitor/validation/PriceDataValidator.java
- [x] T217 [P] Implement fundamental data validator in backend/src/main/java/com/stockmonitor/validation/FundamentalDataValidator.java
- [x] T218 [P] Add anomaly detection for price data in backend/src/main/java/com/stockmonitor/validation/PriceAnomalyDetector.java
- [x] T218.5 [P] Implement anomaly detection for fundamental data per FR-040 in backend/src/main/java/com/stockmonitor/validation/FundamentalAnomalyDetector.java - Detect outliers using z-score method (flag if |z| > 3.0) for financial ratios (P/E, P/B, ROE, debt/equity), sudden changes (>50% QoQ change in revenue/earnings), and missing expected data. Log anomalies with severity (WARNING for single metric, ERROR for multiple metrics) and block ingestion on ERROR level.

### Caching

- [x] T219 [P] Implement Redis cache service for data feeds in backend/src/main/java/com/stockmonitor/service/DataFeedCacheService.java
- [x] T220 [P] Add cache invalidation logic in backend/src/main/java/com/stockmonitor/service/DataFeedCacheService.java

**Checkpoint**: External data integration complete, data flows into database with validation and caching

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

### Error Handling & Edge Cases

- [x] T221 [P] Implement empty state handling for all pages in frontend/src/components/common/EmptyState.tsx
- [x] T222 [P] Add error boundaries for all pages in frontend/src/components/common/ErrorBoundary.tsx
- [x] T223 [P] Implement graceful degradation for WebSocket failures in frontend/src/hooks/
- [x] T224 [P] Add constraint conflict validation with user-friendly messages in backend/src/main/java/com/stockmonitor/validation/ConstraintConflictValidator.java

### Performance Optimization

- [x] T225 [P] Implement query optimization for portfolio calculations in backend/src/main/java/com/stockmonitor/repository/
- [x] T226 [P] Add database connection pooling tuning in backend/src/main/resources/application.yml
- [x] T227 [P] Implement lazy loading for recommendations list in frontend/src/pages/Recommendations.tsx
- [x] T228 [P] Add React Query prefetching for dashboard in frontend/src/pages/Dashboard.tsx

### Security Hardening

- [x] T229 [P] Implement rate limiting on auth endpoints in backend/src/main/java/com/stockmonitor/security/RateLimitFilter.java
- [x] T230 [P] Add CSRF protection in backend/src/main/java/com/stockmonitor/security/CsrfConfig.java
- [x] T231 [P] Implement password reset functionality in backend/src/main/java/com/stockmonitor/service/PasswordResetService.java
- [x] T232 [P] Add audit logging for security-sensitive operations in backend/src/main/java/com/stockmonitor/service/AuditLogService.java
- [x] T232.5 [P] Implement audit log archival job with 7-year retention per constitution in backend/src/main/java/com/stockmonitor/batch/AuditLogArchivalJob.java - Scheduled job runs monthly to verify retention policy enforcement. Queries AuditLog for records >7 years old (should be empty due to TimescaleDB retention policy). Logs archival statistics (records archived, oldest record, storage usage). Job failure triggers alert.

### Observability

- [x] T233 [P] Configure ELK Stack integration in backend/src/main/resources/logback-spring.xml
- [x] T234 [P] Add custom metrics for recommendation engine in backend/src/main/java/com/stockmonitor/engine/RecommendationEngine.java
- [x] T235 [P] Create Grafana dashboards for system health in docs/grafana/
- [x] T236 [P] Configure alerting rules for data staleness in backend/src/main/java/com/stockmonitor/monitoring/AlertingService.java
- [x] T236.1 [P] Create Grafana dashboard for data feed health in docs/grafana/data-feed-health.json - Panels: data source status (green/yellow/red), last update time per source, staleness duration, refresh rate compliance. Alert threshold: any source stale >1 hour.
- [x] T236.2 [P] Create Grafana dashboard for API performance in docs/grafana/api-performance.json - Panels: request rate (req/s), p50/p95/p99 latency by endpoint, error rate (%), cache hit rate. Alert threshold: p95 latency >2s OR error rate >5%.
- [x] T236.3 [P] Create Grafana dashboard for prediction accuracy in docs/grafana/prediction-accuracy.json - Panels: confidence score distribution, prediction hit rate (% picks that outperformed), turnover vs cap compliance. Alert threshold: hit rate <50% (weekly aggregate).
- [x] T236.4 [P] Implement weekly metrics aggregation job in backend/src/main/java/com/stockmonitor/batch/WeeklyMetricsAggregationJob.java - Aggregate: avg recommendation confidence, avg turnover, avg run duration, data staleness incidents. Store in SystemMetricsWeekly table. Scheduled: Sunday 00:00 UTC.

### Documentation

- [x] T237 [P] Generate OpenAPI 3.0 spec from Spring controllers in backend/src/main/resources/openapi.yaml
- [x] T238 [P] Create API documentation site with Swagger UI in docs/api/
- [x] T239 [P] Update README.md with project overview and setup instructions
- [x] T240 [P] Validate quickstart.md by following steps in fresh environment

### Testing

- [x] T241 [P] Add E2E tests for onboarding flow with Playwright in frontend/tests/e2e/onboarding.spec.ts
- [x] T241.5 [P] Add cross-browser E2E tests per Assumption #9 in frontend/tests/e2e/browser-compatibility.spec.ts - Test Chrome, Firefox, Safari, Edge for critical flows: login, holdings upload, manual run trigger, report download. Validate UI rendering, form submissions, and data display consistency across browsers.
- [x] T242 [P] Add E2E tests for month-end workflow in frontend/tests/e2e/month-end.spec.ts
- [x] T243 [P] Add performance tests for recommendation engine in backend/src/test/java/com/stockmonitor/performance/RecommendationEnginePerformanceTest.java
- [x] T244 [P] Verify 80%+ test coverage requirement in backend/src/test/

### Deployment Preparation

- [x] T245 [P] Create Docker images for backend and frontend in docker/Dockerfile.backend and docker/Dockerfile.frontend
- [x] T246 [P] Create Kubernetes deployment manifests in k8s/
- [x] T247 [P] Configure CI/CD pipeline in .github/workflows/ci.yml
- [x] T248 [P] Create deployment runbook in docs/deployment.md

**Checkpoint**: System is production-ready with comprehensive testing, monitoring, and documentation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational completion - No dependencies on other stories
- **User Story 2 (Phase 4)**: Depends on Foundational completion - May integrate with US1 but independently testable
- **User Story 3 (Phase 5)**: Depends on Foundational completion - May integrate with US1 but independently testable
- **User Story 4 (Phase 6)**: Depends on Foundational completion - May integrate with US1 but independently testable
- **User Story 5 (Phase 7)**: Depends on Foundational completion - May integrate with US1 but independently testable
- **User Story 6 (Phase 8)**: Depends on Foundational completion - May integrate with US1 but independently testable
- **Data Integration (Phase 9)**: Can proceed in parallel with user stories once Foundational complete
- **Polish (Phase 10)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Uses RecommendationRun from US1 but independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Uses ConstraintSet from US1 but independently testable
- **User Story 4 (P4)**: Can start after Foundational (Phase 2) - Uses Portfolio from US1 but independently testable
- **User Story 5 (P5)**: Can start after Foundational (Phase 2) - Uses ConstraintSet from US1 but independently testable
- **User Story 6 (P6)**: Can start after Foundational (Phase 2) - Uses RecommendationRun from US1 but independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD mandate)
- Models before services
- Services before controllers
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

**Setup Phase (Phase 1)**:
- T002, T003, T004, T005, T006, T007, T008 can all run in parallel

**Foundational Phase (Phase 2)**:
- T010, T011, T012 database migrations can run in parallel
- T013-T016 entity creation can run in parallel
- T018-T025 configuration tasks can run in parallel
- T026-T032 frontend foundation can run in parallel
- T033-T035 test configuration can run in parallel

**User Story 1 (Phase 3)**:
- T036-T045 tests can all run in parallel
- T046, T047, T049, T050 auth tasks can run in parallel
- T051-T056 portfolio tasks have some dependencies but T055 can run in parallel
- T057-T059 universe tasks have dependencies
- T063-T070 recommendation engine tasks have dependencies but T069, T070 can run in parallel
- T073-T076 frontend auth pages can run in parallel
- T077-T080 portfolio setup pages can run in parallel
- T081-T088 dashboard/recommendations components can run in parallel

**Once Foundational completes, all user stories (US1-US6) can start in parallel if team capacity allows**

**Data Integration (Phase 9)**:
- T205-T210 client implementations can all run in parallel
- T211-T215 ingestion jobs can all run in parallel
- T216-T218 validators can all run in parallel

**Polish (Phase 10)**:
- All tasks marked [P] can run in parallel

---

## Parallel Example: User Story 1

Launch all tests for User Story 1 together:
```bash
# Tests (T036-T045)
Task: "Contract test for POST /api/auth/register"
Task: "Contract test for POST /api/auth/login"
Task: "Contract test for POST /api/portfolios/{id}/holdings/upload"
# ... all 10 tests in parallel
```

Launch parallelizable models/components:
```bash
# Auth components (T046, T049, T050)
Task: "Create UserDTO and registration request/response DTOs"
Task: "Implement email service with verification templates"
Task: "Add validation for email format and password complexity"

# Frontend pages (T073-T076)
Task: "Create registration page with email/password form"
Task: "Create login page with email/password form"
Task: "Create email verification page"
Task: "Create auth service with register, login, logout methods"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

**Total MVP Tasks**: T001-T091 (91 tasks)

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Add User Story 4 → Test independently → Deploy/Demo
6. Add User Story 5 → Test independently → Deploy/Demo
7. Add User Story 6 → Test independently → Deploy/Demo
8. Add Data Integration (Phase 9) → Real market data
9. Add Polish (Phase 10) → Production-ready

Each story adds value without breaking previous stories.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (T036-T091)
   - Developer B: User Story 2 (T092-T131)
   - Developer C: User Story 4 (T147-T169)
   - Developer D: Data Integration (T205-T220)
3. Stories complete and integrate independently

---

## Total Task Count

- **Phase 1 (Setup)**: 8 tasks
- **Phase 2 (Foundational)**: 27 tasks
- **Phase 3 (User Story 1)**: 56 tasks
- **Phase 4 (User Story 2)**: 40 tasks
- **Phase 5 (User Story 3)**: 15 tasks
- **Phase 6 (User Story 4)**: 23 tasks
- **Phase 7 (User Story 5)**: 20 tasks
- **Phase 8 (User Story 6)**: 15 tasks
- **Phase 9 (Data Integration)**: 16 tasks
- **Phase 10 (Polish)**: 28 tasks

**Total**: 248 tasks

**Tasks per User Story**:
- US1: 56 tasks
- US2: 40 tasks
- US3: 15 tasks
- US4: 23 tasks
- US5: 20 tasks
- US6: 15 tasks

**Parallel Opportunities**: 158 tasks marked [P] (64% of total can run in parallel within their phase)

**Independent Test Criteria**:
- User Story 1: Complete onboarding flow from signup to first recommendation
- User Story 2: Receive scheduled run with notifications and download report
- User Story 3: Modify constraints with preview and save
- User Story 4: View factor analysis and performance metrics
- User Story 5: Run backtest and view results
- User Story 6: Review exclusions with explanations

**Suggested MVP Scope**: User Story 1 only (91 tasks including Setup + Foundational)

---

## Notes

- [P] tasks = different files, no dependencies within phase
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Tests written FIRST per TDD mandate (Constitution Principle III)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Format strictly follows template requirements (checkbox, ID, optional [P], optional [Story], description with file path)
