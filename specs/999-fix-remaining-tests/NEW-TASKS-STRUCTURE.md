# New tasks.md Structure (Just-In-Time Design)

## ✅ RESTRUCTURED - Ready to Use with /speckit.implement

**Changes**: Design tasks are now embedded within each User Story phase as sub-phase A, enabling just-in-time design and incremental delivery.

---

## Complete Task Organization (355 tasks)

### **Phase 1: Setup** (8 tasks: T001-T008)
*Unchanged from original*

```
- [ ] T001 Create backend project structure...
- [ ] T002 [P] Create frontend project structure...
...
- [ ] T008 [P] Configure Flyway migrations framework...
```

---

### **Phase 2: Foundational** (27 tasks: T009-T035)
*Unchanged from original*

```
- [ ] T009 Create Flyway migration V1.0.0__initial_schema.sql...
- [ ] T010 [P] Create Flyway migration V1.0.1__add_indexes.sql...
...
- [ ] T035 [P] Create base test utilities and fixtures...
```

**Checkpoint**: Foundation ready

---

### **Phase 2.5: Design System Foundation** ⭐ NEW (14 tasks: D001-D014)

**Purpose**: Create universal design system needed for ALL pages

**Duration**: 3-5 days

**⚠️ CRITICAL**: Must be complete before ANY page mockups

#### Stage 1: Design System (7 tasks)

```
- [ ] D001 Define color palette with semantic naming in specs/001-month-end-analyst/design/design-system.md
- [ ] D002 [P] Establish typography scale (headings, body, monospace for numbers) in specs/001-month-end-analyst/design/design-system.md
- [ ] D003 [P] Document spacing system (4px/8px grid) in specs/001-month-end-analyst/design/design-system.md
- [ ] D004 [P] Define component library decisions (shadcn/ui + Tremor) in specs/001-month-end-analyst/design/design-system.md
- [ ] D005 [P] Create dark/light theme specifications in specs/001-month-end-analyst/design/design-system.md
- [ ] D006 Create Figma project structure with design system library
- [ ] D007 Document icon library and style in specs/001-month-end-analyst/design/design-system.md
```

**Approval Gate D1**:
```
[ ] Color palette approved by Product Owner
[ ] Typography reviewed by team
[ ] Spacing system validated
[ ] Component library decisions confirmed
✅ APPROVED → Proceed to Stage 2
```

#### Stage 2: Global Layout (7 tasks)

```
- [ ] D008 Create app shell mockup (sidebar, header, footer) in Figma
- [ ] D009 [P] Design navigation structure and menu states in Figma
- [ ] D010 [P] Design responsive layouts (mobile, tablet, desktop) in Figma
- [ ] D011 [P] Create empty state designs in Figma
- [ ] D012 [P] Create loading state designs (spinners, skeletons) in Figma
- [ ] D013 [P] Create error state designs (404, 500, network error) in Figma
- [ ] D014 Document layout specifications in specs/001-month-end-analyst/design/layout-specs.md
```

**Approval Gate D2**:
```
[ ] Layout reviewed by team
[ ] Navigation UX validated
[ ] Responsive behavior approved
[ ] All states designed
✅ APPROVED → User Story mockups can begin
```

**Checkpoint**: Design system foundation complete - page-specific design can begin

---

### **Phase 3: User Story 1** 🎯 MVP (105 tasks total)

**Goal**: Enable investor to sign up, upload holdings, select universe, and receive first ranked recommendations

**Deliverable**: Working MVP with complete onboarding flow

**Structure**: Design → Tests → Implementation (Just-In-Time!)

---

#### **Phase 3A: US1 Design & Mockups** (39 tasks: D015-D053)

**Purpose**: Design all US1 pages before implementing them

**Duration**: 1 week

**Prerequisites**: Phase 2.5 (Design System) complete

##### M001: Authentication Pages (8 tasks)

