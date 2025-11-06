# Design-First Implementation Guide

**Quick Reference**: How to integrate mockups into your development workflow

---

## TL;DR - The Process

```
1. Create mockup in Figma
2. Review with team (2-3 days)
3. Get Product Owner approval
4. Mark design task as complete
5. Implementation tasks are now unblocked
6. Code the approved mockup
7. Designer reviews implementation
8. Done!
```

---

## File Structure Overview

```
specs/001-month-end-analyst/
│
├── tasks.md                    # Implementation tasks (T001-T248)
├── design-tasks.md             # Design tasks (D001-D107) ⭐ NEW
├── design-workflow.md          # Process documentation ⭐ NEW
├── DESIGN-FIRST-GUIDE.md       # This file ⭐ NEW
│
└── design/                     # All design deliverables ⭐ NEW
    ├── design-system.md        # Colors, typography, spacing
    ├── mockups/
    │   ├── M001-login.md       # Mockup specs
    │   ├── M002-holdings.md
    │   ├── M003-dashboard.md
    │   └── ...
    ├── assets/
    │   ├── icons/              # Exported SVG icons
    │   ├── images/             # Exported images
    │   └── screenshots/        # Mockup screenshots
    ├── figma-links.md          # Links to Figma files
    └── handoff/
        ├── component-specs.md  # Component specifications
        └── interaction-notes.md # Animation details
```

---

## How Tasks Are Linked

### **Before (Old Way)**

```
Phase 2: Foundational Complete
         ↓
Phase 3: User Story 1 Implementation
  - [ ] T073 Create registration page
  - [ ] T074 Create login page
  - [ ] T081 Create dashboard page
  → START CODING IMMEDIATELY (no design guidance)
```

### **After (Design-First)**

```
Phase 2: Foundational Complete
         ↓
Phase 2.5: Design Mockups
  - [ ] D015 Create login page mockup
  - [ ] D019 Document M001 specifications
  - [ ] D022 M001 approval ✅ ← GATE
         ↓ (Gate passed)
Phase 3: User Story 1 Implementation
  - [ ] T073 Create registration page (uses M001 mockup) ✅ UNBLOCKED
  - [ ] T074 Create login page (uses M001 mockup) ✅ UNBLOCKED
```

---

## Daily Workflow

### **Week 1-2: Design Phase (While Backend Devs Work)**

**Designer Schedule:**

```
Day 1-3: Design System
- [ ] D001-D007: Create design-system.md
- [ ] D006: Set up Figma project
- Approval Gate D1

Day 4-5: Global Layout
- [ ] D008-D014: Create app shell mockups
- Approval Gate D2

Day 6-10: US1 Mockups
- [ ] D015-D022: Login mockups (M001) → Approval
- [ ] D023-D031: Holdings mockups (M002) → Approval
- [ ] D032-D041: Dashboard mockups (M003) → Approval
- [ ] D042-D053: Recommendations mockups (M004) → Approval
```

**Backend Developer Schedule (Parallel):**

```
Day 1-10: Continue Phase 2
- [ ] T009-T035: Database, entities, security, etc.
- No waiting for design!
```

### **Week 3+: Implementation Phase (With Approved Mockups)**

**Frontend Developer Schedule:**

```
Day 11: Start US1 Frontend
- Check: M001 approved? ✅ Yes → Start T073
- Open: specs/001-month-end-analyst/design/mockups/M001-login.md
- Open: Figma link from specs/001-month-end-analyst/design/figma-links.md
- Code registration page matching mockup pixel-perfect
- Mark T073 complete

Day 12: Continue US1 Frontend
- Check: M002 approved? ✅ Yes → Start T077
- Open: M002-holdings.md
- Code holdings upload page
- Mark T077 complete
```

---

## Step-by-Step: Creating Your First Mockup

### **Example: M001 (Login Page)**

#### **Step 1: Generate the Mockup (2-4 hours)**

