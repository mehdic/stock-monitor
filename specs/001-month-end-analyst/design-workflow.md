# Design-First Workflow: Month-End Market Analyst

**Purpose**: This document defines the design validation process that gates implementation tasks.

**Principle**: No implementation task begins until its corresponding mockup is reviewed and approved.

---

## Design Phase Structure

```
Phase 1: Setup (Infrastructure)
         ↓
Phase 2: Foundational (Backend/DB/Auth)
         ↓
Phase 2.5: Design System & Mockups ⭐ YOU ARE HERE
         ↓ (Design Gate - All mockups approved?)
Phase 3+: User Story Implementation (with per-page gates)
```

---

## Phase 2.5: Design System & Mockups

### **Stage 1: Design System Foundation** (2-3 days)

**Deliverables:**
1. Color palette definition
2. Typography scale
3. Component library decisions
4. Layout grid system
5. Dark/light theme specifications

**Tools:**
- Figma or similar
- Design tokens document
- Component showcase

**Approval Gate:** Design system must be approved before page mockups

---

### **Stage 2: Global Layout Mockups** (1-2 days)

**Deliverables:**
1. Main app shell (sidebar, header, footer)
2. Navigation structure
3. Responsive breakpoints
4. Empty states
5. Loading states
6. Error states

**Approval Gate:** Layout must be approved before page-specific mockups

---

### **Stage 3: Page-Specific Mockups** (1 week)

**One mockup per User Story page, following this cycle:**

#### **For Each Page:**

1. **Generate** (Designer/Tool) - 2 hours
   - Create high-fidelity mockup in Figma
   - Include all states: default, loading, error, empty
   - Mobile + desktop views
   - Interactive prototype (optional)

2. **Review** (Team) - 1 day
   - Stakeholder review
   - UX review
   - Accessibility check
   - Brand alignment check

3. **Modify** (Designer) - 4 hours
   - Address feedback
   - Create revision
   - Document changes

4. **Validate** (Team) - 4 hours
   - Final approval
   - Design handoff preparation
   - Implementation notes

5. **Approve** (Product Owner) - 1 hour
   - Sign-off
   - **Implementation can begin** ✅

#### **Page Mockup Priority:**

1. **User Story 1 (MVP):**
   - M001: Login/Registration pages
   - M002: Holdings upload page
   - M003: Dashboard page
   - M004: Recommendations page

2. **User Story 2:**
   - M005: Notifications (bell + dropdown)
   - M006: WebSocket progress indicators
   - M007: Report download interface

3. **User Story 3:**
   - M008: Settings/Constraints page
   - M009: Preview panel

4. **User Story 4:**
   - M010: Factor analysis page
   - M011: Performance metrics

5. **User Story 5:**
   - M012: Backtesting page

6. **User Story 6:**
   - M013: Exclusions interface

---

## Implementation Gating

### **Rule:**
Each User Story implementation phase has a **Design Approval Gate** that must pass before coding begins.

### **Gate Checklist:**

```
[ ] All page mockups for this User Story are complete
[ ] Mockups reviewed by team
[ ] Feedback addressed
[ ] Accessibility validated (WCAG 2.1 AA)
[ ] Responsive designs approved (mobile, tablet, desktop)
[ ] Component specifications documented
[ ] Design handoff complete
[ ] Product Owner sign-off obtained

✅ APPROVED → Implementation can begin
❌ NOT APPROVED → Return to mockup revision
```

---

## Tools & Assets

### **Design Tools:**

**Figma** (Recommended)
- File: `StockMonitor-Designs.fig`
- Structure:
  - Page 1: Design System
  - Page 2: Global Layout
  - Page 3: User Story 1 Mockups
  - Page 4: User Story 2 Mockups
  - etc.

**Deliverables per Mockup:**
1. High-fidelity screen designs (2x resolution)
2. Component specs (spacing, colors, typography)
3. Interaction notes (hover states, animations)
4. Developer handoff (measurements, assets)

---

## Workflow Process

### **Daily Standup During Design Phase:**

```
Designer: "Yesterday: Completed Dashboard mockup (M003)
          Today: Starting Recommendations page (M004)
          Blockers: Need clarification on confidence score display"

Product Owner: "I'll review M003 by EOD and provide feedback"

Developer: "Standing by for M003 approval to start T081"
```

### **Design Review Meeting (2x per week):**

**Agenda:**
1. Present new mockups (15 min)
2. Team feedback (15 min)
3. Revision discussion (10 min)
4. Approval votes (10 min)

**Attendees:**
- Product Owner (decision maker)
- Designer
- Lead Developer (feasibility check)
- UX/Accessibility reviewer