```
- [ ] D015 Create login page mockup with all states in Figma
- [ ] D016 [P] Create registration page mockup with validation states in Figma
- [ ] D017 [P] Create email verification page mockup in Figma
- [ ] D018 Design mobile responsive variants for auth pages in Figma
- [ ] D019 Document M001 specifications in specs/001-month-end-analyst/design/mockups/M001-login.md
- [ ] D020 Team review of M001 mockups (1 day for feedback)
- [ ] D021 Address M001 feedback and create revision in Figma
- [ ] D022 M001 final validation and approval by Product Owner
```

**Approval Gate M001**:
```
[ ] All auth states designed (default, loading, error, success)
[ ] Mobile + desktop responsive designs complete
[ ] Accessibility validated (contrast, focus states)
[ ] Component specifications documented
✅ APPROVED → Frontend auth pages (T073-T076) can begin
```

##### M002: Holdings Upload Page (9 tasks)

```
- [ ] D023 Create holdings upload page mockup with drag-drop zone in Figma
- [ ] D024 [P] Design holdings table with inline editing in Figma
- [ ] D025 [P] Design universe selector and coverage indicator in Figma
- [ ] D026 [P] Design CSV validation error states in Figma
- [ ] D027 Design mobile responsive variant for holdings page in Figma
- [ ] D028 Document M002 specifications in specs/001-month-end-analyst/design/mockups/M002-holdings.md
- [ ] D029 Team review of M002 mockups
- [ ] D030 Address M002 feedback and create revision in Figma
- [ ] D031 M002 final validation and approval by Product Owner
```

**Approval Gate M002**:
```
✅ APPROVED → Holdings page (T077-T080) can begin
```

##### M003: Dashboard Page (10 tasks)

```
- [ ] D032 Create dashboard layout with portfolio summary cards in Figma
- [ ] D033 [P] Design holdings table component for dashboard in Figma
- [ ] D034 [P] Design run control section (button, status badge, timer) in Figma
- [ ] D035 [P] Design data freshness indicators in Figma
- [ ] D036 [P] Design performance metrics cards in Figma
- [ ] D037 Design mobile responsive variant for dashboard in Figma
- [ ] D038 Document M003 specifications in specs/001-month-end-analyst/design/mockups/M003-dashboard.md
- [ ] D039 Team review of M003 mockups
- [ ] D040 Address M003 feedback and create revision in Figma
- [ ] D041 M003 final validation and approval by Product Owner
```

**Approval Gate M003**:
```
✅ APPROVED → Dashboard page (T081-T088) can begin
```

##### M004: Recommendations Page (12 tasks)

```
- [ ] D042 Create recommendations list layout with ranked cards in Figma
- [ ] D043 [P] Design recommendation card component with all states in Figma
- [ ] D044 [P] Design expandable explanation panel in Figma
- [ ] D045 [P] Design confidence score visualization in Figma
- [ ] D046 [P] Design change badges (NEW, INCREASED, DECREASED) in Figma
- [ ] D047 [P] Design disclaimer modal in Figma
- [ ] D048 [P] Design exclusions tab layout in Figma
- [ ] D049 Design mobile responsive variant for recommendations in Figma
- [ ] D050 Document M004 specifications in specs/001-month-end-analyst/design/mockups/M004-recommendations.md
- [ ] D051 Team review of M004 mockups
- [ ] D052 Address M004 feedback and create revision in Figma
- [ ] D053 M004 final validation and approval by Product Owner
```

**Approval Gate M004**:
```
✅ APPROVED → Recommendations page (T082-T091) can begin
```

**🎯 Checkpoint**: All US1 mockups approved - implementation can proceed

---

#### **Phase 3B: US1 Tests** (10 tasks: T036-T045)

**Purpose**: Write tests FIRST per TDD mandate

**Prerequisites**: Phase 3A (US1 mockups) approved

