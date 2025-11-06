# Phase 2.5: Design System & Mockups (INSERT AFTER PHASE 2)

**Purpose**: Create and validate all UI mockups before implementation begins

**⚠️ CRITICAL**: This phase gates User Story implementation. No coding begins until mockups are approved.

**Duration**: 2-3 weeks (can run parallel with backend Phase 2 work)

---

## Stage 1: Design System Foundation (3 days)

**Purpose**: Establish visual language and component standards

- [ ] D001 Define color palette with semantic naming in specs/001-month-end-analyst/design/design-system.md
- [ ] D002 [P] Establish typography scale (headings, body, monospace for numbers) in specs/001-month-end-analyst/design/design-system.md
- [ ] D003 [P] Document spacing system (4px/8px grid) in specs/001-month-end-analyst/design/design-system.md
- [ ] D004 [P] Define component library decisions (shadcn/ui + Tremor) in specs/001-month-end-analyst/design/design-system.md
- [ ] D005 [P] Create dark/light theme specifications in specs/001-month-end-analyst/design/design-system.md
- [ ] D006 Create Figma project structure with design system library
- [ ] D007 Document icon library and style in specs/001-month-end-analyst/design/design-system.md

**Approval Gate D1**:
```
[ ] Color palette approved by Product Owner
[ ] Typography reviewed by team
[ ] Spacing system validated
[ ] Component library decisions confirmed
✅ APPROVED → Proceed to Stage 2
```

**Checkpoint**: Design system documented and approved. Layout mockups can begin.

---

## Stage 2: Global Layout & Shell (2 days)

**Purpose**: Design app shell and navigation structure before page-specific mockups

- [ ] D008 Create app shell mockup (sidebar, header, footer) in Figma
- [ ] D009 [P] Design navigation structure and menu states in Figma
- [ ] D010 [P] Design responsive layouts (mobile, tablet, desktop) in Figma
- [ ] D011 [P] Create empty state designs in Figma
- [ ] D012 [P] Create loading state designs (spinners, skeletons) in Figma
- [ ] D013 [P] Create error state designs (404, 500, network error) in Figma
- [ ] D014 Document layout specifications in specs/001-month-end-analyst/design/layout-specs.md

**Approval Gate D2**:
```
[ ] Layout reviewed by team
[ ] Navigation UX validated
[ ] Responsive behavior approved
[ ] All states designed
✅ APPROVED → Proceed to Stage 3
```

**Checkpoint**: Global layout approved. Page-specific mockups can begin.

---

## Stage 3: User Story 1 Mockups (MVP Pages) (5 days)

**Purpose**: Create and approve all mockups for MVP before US1 implementation begins

### M001: Authentication Pages (Login/Registration)

- [ ] D015 Create login page mockup with all states in Figma
- [ ] D016 [P] Create registration page mockup with validation states in Figma
- [ ] D017 [P] Create email verification page mockup in Figma
- [ ] D018 Design mobile responsive variants for auth pages in Figma
- [ ] D019 Document M001 specifications in specs/001-month-end-analyst/design/mockups/M001-login.md
- [ ] D020 Team review of M001 mockups (1 day for feedback)
- [ ] D021 Address M001 feedback and create revision in Figma
- [ ] D022 M001 final validation and approval by Product Owner

**Approval Gate M001**:
```
[ ] All auth states designed (default, loading, error, success)
[ ] Mobile + desktop responsive designs complete
[ ] Accessibility validated (contrast, focus states)
[ ] Component specifications documented
[ ] Design handoff complete
[ ] Product Owner sign-off
✅ APPROVED → Tasks T073-T076 can begin
```

---

### M002: Holdings Upload Page