**Outcome:**
- Approved mockups move to "Ready for Implementation"
- Rejected mockups return to revision queue

---

## Mockup States

### **Kanban Board:**

```
┌─────────────┬──────────┬──────────┬──────────┬───────────────┐
│   Backlog   │  Design  │  Review  │ Revision │   Approved    │
├─────────────┼──────────┼──────────┼──────────┼───────────────┤
│ M005-M013   │  M004    │  M003    │  M002    │  M001 ✅      │
│ (planned)   │ (WIP)    │ (review) │ (fixing) │ (ready for    │
│             │          │          │          │ implementation)│
└─────────────┴──────────┴──────────┴──────────┴───────────────┘
```

### **State Definitions:**

- **Backlog**: Mockup not started
- **Design**: Designer actively creating mockup
- **Review**: Team reviewing, providing feedback
- **Revision**: Designer addressing feedback
- **Approved**: ✅ Implementation can begin

---

## Mockup Specifications Document

### **Template for Each Mockup:**

```markdown
# Mockup M001: Login Page

## Overview
- User Story: US1 - Initial Portfolio Setup
- Related Tasks: T073, T074
- Priority: P1 (MVP)
- Status: ✅ Approved

## Screens
1. Login form (default state)
2. Login form (error state)
3. Registration form
4. Email verification pending screen

## Components Used
- Input field (from design system)
- Button (primary variant)
- Card container
- Error message component
- Link component

## Interactions
1. Email input validation on blur
2. Password show/hide toggle
3. Form submission loading state
4. Error shake animation

## Responsive Behavior
- Mobile (<768px): Full-width card, stacked fields
- Tablet (768-1024px): Centered card, max-width 500px
- Desktop (>1024px): Centered card, max-width 500px

## Accessibility Notes
- Form labels properly associated
- Error messages announced to screen readers
- Keyboard navigation order: email → password → submit
- Focus visible on all interactive elements

## Design Handoff
- Figma link: [figma.com/file/...]
- Exported assets: `/design/assets/login/`
- Measurements: See Figma inspect mode
- Colors: Primary blue (#2563eb), Error red (#ef4444)

## Implementation Notes
- Use React Hook Form for validation
- Email regex: /^[^\s@]+@[^\s@]+\.[^\s@]+$/
- Password min length: 12 characters
- Implement rate limiting (5 attempts / 15 min)

## Approval
- Designer: ✅ Jane Doe (2024-10-30)
- Product Owner: ✅ John Smith (2024-10-30)
- Lead Dev: ✅ Sarah Johnson (2024-10-31)
- Ready for Implementation: ✅ Yes
```

---

## Design → Development Handoff

### **Designer Responsibilities:**

1. **Export Assets**
   - Icons (SVG format)
   - Images (WebP + PNG fallback, 2x resolution)
   - Logos (all variants and sizes)

2. **Document Specifications**
   - Spacing (margins, padding in px or rem)
   - Typography (font-size, line-height, font-weight)
   - Colors (hex codes with semantic names)
   - Shadows, borders, radius values

3. **Create Component Map**
   ```
   LoginPage uses:
   - InputField (email variant)
   - InputField (password variant)
   - Button (primary, large)
   - ErrorMessage
   - Link (secondary)
   ```

4. **Note Interactions**
   - Hover states
   - Focus states
   - Active states
   - Loading states
   - Error states
   - Success states

### **Developer Responsibilities:**

1. **Review Mockup Before Implementation**
   - Feasibility check
   - API integration points identified
   - State management needs
   - Performance considerations

2. **Ask Questions Early**
   - "What happens if the username is 100 characters?"
   - "How should this behave on slow connections?"
   - "What's the exact animation duration?"

3. **Implementation Fidelity**
   - Match mockup pixel-perfect (within 2-3px tolerance)
   - Use exact colors from design tokens
   - Implement all states shown in mockup
   - Follow responsive behavior specifications

4. **Design QA**
   - Compare implementation to mockup
   - Check all interactive states
   - Validate responsive behavior
   - Test accessibility requirements

---

## Design Validation Checklist

### **Before Marking Mockup as Approved:**

**Visual Design:**
- [ ] Follows design system (colors, typography, spacing)
- [ ] Consistent with other approved mockups
- [ ] All states designed (default, hover, focus, active, disabled, error, loading)
- [ ] Responsive designs for mobile, tablet, desktop
- [ ] Dark mode variant (if applicable)

**Content:**
- [ ] Placeholder text is realistic (not Lorem Ipsum)
- [ ] All labels are clear and actionable
- [ ] Error messages are helpful and specific
- [ ] Success messages are encouraging
- [ ] Microcopy follows brand voice