```
- [ ] T036 [P] [US1] Contract test for POST /api/auth/register in backend/src/test/java/com/stockmonitor/contract/AuthContractTest.java
- [ ] T037 [P] [US1] Contract test for POST /api/auth/login in backend/src/test/java/com/stockmonitor/contract/AuthContractTest.java
- [ ] T038 [P] [US1] Contract test for POST /api/portfolios/{id}/holdings/upload in backend/src/test/java/com/stockmonitor/contract/PortfolioContractTest.java
- [ ] T039 [P] [US1] Contract test for GET /api/universes in backend/src/test/java/com/stockmonitor/contract/UniverseContractTest.java
- [ ] T040 [P] [US1] Contract test for POST /api/runs in backend/src/test/java/com/stockmonitor/contract/RecommendationContractTest.java
- [ ] T041 [P] [US1] Contract test for GET /api/runs/{id}/recommendations in backend/src/test/java/com/stockmonitor/contract/RecommendationContractTest.java
- [ ] T042 [P] [US1] Integration test for user registration to first recommendation flow in backend/src/test/java/com/stockmonitor/integration/OnboardingFlowTest.java
- [ ] T043 [P] [US1] Unit test for CSV holdings parser in backend/src/test/java/com/stockmonitor/service/HoldingsCsvParserTest.java
- [ ] T044 [P] [US1] Unit test for portfolio calculation service in backend/src/test/java/com/stockmonitor/service/PortfolioCalculationServiceTest.java
- [ ] T045 [P] [US1] Unit test for constraint validation in backend/src/test/java/com/stockmonitor/service/ConstraintValidationServiceTest.java
```

**Checkpoint**: All tests written and FAILING (as expected) - ready for implementation

---

#### **Phase 3C: US1 Implementation** (56 tasks: T046-T091)

**Purpose**: Implement all US1 features with approved mockups as guide

**Prerequisites**:
- Phase 3A (US1 mockups) approved
- Phase 3B (US1 tests) complete

*[Contains all original T046-T091 tasks - authentication, portfolio management, recommendations, etc.]*

**🚀 MAJOR CHECKPOINT**: User Story 1 complete and shippable!

---

### **Phase 4: User Story 2** (61 tasks total)

**Structure**: Design → Tests → Implementation

#### **Phase 4A: US2 Design** (15 tasks: D054-D068)

**Purpose**: Design real-time features (notifications, WebSocket, reports)

**Prerequisites**: Phase 3 (US1) shipped

##### M005: Notifications System (6 tasks: D054-D059)
##### M006: Run Progress & WebSocket UI (5 tasks: D060-D064)
##### M007: Report Download & Change Indicators (4 tasks: D065-D068)

#### **Phase 4B: US2 Tests** (6 tasks: T092-T097)
#### **Phase 4C: US2 Implementation** (40 tasks: T098-T131)

**Checkpoint**: User Story 2 complete and shippable!

---

### **Phase 5: User Story 3** (28 tasks total)

#### **Phase 5A: US3 Design** (9 tasks: D069-D077)
- M008: Settings/Constraints Page (5 tasks)
- M009: Constraint Preview Panel (4 tasks)

#### **Phase 5B: US3 Tests** (4 tasks: T132-T135)
#### **Phase 5C: US3 Implementation** (15 tasks: T136-T146)

**Checkpoint**: User Story 3 complete!

---

### **Phase 6: User Story 4** (37 tasks total)

#### **Phase 6A: US4 Design** (10 tasks: D078-D087)
- M010: Factor Analysis Page (6 tasks)
- M011: Performance Metrics (4 tasks)

#### **Phase 6B: US4 Tests** (4 tasks: T147-T151)
#### **Phase 6C: US4 Implementation** (23 tasks: T152-T169)

**Checkpoint**: User Story 4 complete!

---

### **Phase 7: User Story 5** (31 tasks total)

#### **Phase 7A: US5 Design** (7 tasks: D088-D094)
- M012: Backtesting Page (7 tasks)

#### **Phase 7B: US5 Tests** (4 tasks: T170-T174)
#### **Phase 7C: US5 Implementation** (20 tasks: T175-T189)

**Checkpoint**: User Story 5 complete!

---

### **Phase 8: User Story 6** (24 tasks total)