- [ ] D023 Create holdings upload page mockup with drag-drop zone in Figma
- [ ] D024 [P] Design holdings table with inline editing in Figma
- [ ] D025 [P] Design universe selector and coverage indicator in Figma
- [ ] D026 [P] Design CSV validation error states in Figma
- [ ] D027 Design mobile responsive variant for holdings page in Figma
- [ ] D028 Document M002 specifications in specs/001-month-end-analyst/design/mockups/M002-holdings.md
- [ ] D029 Team review of M002 mockups
- [ ] D030 Address M002 feedback and create revision in Figma
- [ ] D031 M002 final validation and approval by Product Owner

**Approval Gate M002**:
```
[ ] Upload states designed (empty, uploading, success, error)
[ ] Table editing interactions specified
[ ] Validation error display designed
[ ] Component specifications documented
[ ] Product Owner sign-off
✅ APPROVED → Tasks T077-T080 can begin
```

---

### M003: Dashboard Page

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

**Approval Gate M003**:
```
[ ] All metric displays designed with positive/negative states
[ ] Run button states (enabled, disabled, loading) designed
[ ] Data freshness badges (current, stale, error) designed
[ ] Component specifications documented
[ ] Product Owner sign-off
✅ APPROVED → Tasks T081, T082, T086-T088 can begin
```

---

### M004: Recommendations Page

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

**Approval Gate M004**:
```
[ ] Ranked list design complete with all card states
[ ] Explanation panel interactions specified
[ ] Disclaimer modal designed and approved
[ ] Tabs (Recommendations/Exclusions) designed
[ ] Component specifications documented
[ ] Product Owner sign-off
✅ APPROVED → Tasks T082-T085, T089-T091 can begin
```

---

**🎯 MAJOR CHECKPOINT: User Story 1 Mockups Complete**

```
All US1 mockups approved:
✅ M001: Authentication (D015-D022)
✅ M002: Holdings Upload (D023-D031)
✅ M003: Dashboard (D032-D041)
✅ M004: Recommendations (D042-D053)

Gate Status: ✅ APPROVED
→ Phase 3 (User Story 1 Implementation) can begin
→ Start with T036 (US1 contract tests)
```

---

## Stage 4: User Story 2 Mockups (4 days)

**Purpose**: Design real-time features (notifications, WebSocket, reports)

**Note**: These can be created while US1 is being implemented

### M005: Notifications System

- [ ] D054 [P] Design notification bell component with badge in Figma
- [ ] D055 [P] Design notification dropdown list in Figma
- [ ] D056 [P] Design toast notification component in Figma
- [ ] D057 [P] Design notification categories and priorities in Figma
- [ ] D058 Document M005 specifications in specs/001-month-end-analyst/design/mockups/M005-notifications.md
- [ ] D059 Team review and approval of M005 mockups

**Approval Gate M005**:
```
[ ] Bell component with unread count designed
[ ] Dropdown list with read/unread states designed
[ ] Toast variants (success, error, info) designed
✅ APPROVED → Tasks T119-T123 can begin
```

---

### M006: Run Progress & WebSocket UI

- [ ] D060 [P] Design run status badges (QUEUED, RUNNING, FINALIZED) in Figma
- [ ] D061 [P] Design progress bar component with stage labels in Figma
- [ ] D062 [P] Design WebSocket connection indicator in Figma
- [ ] D063 Document M006 specifications in specs/001-month-end-analyst/design/mockups/M006-progress.md
- [ ] D064 Team review and approval of M006 mockups

**Approval Gate M006**:
```
[ ] Status badges designed for all run states
[ ] Progress bar with percentage and stage label designed
[ ] Connection indicator (connected/disconnected) designed
✅ APPROVED → Tasks T124-T127 can begin
```

---

### M007: Report Download & Change Indicators

- [ ] D065 [P] Design report download button and modal in Figma
- [ ] D066 [P] Design change indicator badges in Figma
- [ ] D067 Document M007 specifications in specs/001-month-end-analyst/design/mockups/M007-reports.md
- [ ] D068 Team review and approval of M007 mockups

**Approval Gate M007**:
```
[ ] Report download flow designed
[ ] Change badges (NEW, INCREASED, etc.) designed
✅ APPROVED → Tasks T128-T131 can begin
```

