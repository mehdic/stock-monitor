# Design-First Integration Plan for Speckit

## Overview

This document outlines the integration plan for making design-first workflow an intrinsic part of the Speckit framework.

## Integration Architecture

### Current Speckit Workflow
```
/speckit.constitution → constitution.md
↓
/speckit.specify → spec.md (user stories)
↓
/speckit.plan → plan.md, research.md, data-model.md, contracts/
↓
/speckit.tasks → tasks.md (implementation tasks)
↓
/speckit.implement → executes tasks.md
```

### Design-First Enhanced Workflow
```
/speckit.constitution → constitution.md
↓
/speckit.specify → spec.md (user stories)
↓
/speckit.plan → plan.md, research.md, data-model.md, contracts/
                (optionally: design-system.md if design-first detected)
↓
/speckit.tasks [WITH design-first flag] → tasks.md (design + implementation tasks)
↓
/speckit.implement → executes tasks.md with approval gate support
```

## Key Integration Points

### 1. User Input Detection (commands/tasks.md)
**File**: `otherProjects/speckit/templates/commands/tasks.md`
**Lines**: 10-14 (User Input section)

**Modification**:
```markdown
## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

**Design-First Detection**: Check if user input contains any of these keywords:
- "design-first", "design first"
- "mockups", "wireframes", "prototypes"
- "UI design", "visual design", "UX design"
- "approve mockups before implementation"

If detected, set `DESIGN_FIRST_MODE=true` and follow design-first task generation pattern.
```

### 2. Task Generation Workflow Enhancement (commands/tasks.md)
**File**: `otherProjects/speckit/templates/commands/tasks.md`
**Lines**: 25-58 (Execute task generation workflow)

**Add new section**:
```markdown
3. **Execute task generation workflow**:
   - Load plan.md and extract tech stack, libraries, project structure
   - Load spec.md and extract user stories with their priorities (P1, P2, P3, etc.)
   - **NEW**: If DESIGN_FIRST_MODE=true:
     - Generate Phase 2.5: Design System Foundation tasks (D001-D014)
     - For each user story, create nested A/B/C structure:
       - Phase XA: Design & Mockups (D### tasks with M### mockup IDs)
       - Phase XB: Tests (T### tasks, if requested)
       - Phase XC: Implementation (T### tasks)
     - Add approval gates between Phase XA and XB
   - **ELSE**: Use standard structure (Tests → Implementation per story)
   - If data-model.md exists: Extract entities and map to user stories
   - If contracts/ exists: Map endpoints to user stories
   - If research.md exists: Extract decisions for setup tasks
   - Generate dependency graph showing user story completion order
   - Create parallel execution examples per user story
   - Validate task completeness
```

### 3. Task Template Structure (tasks-template.md)
**File**: `otherProjects/speckit/templates/tasks-template.md`
**Lines**: Entire file structure

**Add new Phase 2.5 section** (between Phase 2 and Phase 3):
```markdown
---

## Phase 2.5: Design System Foundation (DESIGN-FIRST ONLY)

**Purpose**: Create universal design system needed for ALL user stories
**Prerequisites**: Phase 2 (Foundational) complete
**Blocks**: All user story design work (Phase XA)

**⚠️ ONLY INCLUDE THIS PHASE IF USER REQUESTS DESIGN-FIRST APPROACH**

### Design System Core

- [ ] D001 Define color palette with semantic naming in specs/[###-feature]/design/design-system.md
- [ ] D002 [P] Establish typography scale (headings, body, captions) in design-system.md
- [ ] D003 [P] Define spacing system (margins, padding, gaps) in design-system.md
- [ ] D004 [P] Create elevation/shadow system for depth in design-system.md
- [ ] D005 [P] Define border radius scale for consistency in design-system.md

### Global Layout

- [ ] D006 Define responsive breakpoints (mobile, tablet, desktop) in design-system.md
- [ ] D007 [P] Create grid system specification in design-system.md
- [ ] D008 [P] Define navigation patterns (header, sidebar, mobile menu) in design-system.md

### Universal Components

- [ ] D009 [P] Design button variants (primary, secondary, danger, ghost) in design-system.md
- [ ] D010 [P] Design form input components (text, select, checkbox, radio) in design-system.md
- [ ] D011 [P] Design feedback components (alerts, toasts, modals) in design-system.md
- [ ] D012 [P] Design loading states and skeletons in design-system.md
- [ ] D013 [P] Design error and empty states in design-system.md

### Design System Approval

- [ ] D014 Present design system to stakeholders and get approval

**Approval Gate**: ✅ APPROVED → All user story design phases (3A, 4A, 5A...) can begin in parallel

---
```