**Functionality:**
- [ ] User flow is intuitive
- [ ] Primary action is obvious
- [ ] Secondary actions are de-emphasized
- [ ] Navigation is clear
- [ ] Error recovery is straightforward

**Accessibility:**
- [ ] Color contrast meets WCAG 2.1 AA (4.5:1 for text)
- [ ] Focus indicators are visible
- [ ] Touch targets are 44x44px minimum
- [ ] Labels are associated with inputs
- [ ] Icon-only buttons have text alternatives

**Performance:**
- [ ] Images are optimized (WebP with fallbacks)
- [ ] No unnecessary animations
- [ ] Layout shift is minimized
- [ ] Progressive disclosure for complex UIs

**Feasibility:**
- [ ] Developer has confirmed it's implementable
- [ ] No unresolved technical questions
- [ ] API requirements are documented
- [ ] Data requirements are clear

**Sign-off:**
- [ ] Designer approves
- [ ] Product Owner approves
- [ ] Lead Developer approves
- [ ] Accessibility reviewer approves (if applicable)

---

## Timeline Estimates

### **Phase 2.5 Complete Duration: 2-3 weeks**

| Activity | Duration | Dependencies |
|----------|----------|--------------|
| Design System Setup | 2-3 days | Phase 2 complete |
| Global Layout Mockups | 1-2 days | Design system approved |
| US1 Page Mockups (4 pages) | 3-4 days | Layout approved |
| US1 Review & Revision | 2-3 days | Mockups complete |
| US1 Approval Gate | 1 day | Revisions complete |
| US2-6 Mockups (staggered) | 1-2 weeks | US1 in progress |

**Parallel Work:**
- Backend development (Phase 2) continues
- Database setup continues
- Mockups for US2-6 can be done while US1 is in implementation

---

## Risk Mitigation

### **Risk: Design delays implementation**

**Mitigation:**
- Start design immediately after Phase 2 begins
- Prioritize US1 mockups (MVP)
- Allow implementation to start as soon as US1 mockups approved
- US2-6 mockups can be done while US1 is being coded

### **Risk: Mockups don't match technical constraints**

**Mitigation:**
- Include lead developer in design reviews
- Validate API capabilities before finalizing mockups
- Have implementation spike for complex interactions
- Designer and developer pair on complex components

### **Risk: Endless revision cycles**

**Mitigation:**
- Set maximum 2 revision rounds
- Use design review meetings for batch feedback
- Product Owner has final decision-making authority
- Time-box each mockup phase

### **Risk: Mockup-implementation drift**

**Mitigation:**
- Design QA after implementation
- Side-by-side comparison screenshots
- Designer reviews implementation before PR merge
- Figma link in PR description for easy reference

---

## Success Criteria

**Phase 2.5 is complete when:**

✅ Design system documented and approved
✅ All US1 page mockups approved (M001-M004)
✅ Design handoff documents created
✅ Component specifications written
✅ Assets exported and organized
✅ Developer questions answered
✅ Product Owner sign-off obtained

**Then → Phase 3 (US1 Implementation) can begin**

---

## File Organization

```
specs/001-month-end-analyst/
├── design/
│   ├── design-system.md           # Color palette, typography, spacing
│   ├── mockups/
│   │   ├── M001-login.md          # Mockup spec + approval
│   │   ├── M002-holdings.md
│   │   ├── M003-dashboard.md
│   │   ├── M004-recommendations.md
│   │   ├── M005-notifications.md
│   │   └── ...
│   ├── assets/
│   │   ├── icons/                 # Exported SVG icons
│   │   ├── images/                # Exported images
│   │   └── screenshots/           # Mockup screenshots
│   ├── figma-links.md             # Links to Figma files
│   └── handoff/
│       ├── component-specs.md     # Detailed component specs
│       └── interaction-notes.md   # Animation/interaction details
├── plan.md
├── spec.md
├── tasks.md
├── design-workflow.md             # This file
└── data-model.md
```

---

## Next Steps

1. **Create Design System** (Start today)
   - Define color palette
   - Set typography scale
   - Document spacing system
   - Choose component library (shadcn/ui recommended)

2. **Set Up Figma File** (Day 2)
   - Create project structure
   - Import design system
   - Set up auto-layout and components

3. **Generate First Mockup** (Day 3)
   - M001: Login/Registration
   - Include all states
   - Mobile + desktop views

4. **Review & Approve** (Day 4-5)
   - Team review
   - Address feedback
   - Get approval

5. **Begin Implementation** (Day 6)
   - Start T073 (Create registration page)
   - Reference approved M001 mockup

**The design phase runs in parallel with backend development**, so you don't lose time!