---

**CHECKPOINT: User Story 2 Mockups Complete → Phase 4 (US2 Implementation) can begin**

---

## Stage 5: User Story 3 Mockups (3 days)

**Purpose**: Design constraint editor with preview

### M008: Settings/Constraints Page

- [ ] D069 Design constraint editor form layout in Figma
- [ ] D070 [P] Design constraint input components with tooltips in Figma
- [ ] D071 [P] Design constraint validation states in Figma
- [ ] D072 Document M008 specifications in specs/001-month-end-analyst/design/mockups/M008-settings.md
- [ ] D073 Team review and approval of M008 mockups

**Approval Gate M008**:
```
[ ] All constraint inputs designed with labels and tooltips
[ ] Validation error states designed
✅ APPROVED → Tasks T142-T143 can begin
```

---

### M009: Constraint Preview Panel

- [ ] D074 [P] Design preview panel with impact estimates in Figma
- [ ] D075 [P] Design preview loading and result states in Figma
- [ ] D076 Document M009 specifications in specs/001-month-end-analyst/design/mockups/M009-preview.md
- [ ] D077 Team review and approval of M009 mockups

**Approval Gate M009**:
```
[ ] Preview panel designed with all metrics
[ ] Loading state designed
✅ APPROVED → Tasks T144-T146 can begin
```

---

**CHECKPOINT: User Story 3 Mockups Complete → Phase 5 (US3 Implementation) can begin**

---

## Stage 6: User Story 4 Mockups (4 days)

**Purpose**: Design factor analysis and performance metrics

### M010: Factor Analysis Page

- [ ] D078 Design factor heatmap layout in Figma
- [ ] D079 [P] Design factor tooltips with explanations in Figma
- [ ] D080 [P] Design color scale legend in Figma
- [ ] D081 [P] Design sector filter controls in Figma
- [ ] D082 Document M010 specifications in specs/001-month-end-analyst/design/mockups/M010-factors.md
- [ ] D083 Team review and approval of M010 mockups

**Approval Gate M010**:
```
[ ] Heatmap color scale designed (red-yellow-green)
[ ] Cell hover tooltips designed
[ ] Legend and filters designed
✅ APPROVED → Tasks T165-T169 can begin
```

---

### M011: Performance Metrics

- [ ] D084 [P] Design contributors/detractors tables in Figma
- [ ] D085 [P] Design benchmark comparison chart in Figma
- [ ] D086 Document M011 specifications in specs/001-month-end-analyst/design/mockups/M011-performance.md
- [ ] D087 Team review and approval of M011 mockups

**Approval Gate M011**:
```
[ ] Contributors/detractors tables designed
[ ] Benchmark chart designed
✅ APPROVED → Tasks T162-T164 can begin
```

---

**CHECKPOINT: User Story 4 Mockups Complete → Phase 6 (US4 Implementation) can begin**

---

## Stage 7: User Story 5 Mockups (3 days)

**Purpose**: Design backtesting interface

### M012: Backtesting Page

- [ ] D088 Design backtest parameter form in Figma
- [ ] D089 [P] Design equity curve chart layout in Figma
- [ ] D090 [P] Design performance metrics table in Figma
- [ ] D091 [P] Design turnover history chart in Figma
- [ ] D092 [P] Design benchmark comparison verdict in Figma
- [ ] D093 Document M012 specifications in specs/001-month-end-analyst/design/mockups/M012-backtests.md
- [ ] D094 Team review and approval of M012 mockups

**Approval Gate M012**:
```
[ ] Parameter form with all inputs designed
[ ] Charts and tables designed
[ ] Loading states designed
✅ APPROVED → Tasks T183-T189 can begin
```

---

**CHECKPOINT: User Story 5 Mockups Complete → Phase 7 (US5 Implementation) can begin**

---

## Stage 8: User Story 6 Mockups (2 days)

**Purpose**: Design exclusions interface

### M013: Exclusions Interface