**Modify Phase 3 structure** (User Story 1):
```markdown
## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Phase 3A: US1 Design & Mockups (DESIGN-FIRST ONLY) ⚠️

**Purpose**: Design all US1 pages/screens before implementing them
**Prerequisites**: Phase 2.5 (Design System) approved
**Blocks**: Phase 3B (US1 Tests)

**⚠️ ONLY INCLUDE THIS SUB-PHASE IF USER REQUESTS DESIGN-FIRST APPROACH**

#### M001: [First Component/Page Name] (e.g., Authentication Pages)

- [ ] D015 Create [specific page] mockup with all states (loading, error, success) in Figma/tool
- [ ] D016 [P] Create [another page] mockup with validation states in Figma/tool
- [ ] D017 [P] Design [component] interactions and animations
- [ ] D018 Design [component] responsive behavior (mobile, tablet, desktop)
- [ ] D019 Add accessibility annotations (ARIA labels, keyboard navigation)
- [ ] D020 Document component specifications in specs/[###-feature]/design/mockups/m001-[name].md
- [ ] D021 M001 design review with team
- [ ] D022 M001 final validation and approval by Product Owner

**Approval Gate M001**: ✅ APPROVED → Related implementation tasks can begin

#### M002: [Second Component/Page Name]

[Similar structure as M001...]

**Checkpoint**: All US1 mockups (M001-M00X) approved → Phase 3B can begin

---

### Phase 3B: US1 Tests (OPTIONAL - only if tests requested) ⚠️

**Purpose**: Write tests FIRST per TDD mandate
**Prerequisites**: Phase 3A (US1 mockups) approved

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T010 [P] [US1] Contract test for [endpoint] in tests/contract/test_[name].py
- [ ] T011 [P] [US1] Integration test for [user journey] in tests/integration/test_[name].py

---

### Phase 3C: US1 Implementation

**Purpose**: Implement all US1 features with approved mockups as guide
**Prerequisites**: Phase 3B (US1 Tests) complete

- [ ] T012 [P] [US1] Create [Entity1] model in src/models/[entity1].py
- [ ] T013 [P] [US1] Create [Entity2] model in src/models/[entity2].py
- [ ] T014 [US1] Implement [Service] in src/services/[service].py (depends on T012, T013)
- [ ] T015 [US1] Implement [endpoint/feature] in src/[location]/[file].py
- [ ] T016 [US1] Add validation and error handling
- [ ] T017 [US1] Add logging for user story 1 operations

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---
```

### 4. Approval Gate Handling (commands/implement.md)
**File**: `otherProjects/speckit/templates/commands/implement.md`
**Lines**: 106-114 (Execute implementation section)

**Add approval gate detection**:
```markdown
6. Execute implementation following the task plan:
   - **Phase-by-phase execution**: Complete each phase before moving to the next
   - **Approval gate detection** (DESIGN-FIRST):
     - Scan tasks.md for "**Approval Gate [ID]**:" markers
     - When reached, display: "[Mockup ID] completed. Review required before proceeding."
     - Prompt: "Have you reviewed and approved [Mockup ID]? (yes/no/show-details)"
     - If "show-details": Display design artifacts location
     - If "no": Halt execution, save progress
     - If "yes": Mark gate as passed, proceed to next phase
   - **Respect dependencies**: Run sequential tasks in order, parallel tasks [P] can run together
   - **Follow TDD approach**: Execute test tasks before their corresponding implementation tasks
   - **File-based coordination**: Tasks affecting the same files must run sequentially
   - **Validation checkpoints**: Verify each phase completion before proceeding
```

### 5. New Design System Template
**File**: `otherProjects/speckit/templates/design-system-template.md` (NEW)

**Purpose**: Template for design system documentation