**Using Figma:**
1. Open your Figma file
2. Create new frame: "M001-Login"
3. Design the login form:
   - Email input
   - Password input (with show/hide toggle)
   - Submit button
   - "Forgot password?" link
   - "Don't have an account? Register" link
4. Create variants:
   - Default state
   - Loading state (button disabled, spinner)
   - Error state (red border, error message)
   - Success state (green checkmark)
5. Create mobile responsive variant (320px width)

**Using v0.dev (Alternative):**
1. Paste Prompt 1 from my earlier message
2. Click "Generate"
3. Review generated component
4. Click "Copy Code" (you'll use this later)
5. Take screenshot and save to `design/assets/screenshots/M001-login.png`

---

#### **Step 2: Document the Mockup (1 hour)**

Create `specs/001-month-end-analyst/design/mockups/M001-login.md`:

```markdown
# Mockup M001: Login Page

## Status
- Current State: 🟡 In Review
- Designer: Jane Doe
- Related Tasks: T073, T074
- Priority: P1 (MVP)

## Figma Link
https://figma.com/file/abc123/StockMonitor?node-id=100

## Screens
1. Login form (default)
2. Login form (loading)
3. Login form (error)
4. Mobile responsive view

## Components
- InputField (email variant)
- InputField (password variant with toggle)
- Button (primary, large)
- Link (secondary)
- ErrorMessage

## Specifications

### Colors
- Background: #0f172a (neutral-900)
- Card: #1e293b (neutral-800)
- Primary button: #2563eb (blue-600)
- Error text: #ef4444 (red-500)

### Typography
- Heading: Inter, 1.5rem, 600 weight
- Body: Inter, 1rem, 400 weight
- Input text: Inter, 1rem, 400 weight

### Spacing
- Card padding: 2rem
- Input margin-bottom: 1rem
- Button margin-top: 1.5rem

### Interactions
- Email validation on blur
- Password show/hide toggle on icon click
- Submit button disabled while loading
- Error shake animation (200ms, 5px left/right)

## Approval Checklist
- [ ] Designer review complete
- [ ] Accessibility validated (WCAG 2.1 AA)
- [ ] Mobile responsive approved
- [ ] Product Owner sign-off
- [ ] Lead Developer feasibility check

## Sign-off
- Designer: _____________ Date: _______
- Product Owner: _____________ Date: _______
- Lead Dev: _____________ Date: _______
```

---

#### **Step 3: Team Review (1-2 days)**

**Share the mockup:**
1. Post in Slack/Teams: "@team New mockup ready for review: M001 Login Page. Link: [Figma]. Feedback by EOD tomorrow."
2. Schedule design review meeting (30 min)

**Collect feedback:**
```
Product Owner: "Can we add social login buttons?"
→ Feedback noted: Add Google/GitHub OAuth buttons

UX Reviewer: "Error message too small, increase font size"
→ Feedback noted: Increase error text to 0.875rem

Developer: "What's the API endpoint for login?"
→ Note: POST /api/auth/login (already in contracts)

Accessibility: "Focus state not visible enough"
→ Feedback noted: Increase focus ring thickness to 2px
```

---

#### **Step 4: Revise (2-4 hours)**

1. Update Figma mockup based on feedback
2. Add OAuth buttons
3. Increase error text size
4. Make focus ring thicker
5. Take new screenshot
6. Update M001-login.md with "Revision 1" notes

---

#### **Step 5: Final Approval (1 day)**

**Update M001-login.md:**
```markdown
## Status
- Current State: ✅ Approved
- Revision: 2
- Approved Date: 2024-10-31

## Sign-off
- Designer: Jane Doe - 2024-10-31 ✅
- Product Owner: John Smith - 2024-10-31 ✅
- Lead Dev: Sarah Johnson - 2024-10-31 ✅
```

**Mark design task complete:**
```
- [x] D015 Create login page mockup ✅
- [x] D019 Document M001 specifications ✅
- [x] D022 M001 approval ✅
```

**Announce to team:**
```
🎉 M001 (Login Page) is approved!
Frontend devs can now start T073 and T074.
Mockup: specs/001-month-end-analyst/design/mockups/M001-login.md
Figma: [link]
```

---

#### **Step 6: Implementation (Frontend Developer)**

**When starting T073:**

1. Open `specs/001-month-end-analyst/design/mockups/M001-login.md`
2. Open Figma link
3. Read specifications
4. Create `frontend/src/pages/Register.tsx`
5. Code the component matching the mockup exactly
6. Use design tokens from `design/design-system.md`
7. Test all states (default, loading, error, success)
8. Test mobile responsive behavior
9. Run accessibility checks

**Implementation example:**
```tsx
// frontend/src/pages/Register.tsx
// Based on M001 mockup (specs/001-month-end-analyst/design/mockups/M001-login.md)

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useState } from 'react';

export function Register() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Implementation based on mockup specs...

  return (
    <div className="min-h-screen bg-neutral-900 flex items-center justify-center">
      <div className="bg-neutral-800 p-8 rounded-lg max-w-md w-full">
        <h1 className="text-2xl font-semibold mb-6">Create Account</h1>
        {/* Rest of implementation matching mockup... */}
      </div>
    </div>
  );
}
```

---

#### **Step 7: Design QA (Designer)**

**After implementation:**

1. Developer posts: "T073 complete, ready for design QA"
2. Designer reviews implementation vs mockup
3. Check:
   - ✅ Colors match design tokens
   - ✅ Spacing matches spec (2rem card padding)
   - ✅ Typography matches (Inter, 1.5rem heading)
   - ✅ All states work (loading, error, success)
   - ✅ Mobile responsive works
   - ⚠️ Error animation is 300ms, should be 200ms

**Designer feedback:**
```
Design QA for T073:
- Error animation too slow (300ms → 200ms)
- Otherwise looks great! ✅
```

**Developer fixes:**
```tsx
// Update animation duration
className="animate-shake duration-200"
```

**Designer approves:**
```
✅ Design QA passed for T073
```

---

## Approval Gates in Practice

### **What is an Approval Gate?**

A checkpoint that blocks tasks from starting until designs are approved.

### **Example:**

```
Task T073: Create registration page

Prerequisites:
✅ Phase 2 complete (backend foundation)
✅ M001 mockup approved ← GATE
❌ Cannot start until M001 is approved

Status: 🔒 BLOCKED (waiting for M001 approval)

Once M001 approved:
Status: ✅ READY TO START
```

### **How to Check Gate Status:**

**Before starting a task, check:**

1. Open `design-tasks.md`
2. Find the related mockup (e.g., M001 for T073)
3. Check approval status:
   ```
   - [x] D022 M001 approval ✅
   ```
4. If checked ✅ → Gate passed, start implementation
5. If unchecked ⬜ → Gate blocked, wait for approval

---

## Kanban Board Setup

### **Design Board:**

```
┌─────────────┬──────────┬──────────┬──────────┬───────────┐
│   Backlog   │  Design  │  Review  │ Revision │ Approved  │
├─────────────┼──────────┼──────────┼──────────┼───────────┤
│ M005-M013   │  M003    │  M002    │   M004   │  M001 ✅  │
│             │  (D032)  │  (D029)  │  (D051)  │  (ready)  │
└─────────────┴──────────┴──────────┴──────────┴───────────┘
```

### **Implementation Board:**

```
┌─────────────┬──────────────┬──────────┬──────────┬──────────┐
│   Blocked   │ Ready        │ In Prog  │ Design QA│   Done   │
├─────────────┼──────────────┼──────────┼──────────┼──────────┤
│ T077 (wait  │ T073 (M001✅)│  T074    │   T081   │  T036    │
│  for M002)  │ T074 (M001✅)│          │          │  T037    │
│ T081 (wait  │              │          │          │          │
│  for M003)  │              │          │          │          │
└─────────────┴──────────────┴──────────┴──────────┴──────────┘
```

---

## Common Questions

### **Q: Do we have to wait for ALL mockups before starting ANY coding?**

**A: No!** You can start coding as soon as the mockup for that specific page is approved.

Example:
- M001 (Login) approved on Day 8 → Start T073 immediately
- M002 (Holdings) approved on Day 10 → Start T077 on Day 10
- M003 (Dashboard) still in review → T081 stays blocked

### **Q: Can backend development continue during the design phase?**

**A: Yes!** Backend work (Phase 2) runs in parallel with design (Phase 2.5).

```
Week 1-2:
Backend Team → T009-T035 (database, entities, security)
Design Team → D001-D053 (design system, mockups)
Frontend Team → T026-T035 (React setup, API client)

Week 3+:
Backend Team → Continue building APIs
Design Team → D054+ (US2-6 mockups)
Frontend Team → T073+ (implement approved mockups)
```

### **Q: What if we need to change a mockup after implementation starts?**

**A: Use a change request process:**

1. Create design change ticket: "M001-CR1: Add OAuth buttons"
2. Assess impact:
   - Already implemented? → Estimate rework effort
   - Not implemented yet? → Update mockup before coding
3. Get approval for change
4. Update mockup
5. Update implementation (if already coded)

### **Q: How do we handle urgent fixes that bypass design?**

**A: Create a fast-track process for critical bugs:**

1. Bug found in production
2. Product Owner approves skip-design exception
3. Developer implements fix
4. Designer creates retroactive mockup for documentation
5. Document exception in `design/exceptions.md`

---

## Success Metrics

**Track these to ensure the process is working:**

1. **Design-Implementation Fidelity**: 95%+ match
   - Take screenshots of implementation
   - Compare to mockup side-by-side
   - Measure pixel differences

2. **Rework Rate**: <10%
   - Track how often you redo UI work
   - Low rework = good mockups

3. **Approval Cycle Time**: <3 days per mockup
   - From "Design" to "Approved"
   - If >3 days, streamline review process

4. **Gate Wait Time**: <2 days
   - Time developers wait for mockup approval
   - If >2 days, prioritize design work

---

## Next Steps

### **To Start Using This Process:**

1. **Read**: `design-workflow.md` (full process documentation)

2. **Create**: Design System (D001-D007)
   - Start with `specs/001-month-end-analyst/design/design-system.md`
   - Define colors, typography, spacing

3. **Set Up**: Figma project (D006)
   - Create new Figma file
   - Import design system
   - Create page structure

4. **Generate**: First mockup (M001)
   - Use Figma or v0.dev
   - Follow Step 1-5 above
   - Get approval

5. **Implement**: First page (T073)
   - Wait for M001 approval
   - Code matching mockup
   - Get design QA approval

6. **Repeat**: For all other pages
   - Follow the same process
   - Build momentum

---

## Files Reference

| File | Purpose |
|------|---------|
| `tasks.md` | Implementation tasks (T001-T248) |
| `design-tasks.md` | Design tasks (D001-D107) |
| `design-workflow.md` | Full process documentation |
| `DESIGN-FIRST-GUIDE.md` | Quick reference (this file) |
| `design/design-system.md` | Colors, typography, spacing |
| `design/mockups/M***.md` | Mockup specifications |
| `design/figma-links.md` | Links to Figma files |
| `design/handoff/component-specs.md` | Component details |

---

## Summary

**The Process:**
```
1. Design mockup (2-4 hours)
2. Document spec (1 hour)
3. Team review (1-2 days)
4. Revise based on feedback (2-4 hours)
5. Get approval (1 day)
6. Implement (varies)
7. Design QA (1 hour)
8. Done! ✅
```

**Key Rules:**
- ✅ No implementation without approved mockup
- ✅ Backend and design can run in parallel
- ✅ Start coding as soon as that specific mockup is approved
- ✅ Designer reviews implementation before merge

**Benefits:**
- 🎨 Consistent, polished UI
- ⚡ Less rework (get design right first)
- 🚀 Faster implementation (clear guidance)
- ✅ Stakeholder buy-in early (see mockups before code)

---

**You're ready to start!** Begin with D001 (Define color palette) tomorrow. 🚀