#### **Phase 8A: US6 Design** (5 tasks: D095-D099)
- M013: Exclusions Interface (5 tasks)

#### **Phase 8B: US6 Tests** (4 tasks: T190-T192)
#### **Phase 8C: US6 Implementation** (15 tasks: T193-T204)

**Checkpoint**: All user stories complete!

---

### **Phase 9: Data Integration** (16 tasks: T205-T220)
*Unchanged - can run parallel with user stories*

### **Phase 10: Polish & Cross-Cutting** (28 tasks: T221-T248)
*Unchanged - final polish*

### **Phase N: Design Assets Export** (8 tasks: D100-D107)
**Purpose**: Export all design assets at the end

---

## Dependencies & Execution Order

### **Linear Path (MVP)**:
```
Phase 1 → Phase 2 → Phase 2.5 (Design Foundation) →
Phase 3A (US1 Design) → Phase 3B (US1 Tests) → Phase 3C (US1 Impl) →
✅ SHIP US1!
```

### **Incremental Delivery**:
```
After US1 ships:
Phase 4A (US2 Design) → Phase 4B → Phase 4C → ✅ SHIP US2!
Phase 5A (US3 Design) → Phase 5B → Phase 5C → ✅ SHIP US3!
...
```

### **Parallel Opportunities**:
- Backend APIs can be developed while mockups are being designed
- Multiple designers can work on different user story mockups
- Different developers can implement different user stories (after mockups approved)

---

## Timeline Comparison

### **Old Way (All Design Upfront)**:
- Week 1-3: Design all 6 user stories
- Week 4-5: Implement US1
- Week 6: **FIRST SHIP** ← 6 weeks to value

### **New Way (Just-In-Time)**: ⭐
- Week 1: Design system + US1 design
- Week 2-3: Implement US1
- Week 3: **FIRST SHIP** ← 3 weeks to value (50% faster!)
- Week 4: Design US2 while implementing
- Week 5: Ship US2
- ...incremental value every 2 weeks

---

## Total Task Count: 355 tasks

| Phase | Tasks | Type |
|-------|-------|------|
| Phase 1 | 8 | Setup |
| Phase 2 | 27 | Foundational |
| Phase 2.5 | 14 | Design Foundation |
| Phase 3 | 105 | US1 (39 design + 10 tests + 56 impl) |
| Phase 4 | 61 | US2 (15 design + 6 tests + 40 impl) |
| Phase 5 | 28 | US3 (9 design + 4 tests + 15 impl) |
| Phase 6 | 37 | US4 (10 design + 4 tests + 23 impl) |
| Phase 7 | 31 | US5 (7 design + 4 tests + 20 impl) |
| Phase 8 | 24 | US6 (5 design + 4 tests + 15 impl) |
| Phase 9 | 16 | Data Integration |
| Phase 10 | 28 | Polish |
| Phase N | 8 | Assets Export |
| **TOTAL** | **355** | |

---

## How /speckit.implement Will Work

```bash
$ /speckit.implement

Scanning: specs/001-month-end-analyst/tasks.md
Found: 355 tasks organized in just-in-time structure

Current Phase: Phase 3A (US1 Design)
Progress: 0/39 tasks complete

Next task: D015 Create login page mockup

Dependencies:
✅ Phase 2.5 complete (Design system approved)
⏸️ Phase 3B blocked (waiting for Phase 3A approval)
⏸️ Phase 3C blocked (waiting for Phase 3B tests)

Ready to begin US1 design!
```

---

## ✅ Success! Structure Ready

**What you get:**
- Just-in-time design approach
- Incremental delivery per user story
- Ship US1 50% faster
- Learn and adapt between stories
- Designer and developer collaboration
- Flexible prioritization

**Next steps:**
1. Start Phase 1 (Setup)
2. Complete Phase 2 (Foundational)
3. Complete Phase 2.5 (Design System)
4. Start Phase 3A (US1 Design)
5. Get approval, then implement!

**Ready to build!** 🚀