```markdown
# Design System: [FEATURE NAME]

**Project**: [Project Name]
**Feature**: [###-feature-name]
**Version**: 1.0
**Last Updated**: [DATE]

## Color Palette

### Primary Colors
```color
primary-50: #...
primary-100: #...
...
primary-900: #...
```

### Secondary Colors
[Similar structure]

### Semantic Colors
- Success: #...
- Warning: #...
- Error: #...
- Info: #...

### Neutrals
[Gray scale]

## Typography

### Font Families
- Primary: [Font Name]
- Monospace: [Font Name]

### Type Scale
| Level | Size | Weight | Line Height | Use Case |
|-------|------|--------|-------------|----------|
| H1    | 2.5rem | 700 | 1.2 | Page titles |
| H2    | 2rem | 600 | 1.3 | Section headers |
| H3    | 1.5rem | 600 | 1.4 | Subsection headers |
| Body  | 1rem | 400 | 1.5 | Body text |
| Caption | 0.875rem | 400 | 1.4 | Supporting text |

## Spacing System

Based on 4px grid:
- xs: 4px (0.25rem)
- sm: 8px (0.5rem)
- md: 16px (1rem)
- lg: 24px (1.5rem)
- xl: 32px (2rem)
- 2xl: 48px (3rem)
- 3xl: 64px (4rem)

## Elevation

| Level | Shadow | Use Case |
|-------|--------|----------|
| 0     | none   | Flat elements |
| 1     | 0 1px 2px rgba(0,0,0,0.05) | Cards |
| 2     | 0 2px 4px rgba(0,0,0,0.1) | Dropdowns |
| 3     | 0 4px 8px rgba(0,0,0,0.15) | Modals |
| 4     | 0 8px 16px rgba(0,0,0,0.2) | Navigation |

## Border Radius

- none: 0
- sm: 4px
- md: 8px
- lg: 12px
- xl: 16px
- full: 9999px (circular)

## Responsive Breakpoints

| Breakpoint | Min Width | Use Case |
|------------|-----------|----------|
| mobile     | 0px       | Small phones |
| sm         | 640px     | Large phones |
| md         | 768px     | Tablets |
| lg         | 1024px    | Laptops |
| xl         | 1280px    | Desktops |
| 2xl        | 1536px    | Large screens |

## Component Specifications

### Buttons

**Variants**:
- Primary: [color, hover state, active state]
- Secondary: [color, hover state, active state]
- Danger: [color, hover state, active state]
- Ghost: [color, hover state, active state]

**Sizes**:
- sm: padding-x: 12px, padding-y: 6px, font-size: 0.875rem
- md: padding-x: 16px, padding-y: 8px, font-size: 1rem
- lg: padding-x: 20px, padding-y: 10px, font-size: 1.125rem

**States**:
- Default
- Hover
- Active
- Disabled
- Loading

### Form Inputs

[Similar component spec structure]

### Feedback Components

[Alerts, toasts, modals specifications]

## Animation & Motion

### Timing Functions
- ease-in: cubic-bezier(0.4, 0, 1, 1)
- ease-out: cubic-bezier(0, 0, 0.2, 1)
- ease-in-out: cubic-bezier(0.4, 0, 0.2, 1)

### Duration Scale
- fast: 150ms
- normal: 250ms
- slow: 350ms

### Common Transitions
- Fade: opacity, duration-normal, ease-in-out
- Slide: transform, duration-normal, ease-out
- Scale: transform, duration-fast, ease-in-out

## Accessibility

### Color Contrast
- All text must meet WCAG AA standards (4.5:1 for normal text, 3:1 for large text)
- Interactive elements must have 3:1 contrast with background

### Focus States
- Visible focus indicator on all interactive elements
- Focus ring: 2px solid primary-500, offset 2px

### ARIA Labels
- All interactive elements must have descriptive labels
- Icons must have aria-labels or sr-only text

## Tools & Resources

**Design Tool**: Figma / [Other]
**Icon Library**: [Library Name]
**Prototype Tool**: [Tool Name]
```

## Implementation Steps

### Step 1: Modify tasks-template.md
Add Phase 2.5 and nested A/B/C structure for user stories when design-first is detected.

### Step 2: Modify commands/tasks.md
Add design-first detection logic and conditional task generation.

### Step 3: Create design-system-template.md
New template for design system documentation.

### Step 4: Modify commands/implement.md
Add approval gate detection and prompting logic.

### Step 5: Test Integration
Run through a test feature with design-first approach to validate.

## Task ID Conventions (Design-First)

### Design Tasks: D###
- D001-D014: Design System Foundation (Phase 2.5)
- D015+: User story-specific design tasks

### Mockup IDs: M###
- M001: First mockup group (e.g., Authentication Pages)
- M002: Second mockup group (e.g., Dashboard)
- Each M### represents a logical grouping of related pages/components

### Implementation Tasks: T###
- T001+: Standard implementation tasks (maintain existing convention)

## Benefits of This Integration

### 1. **Seamless Activation**
User simply mentions "design-first" when running `/speckit.tasks` and gets the full design workflow.

### 2. **Backward Compatible**
Existing projects without design-first continue to work unchanged.

### 3. **Just-In-Time Design**
Design System Foundation → Design US1 → Implement US1 → Ship US1
Each user story gets designed right before implementation.

### 4. **Clear Approval Gates**
No implementation without approved mockups. Gates built into the workflow.

### 5. **Parallel Opportunities**
Once Design System is approved, all user story design phases (3A, 4A, 5A) can run in parallel.

### 6. **Proper Tracking**
All design and implementation tasks tracked in single tasks.md file, fully compatible with `/speckit.implement`.

## Timeline Impact

### Standard Approach (No Design-First)
```
Week 1-2: Setup + Foundational
Week 3: Implement US1
Week 4: Implement US2
Week 5: Ship
```

### Design-First Approach
```
Week 1: Setup + Foundational
Week 2: Design System Foundation (D001-D014)
Week 3: Design US1 (3A) + Start Implementation (3C)
Week 4: Complete US1 → SHIP US1 (50% faster to first value!)
Week 5: Design US2 (4A) + Implement US2 (4C)
Week 6: SHIP US2
```

## Success Metrics

✅ User can activate design-first with simple keyword
✅ All design tasks properly sequenced before implementation
✅ Approval gates prevent premature implementation
✅ `/speckit.implement` honors design phases
✅ Existing projects unaffected (backward compatible)
✅ 50% faster time-to-first-ship with incremental delivery