- [ ] D095 [P] Design exclusions list component in Figma
- [ ] D096 [P] Design exclusion detail modal in Figma
- [ ] D097 [P] Design "No trade" banner in Figma
- [ ] D098 Document M013 specifications in specs/001-month-end-analyst/design/mockups/M013-exclusions.md
- [ ] D099 Team review and approval of M013 mockups

**Approval Gate M013**:
```
[ ] Exclusions list with reason badges designed
[ ] Detail modal designed
[ ] "No trade" guidance designed
✅ APPROVED → Tasks T199-T204 can begin
```

---

**CHECKPOINT: User Story 6 Mockups Complete → Phase 8 (US6 Implementation) can begin**

---

## Stage 9: Design Assets & Handoff (2 days)

**Purpose**: Prepare all assets and documentation for developers

- [ ] D100 [P] Export all icons as SVG in specs/001-month-end-analyst/design/assets/icons/
- [ ] D101 [P] Export all images as WebP + PNG fallback in specs/001-month-end-analyst/design/assets/images/
- [ ] D102 [P] Create component specifications document in specs/001-month-end-analyst/design/handoff/component-specs.md
- [ ] D103 [P] Document all interactions and animations in specs/001-month-end-analyst/design/handoff/interaction-notes.md
- [ ] D104 Create Figma links reference document in specs/001-month-end-analyst/design/figma-links.md
- [ ] D105 [P] Take screenshots of all approved mockups in specs/001-month-end-analyst/design/assets/screenshots/
- [ ] D106 Create design QA checklist in specs/001-month-end-analyst/design/design-qa-checklist.md
- [ ] D107 Final design system documentation review and publish

**Checkpoint**: All design assets ready. Developers have everything needed for implementation.

---

## Design Phase Summary

**Total Design Tasks**: 107 tasks (D001-D107)

**Breakdown by Stage**:
- Stage 1 (Design System): 7 tasks (D001-D007)
- Stage 2 (Global Layout): 7 tasks (D008-D014)
- Stage 3 (US1 Mockups): 39 tasks (D015-D053)
- Stage 4 (US2 Mockups): 15 tasks (D054-D068)
- Stage 5 (US3 Mockups): 9 tasks (D069-D077)
- Stage 6 (US4 Mockups): 10 tasks (D078-D087)
- Stage 7 (US5 Mockups): 7 tasks (D088-D094)
- Stage 8 (US6 Mockups): 5 tasks (D095-D099)
- Stage 9 (Assets & Handoff): 8 tasks (D100-D107)

**Parallel Opportunities**: 38 tasks marked [P] can run in parallel

**Critical Path**:
1. Design System (3 days) →
2. Global Layout (2 days) →
3. US1 Mockups (5 days) →
4. US1 Implementation can begin

**While US1 is being coded**: Design US2-6 mockups in parallel

**Total Duration**: 2-3 weeks (overlaps with Phase 2 backend work)

---

## Updated Phase Dependencies

```
Phase 1: Setup (8 tasks)
         ↓
Phase 2: Foundational Backend (27 tasks) ← Can run in parallel with Phase 2.5
         ↓
Phase 2.5: Design System & Mockups (107 tasks) ← NEW
         ↓ Approval Gate: All US1 mockups approved?
Phase 3: User Story 1 Implementation (56 tasks)
         (Can start once M001-M004 approved, even if US2-6 mockups still in progress)
         ↓
Phase 4-8: User Stories 2-6 Implementation
         (Each starts when its mockups are approved)
         ↓
Phase 9: Data Integration (16 tasks)
         ↓
Phase 10: Polish (28 tasks)
```

---

## Notes

- [P] tasks = different mockups/files, no dependencies
- Each mockup has a review-revise-approve cycle (2-3 days per mockup)
- Design work can overlap with backend development
- Mockups for US2-6 can be created while US1 is being coded
- All mockups must be approved before their implementation tasks begin
- Design QA happens during implementation to ensure fidelity
