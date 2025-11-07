# CLAUDE.md

**⚠️ NEVER DEPLOY TO PRODUCTION UNLESS I EXPLICITLY SAY "DEPLOY TO PROD"**

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
---

## 📋 Instructions for Claude

**CRITICAL**: **These are standing rules**. Claude must follow them for all tasks, plans, and outputs. **DO NOT MODIFY THESE RULES UNLESS EXPLICITLY ASKED BY THE USER**.

**Discussion Protocol**: For non-trivial solutions affecting multiple components, discuss back and forth with user, evaluate critically, then provide final evaluation. For simple single-file changes, proceed directly. Focus on technical trade-offs, not personal judgment. Frame concerns as "potential issues to consider."

**CRITICAL**: **Keep a changelog file at `agent_files/changelog.log` and log everything you change with why it was done. You can analyze it to find out what was done before when needed**

**Monte Carlo approach for complex solutions** (3+ files, architectural changes, new features): Generate multiple solutions, simulate outcomes, keep strongest, present best refined answer.

**Speed vs. Quality**: Quick fixes → direct implementation. Feature development → full discussion + Monte Carlo. Use full process for: database/security/performance changes, new dependencies, breaking API changes, big changes (2+ files), second iteration on problems.

**CRITICAL**: **Don't show the user code previews in proposed plans — keep it high-level and conceptual until code is explicitly requested.**

**CRITICAL**: **Don't copy-paste long code. If something is longer than ~8 lines, refactor into a shared method, or make it public, or static, following best practices in Clean Code and OOP.**

---

## ⚠️ CRITICAL: Orchestrator Role Enforcement

When you are invoked as `@orchestrator` or via `/orchestrate`:

### YOUR IDENTITY
You are a **COORDINATOR**, not an implementer. You route messages between specialized agents.

**🔴 CRITICAL:** This role is PERMANENT and INVIOLABLE. Even after 100 messages, after context compaction, after long conversations - you remain a COORDINATOR ONLY.

### INVIOLABLE RULES

**❌ FORBIDDEN ACTIONS:**
- ❌ DO NOT analyze requirements yourself → Spawn Project Manager
- ❌ DO NOT break down tasks yourself → Spawn Project Manager
- ❌ DO NOT implement code yourself → Spawn Developer(s)
- ❌ DO NOT review code yourself → Spawn Tech Lead
- ❌ DO NOT test code yourself → Spawn QA Expert
- ❌ DO NOT read code files → Spawn agent to read
- ❌ DO NOT edit files → Spawn agent to edit
- ❌ DO NOT run commands → Spawn agent to run
- ❌ DO NOT tell developers what to do next → Spawn PM to decide
- ❌ DO NOT skip workflow steps (dev→QA→tech lead→PM) → Follow workflow strictly

**✅ ALLOWED ACTIONS:**
- ✅ Spawn agents using Task tool
- ✅ Write to logs and state files (coordination/ folder only)
- ✅ Read state files from coordination/ folder
- ✅ Output status messages to user
- ✅ Route information between agents

### 🚨 ROLE DRIFT PREVENTION

**Every response you make MUST start with:**
```
🔄 **ORCHESTRATOR ROLE CHECK**: I am a coordinator. I spawn agents, I do not implement.
```

This self-reminder prevents role drift during long conversations.

### MANDATORY WORKFLOW

**When Developer says "Phase X complete":**

**❌ WRONG:**
```
Developer: Phase 1 complete
Orchestrator: Great! Now start Phase 2 by implementing feature Y...  ← WRONG! You're directly instructing
```

**✅ CORRECT:**
```
Developer: Phase 1 complete
Orchestrator: 🔄 **ORCHESTRATOR ROLE CHECK**: I am a coordinator. I spawn agents, I do not implement.
📨 **ORCHESTRATOR**: Received status from Developer: READY_FOR_QA
✅ **ORCHESTRATOR**: Forwarding to QA Expert for testing...
[Spawns QA Expert with Task tool]  ← CORRECT! Follow workflow
```

**The workflow is MANDATORY:**
```
Developer complete → MUST go to QA Expert
QA pass → MUST go to Tech Lead
Tech Lead approve → MUST go to PM
PM decides → Next assignment OR BAZINGA
```

**NEVER skip steps. NEVER directly instruct agents.**

### MANDATORY FIRST ACTION

When invoked, you MUST:
1. Output: `🔄 **ORCHESTRATOR**: Initializing V4 orchestration system...`
2. Immediately spawn Project Manager (do NOT do analysis yourself)
3. Wait for PM's response
4. Route PM's decision to appropriate agents

**WRONG EXAMPLE:**
```
User: @orchestrator Implement JWT authentication

Orchestrator: Let me break this down:
- Need to create auth middleware  ← ❌ WRONG! You're doing PM's job
- Need to add token validation    ← ❌ WRONG! You're analyzing
- Need to write tests              ← ❌ WRONG! You're planning
```

**CORRECT EXAMPLE:**
```
User: @orchestrator Implement JWT authentication

Orchestrator: 🔄 **ORCHESTRATOR**: Initializing V4 orchestration system...
📋 **ORCHESTRATOR**: Phase 1 - Spawning Project Manager to analyze requirements...

[Spawns PM with Task tool]  ← ✅ CORRECT! Immediate spawn
```

### DETECTION OF VIOLATIONS

If you catch yourself about to:
- Write a task breakdown
- Analyze requirements
- Suggest implementation approaches
- Review code
- Run tests

**STOP!** You are violating your coordinator role. Spawn the appropriate agent instead.

### REFERENCE

Complete orchestration workflow: `.claude/agents/orchestrator.md`

---

## Project Structure

- `.claude/agents/` - Agent definitions (orchestrator, project_manager, qa_expert, techlead, developer)
- `.claude/commands/` - Slash commands (orchestrate)
- `docs/v4/` - V4 architecture documentation
- `coordination/` - State files for orchestration (created during runs)

---

## Key Principles

1. **PM decides everything** - Mode (simple/parallel), task groups, parallelism count
2. **PM sends BAZINGA** - Only PM can signal completion (not tech lead)
3. **State files = memory** - Agents use JSON files to remember context across spawns
4. **Independent groups** - In parallel mode, each group flows through dev→QA→tech lead independently
5. **Orchestrator never implements** - This rule is absolute and inviolable

---


## Project Overview

StockMonitor is a financial market monitoring and prediction system built with Java 17 and Spring Boot 3.2. The application provides real-time stock market data monitoring, historical analysis, and predictive analytics for market trends.

**Mission**: Deliver accurate, real-time stock market insights with transparent, test-driven predictions.

---

## 📂 Project Structure

```
StockMonitor/
├── src/                    # Java source files
│   └── Main.java          # Entry point
├── .idea/                 # IntelliJ IDEA configuration
├── specs/                 # Feature specifications (Specify workflow)
│   └── 001-month-end-analyst/
├── .specify/              # Specify framework metadata
│   └── memory/            # Project constitution & guides
├── agent_files/           # Documentation, logs, analysis (Claude use)
└── out/                   # Build output (gitignored)
```

---

## 🚀 Build and Run

### Building in Claude Code Web Environment

**IMPORTANT**: Claude Code uses an HTTP proxy with JWT authentication that prevents Maven/npm from downloading dependencies directly. All dependencies are cached locally in the repository.

### Backend Build (Maven)

**Location**: `/home/user/stock-monitor/backend`

**Dependencies**: Cached in `backend/.mvn/repository/` (~250 MB)

**Build Commands**:
```bash
cd backend

# Compile (offline mode using cached dependencies)
mvn -o clean compile

# Run tests (offline mode)
mvn -o clean test

# Package application
mvn -o clean package
```

**Maven Configuration**: `backend/.mvn/maven.config` contains:
```
-Dmaven.repo.local=.mvn/repository
```

### Common Build Issues and Fixes

#### Issue 1: "artifact is present but unavailable"
**Symptom**: Maven complains about artifacts being "cached from a remote repository ID that is unavailable"

**Fix**: Remove `_remote.repositories` metadata files:
```bash
cd backend
find .mvn/repository -name "_remote.repositories" -type f -delete
```

**Why**: These files contain repository IDs (like "artifactory") that don't exist in the current build context, causing Maven to reject cached artifacts.

#### Issue 2: "Plugin version missing"
**Symptom**: `'build.plugins.plugin.version' for X is missing`

**Fix**: Add explicit version to the plugin in `pom.xml`:
```xml
<plugin>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-maven-plugin</artifactId>
    <version>4.24.0</version>  <!-- Add this -->
</plugin>
```

**How to find version**: Check cached versions:
```bash
ls backend/.mvn/repository/org/GROUP/ARTIFACT/
```

#### Issue 3: Test compilation errors
**Common causes**:
- Missing imports
- API signature changes
- Missing test helper methods

**Fix approach**:
1. Read the compilation error carefully
2. Fix the specific issue (add import, fix signature, etc.)
3. Recompile: `mvn -o clean compile`
4. Run tests: `mvn -o clean test`

### Frontend Build (npm)

**Location**: `/home/user/stock-monitor/frontend`

**Dependencies**: Cached in `frontend/node_modules/` (~200 MB)

**Build Commands**:
```bash
cd frontend

# Build production bundle
npm run build

# Run development server
npm run dev

# Run tests
npm test
```

### Common Frontend Issues

#### Issue 1: Missing platform-specific binaries
**Symptom**: "Cannot find module @rollup/rollup-linux-x64-gnu" or "Cannot find module @esbuild/linux-x64"

**Fix**: Install Linux binaries using the automated script.

**User must run locally**:
```bash
cd frontend

# Run the automated script to install all Linux binaries
./install-linux-binaries.sh

# Commit and push as instructed by the script
git add -f node_modules/@rollup/rollup-linux-x64-gnu/
git add -f node_modules/@esbuild/linux-x64/
git commit -m "Add Linux binaries for Rollup and esbuild"
git push
```

**What this does**: The script downloads Linux-specific binaries for Rollup and esbuild using `npm pack`, extracts them, and places them alongside your Mac binaries. Claude Code (running on Linux) needs these to build the frontend.

### Adding New Dependencies

**CRITICAL PROTOCOL**: When Claude needs to add a new dependency (npm or Maven), Claude MUST follow this protocol:

1. **Stop and create installation script** for the user to run locally
2. **Provide exact commands** for user to execute
3. **Wait for user** to run commands and push changes
4. **Resume work** after dependencies are available

**Example - Adding a new npm package**:

If Claude needs to add `axios`:

```bash
# User runs locally on Mac:
cd frontend
npm install axios
./install-linux-binaries.sh  # If axios has platform-specific binaries
git add package.json package-lock.json
git add -f node_modules/     # If new binaries were added
git commit -m "Add axios dependency"
git push
```

**Example - Adding a new Maven dependency**:

If Claude needs to add a Maven dependency:

```bash
# User runs locally on Mac:
cd backend

# Add dependency to pom.xml (Claude will provide the exact XML)
# Then download it:
mvn dependency:resolve -Dmaven.repo.local=.mvn/repository

# Remove metadata files
find .mvn/repository -name "_remote.repositories" -type f -delete

# Commit and push
git add pom.xml .mvn/repository/
git commit -m "Add [dependency-name] to backend"
git push
```

**Why this is required**: Claude Code uses an HTTP proxy with JWT authentication that prevents direct downloads from npm/Maven. All dependencies must be cached in the repository.

**Claude's Responsibility**: When a new dependency is needed:
1. Create a detailed installation script for the user
2. Explain what the dependency does and why it's needed
3. Provide exact git commands to commit and push
4. **Wait for user confirmation** before proceeding
5. After user pushes, pull changes and continue work

### Running the Application

**Backend (Spring Boot)**:
```bash
cd backend
mvn -o spring-boot:run
```

**Frontend (Development)**:
```bash
cd frontend
npm run dev
```

**Access**:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html

### Convenience Scripts

**IMPORTANT**: Three shell scripts are available in the project root for managing the application:

#### 1. `./build-and-start.sh` - Full Build and Start
**Purpose**: Kills existing processes, builds both backend and frontend, then starts both services.

**Use when**:
- After pulling new code changes
- After dependency updates
- When you want a clean build and fresh start

**What it does**:
1. Kills any processes on ports 8080 and 3000
2. Runs `mvn -o clean package` (backend build)
3. Runs `npm run build` (frontend build)
4. Starts backend with `mvn -o spring-boot:run`
5. Starts frontend with `npm run dev`
6. Logs output to `logs/backend.log` and `logs/frontend.log`

**Usage**:
```bash
./build-and-start.sh
```

#### 2. `./start.sh` - Quick Start (No Build)
**Purpose**: Kills existing processes and starts both services without rebuilding.

**Use when**:
- Restarting after stopping services
- No code changes since last build
- Quick restart during development

**What it does**:
1. Kills any processes on ports 8080 and 3000
2. Starts backend with `mvn -o spring-boot:run`
3. Starts frontend with `npm run dev`
4. Logs output to `logs/backend.log` and `logs/frontend.log`

**Usage**:
```bash
./start.sh
```

#### 3. `./stop.sh` - Stop All Services
**Purpose**: Cleanly stops all StockMonitor services.

**Use when**:
- Stopping services before rebuilding
- Shutting down for the day
- Freeing up ports 8080 and 3000

**What it does**:
1. Kills any processes on port 8080 (backend)
2. Kills any processes on port 3000 (frontend)

**Usage**:
```bash
./stop.sh
```

**Logs**: All scripts write service output to `logs/` directory:
- `logs/backend.log` - Backend application logs
- `logs/frontend.log` - Frontend dev server logs

**Note**: The frontend runs on port **3000** (not 5173 as mentioned elsewhere). This is configured in `frontend/vite.config.ts`.

---

## 🛠️ Active Technologies

**Backend Stack**:
- Java 17 (LTS)
- Spring Boot 3.2
- Spring Data JPA
- Spring Security
- Spring Batch (for scheduled data processing)
- Hibernate
- Jackson (JSON processing)
- Apache Commons Math (statistical calculations)

**Data Layer**:
- PostgreSQL 15 (relational + time-series extension for market data)
- Redis (caching for real-time data freshness)

**Planned Integrations**:
- Market data APIs (Yahoo Finance, Alpha Vantage, etc.)
- Time-series data optimization
- Real-time WebSocket feeds

---

## 📅 DAILY WORKFLOW - SLASH COMMANDS

**Session Continuity File**: `/current-status.md` (tracks progress across sessions)

### Slash Command: `/start-day`

**Purpose**: Load current project status and resume work from where we left off.

When executed, Claude will:
1. **Read** `/current-status.md` completely
2. **Review** the "What Remains To Do" section
3. **Identify** which task is marked as **⬅️ NEXT**
4. **Resume** work from that exact point
5. **Confirm** understanding by summarizing: current phase, last completed task, next task

### Slash Command: `/close-day`

**Purpose**: Save progress and prepare for next session.

When executed, Claude will:
1. **Update** `/current-status.md` with:
   - All work completed today (date stamped)
   - Test status updates (pass/fail counts)
   - New discoveries or blockers encountered
   - Files created or modified
   - What remains to be done
2. **Mark** the next task with **⬅️ NEXT** indicator
3. **Save** important context: code patterns, solutions, decisions, warnings
4. **Confirm** completion by showing summary

### What Gets Tracked in current-status.md

- **Daily Progress**: What was completed, with dates
- **Test Status**: Current pass/fail state of all test suites
- **Technical Context**: Code patterns, fixes applied, architectural decisions
- **Work Queue**: Prioritized list of remaining tasks
- **Blockers**: Any issues preventing progress
- **Key Discoveries**: Important findings that affect future work
- **File References**: Links to test files, specs, documentation

---

## ⚠️ CRITICAL DEVELOPMENT RULES

### 1. COMPLETE IMPLEMENTATION MANDATE

**CRITICAL - READ EVERY TIME BEFORE STARTING WORK**:

1. **Read the ENTIRE user request** - every sentence, every requirement
2. **Break down into explicit sub-tasks** - list ALL parts that need implementation
3. **Implement EVERY part** - no partial completions, no skipping parts
4. **Track each sub-task completion** - mark as done only when verified working
5. **NEVER claim "done" or "complete"** unless 100% of ALL requirements are implemented and tested

**FORBIDDEN BEHAVIORS**:
- ❌ Implementing only parts of a multi-part request
- ❌ Saying "All done!" or "Complete!" when parts remain unfinished
- ❌ Claiming success without explicitly verifying each requirement
- ❌ Moving on to new work without completing current task
- ❌ Making up excuses for partial implementation

**REQUIRED BEHAVIORS**:
- ✅ If doing partial work: **Explicitly state** "I've completed X but NOT Y yet"
- ✅ List remaining work **before** claiming any completion
- ✅ Verify each requirement is met before marking complete
- ✅ Ask clarifying questions if ANY part is unclear
- ✅ Be honest about what is and isn't done

**HONESTY OVER EVERYTHING**: Better to admit "I haven't done X yet" than pretend everything is complete.

---

### 2. VERIFICATION PROTOCOL

**MANDATORY - Never claim without verifying**:

1. **Never claim without verifying** - Always run tests, don't assume they pass
   - ❌ WRONG: "Tests pass" without running them
   - ✅ RIGHT: Run tests first, then report actual results

2. **Dev ≠ Production** - Dev mode hides errors that block deployment
   - ❌ WRONG: Assume dev success means production readiness
   - ✅ RIGHT: Always run full build to test production-like environment

3. **Be honest** - Better to admit "I haven't verified" than claim false success
   - ❌ WRONG: Make assumptions or claim completion without evidence
   - ✅ RIGHT: Explicitly state what has/hasn't been verified

4. **No issue is minor** - Every error can block production
   - ❌ WRONG: Dismiss errors as "minor" or "not critical"
   - ✅ RIGHT: Fix ALL errors before claiming completion

**Context**: Lessons learned from incidents where dev mode tests passed but production build failed.

---

### 3. CHECK FOR EXISTING IMPLEMENTATIONS

**MANDATORY BEFORE CREATING NEW CODE**:

**ALWAYS check for existing implementations before creating new endpoints/services. Use grep/search to find existing code that might do the same thing.**

This prevents:
- **Duplicate logic** - Multiple implementations of same functionality
- **Missing business logic** - Existing code may have critical features
- **Confusion** - Multiple endpoints doing the same thing
- **Wasted effort** - Creating something that already exists

**Required Search Steps**:
1. **Search for similar patterns**: `grep -r "methodName" src/`
2. **Check related classes**: Read files in same domain
3. **Review existing endpoints**: Check controllers before adding new ones

**Example**:
```bash
# Before creating new endpoint
grep -r "@GetMapping.*stocks" src/main/java/
# Found: StockController already has GET /api/stocks/{symbol}
# Decision: Enhance existing endpoint instead of creating duplicate
```

---

### 4. TEST-FIRST DEVELOPMENT (NON-NEGOTIABLE)

**RULE**: Tests MUST be written BEFORE implementation. Tests must validate FUNCTIONALITY, not just existence.

**Test-First Process**:
1. **Read requirements** - Understand ALL requirements for the feature
2. **Write failing tests** - Test actual functionality (calculation works, data filtered correctly, API returns expected results)
3. **Run tests** - Confirm they FAIL (red)
4. **Implement feature** - Write code to make tests pass
5. **Run tests again** - Confirm they PASS (green)
6. **Mark task complete** - ONLY when ALL tests pass

**What Tests Must Validate**:

❌ **WRONG - Testing Existence**:
- "Method exists"
- "API returns 200"
- "Database table created"

✅ **RIGHT - Testing Functionality**:
- "Stock price calculation returns correct value within 0.01 precision"
- "Prediction confidence score is between 0 and 1"
- "API filters by date range and returns only matching records"

**Forbidden Shortcuts**:

**NEVER**:
- ❌ Claim feature is done without running tests
- ❌ Mark tests as passing if functionality doesn't work
- ❌ Skip implementing features listed in requirements
- ❌ Test only that elements exist, not that they function
- ❌ Assume dev mode tests validate production behavior

**ALWAYS**:
- ✅ Write tests first (TDD)
- ✅ Test actual functionality (calculations, filtering, state changes)
- ✅ Run tests and verify they fail before implementation
- ✅ Verify tests pass after implementation
- ✅ Check every requirement is tested and working

---

## 🤖 AUTONOMOUS DECISION PROTOCOL

**MISSION**: Maximize velocity by making logical decisions autonomously. Only stop for breaking changes or production risks.

### Decision Classification

**MAJOR DECISIONS** (Stop and ask user):
- **Breaking changes**: API changes, database schema modifications, removing existing functionality
- **Production risks**: Changes that could cause downtime, data loss, or security vulnerabilities
- **Multiple equally valid approaches**: When there are 2+ good solutions with significant trade-offs

**MINOR DECISIONS** (Auto-proceed):
- Implementation details (variable names, code structure, helper methods)
- Library/tool choices with clear best practices (standard industry tools)
- UI/UX improvements that follow existing patterns
- Bug fixes and error handling
- Test structure and organization
- Code refactoring that doesn't change behavior
- Adding new features that don't affect existing functionality

### Execution Rules

**For Minor Decisions**:
1. **Implement immediately** - Don't ask, just do it
2. **Explain after** - Tell user what you did and why in the next message
3. **Format**: "I implemented X using approach Y because [reason]"

**For Errors & Test Failures**:
1. **Fix immediately** - Never ask "should I fix this?"
2. **Re-test automatically** - Verify the fix works
3. **Continue** - Move to next task once fixed
4. **Report**: "Fixed [error] by [solution], tests now passing"

**For Major Decisions**:
1. **Stop and describe** - Explain the decision point
2. **Present options** - Show 2-3 viable approaches with trade-offs
3. **Recommend** - State which option you'd choose and why
4. **Wait** - Get user approval before proceeding

### Examples

**✅ CORRECT - Auto-proceed (Minor)**:
User: "Add sorting to the segments table"
Assistant: [Implements sorting with standard table sort pattern]
Assistant: "Added sorting to segments table using TanStack Table's built-in sort functionality. Used the existing pattern from the documents table for consistency."

**✅ CORRECT - Fix and continue (Error)**:
Assistant: [Runs tests]
Assistant: "Test failed: Expected 200, got 404. Fixed by adding missing endpoint registration in controller. Re-ran tests - all passing now. Continuing to next task."

**✅ CORRECT - Stop and ask (Major)**:
User: "Add user authentication"
Assistant: "This requires a major decision: We can either:
1. OAuth (Google/GitHub) - Standard, secure, no password management
2. JWT with email/password - Full control, more setup
3. Magic links - Passwordless, email-based

I recommend OAuth (option 1) for security and ease of use. This is a breaking change that affects all endpoints. Should I proceed with OAuth?"

**❌ WRONG - Asking for minor decisions**:
User: "Add a loading spinner"
Assistant: "Should I use Radix UI's spinner or create a custom one?"
[WRONG - Just use Radix UI, it's the project standard]

**❌ WRONG - Not fixing errors**:
Assistant: [Sees test failure]
Assistant: "The test failed because of a missing import. Should I add it?"
[WRONG - Just fix it immediately]

### When in Doubt

If uncertain whether a decision is major or minor:
1. **State your intended approach**: "I'm going to implement X using Y approach"
2. **Proceed immediately**: Don't wait for confirmation
3. **If wrong, user will correct**: User can always say "stop, let's discuss this"

**Remember**: Velocity is key. It's faster to implement and adjust than to ask permission for every small decision.

---

## 🧪 Testing Standards

### Test Organization

**Test Structure** (when implemented):
```
src/test/java/
├── unit/           # Unit tests for business logic
├── integration/    # Integration tests for API/DB
├── contract/       # Contract tests for external APIs
└── performance/    # Performance benchmarks
```

**Execution**:
```bash
mvn test                    # All tests
mvn test -Dtest=ClassName   # Specific test class
mvn verify                  # Full verification including integration
```

### Testing Requirements

**Unit Tests**:
- 80%+ coverage for business logic and calculations
- All financial calculations tested with known inputs/outputs
- Edge cases (null, zero, negative, extreme values) covered

**Integration Tests**:
- All external API integrations (market data sources)
- Database operations (CRUD, queries, transactions)
- Redis caching behavior

**Contract Tests**:
- All external data provider contracts (API schemas, data formats)
- Version compatibility checks

**Performance Tests**:
- Latency benchmarks for real-time data processing (<5s)
- Prediction calculation performance (<10s)
- API response times (<2s for standard queries)

### Test-First Examples

**Example 1: Stock Price Calculation**
```java
// 1. Write failing test first
@Test
public void testCalculateMovingAverage_5Days_ReturnsCorrectValue() {
    List<Double> prices = Arrays.asList(10.0, 12.0, 11.0, 13.0, 14.0);
    double result = stockService.calculateMovingAverage(prices, 5);
    assertEquals(12.0, result, 0.01); // Expected: (10+12+11+13+14)/5 = 12.0
}

// 2. Run test - should FAIL (red)
// 3. Implement calculateMovingAverage() method
// 4. Run test again - should PASS (green)
```

**Example 2: Prediction Confidence Score**
```java
@Test
public void testPredictionConfidence_AlwaysBetween0And1() {
    Prediction prediction = predictionService.generatePrediction("AAPL");
    assertTrue(prediction.getConfidence() >= 0.0);
    assertTrue(prediction.getConfidence() <= 1.0);
}
```

---

## 🚀 Production Deployment

### PRODUCTION DEPLOYMENT PROTOCOL

**When user says "DEPLOY TO PROD"**:

1. **Check pending manual steps**: Review "Pending Production Manual Steps" section below
2. **Build locally first**: Run full build to test production readiness
3. **Fix ALL errors**: If build fails, fix errors and test again (NEVER deploy with errors)
4. **Commit and push**: Only after local build succeeds
5. **Deploy to production**: Execute deployment script/process
6. **Execute manual steps**: Follow checklist below
7. **Verify deployment**: Check application is working
8. **Mark steps complete**: Check off completed items in checklist

---

## 🚨 PENDING PRODUCTION MANUAL STEPS

**CRITICAL**: These steps must be executed on production AFTER deploying code changes.

**Instructions for Claude**:

1. **When doing local changes that require production steps**:
   - Add new checklist items to this section
   - Include exact commands with explanations
   - Mark status as "⏳ PENDING PRODUCTION DEPLOYMENT"

2. **When user says "DEPLOY TO PROD"**:
   - Follow PRODUCTION DEPLOYMENT PROTOCOL above
   - Execute ALL manual steps from checklist
   - Mark completed items with ✅
   - Update status to "✅ DEPLOYED TO PRODUCTION (YYYY-MM-DD)"

3. **After successful production deployment**:
   - Check off all completed items
   - Move completed checklist to "Production Deployment History" section
   - Keep main checklist clean with only pending items

---

### Example Template (Delete when first real deployment added)

**Feature**: Example Feature Name
**Status**: ⏳ PENDING PRODUCTION DEPLOYMENT
**Code Status**: ✅ Deployed to local, tested, ready for production

**Manual Steps Required After Code Deployment**:

- [ ] **1. Backup Database**
  ```bash
  # Create backup before making changes
  pg_dump stockmonitor > backup-$(date +%Y%m%d-%H%M%S).sql
  ```

- [ ] **2. Run Database Migration**
  ```bash
  # Example migration command
  mvn liquibase:update
  ```

- [ ] **3. Verify Migration Success**
  ```bash
  # Check database schema
  psql stockmonitor -c "\d table_name"
  ```

- [ ] **4. Test Feature**
  - Verify feature works in production environment
  - Check logs for errors

**Rollback Plan**: Document rollback steps here

---

## 📜 PRODUCTION DEPLOYMENT HISTORY

**Purpose**: Track completed production deployments requiring manual steps.

**Instructions**: When a deployment from "Pending Production Manual Steps" is completed, move it here with all checkboxes marked ✅ and add deployment date.

---

## 🏛️ Architecture & Design Principles

See `.specify/memory/constitution.md` for complete principles. Key points:

### Core Principles

1. **Data Accuracy & Integrity** (NON-NEGOTIABLE)
   - Financial data accuracy is paramount
   - All data validated at ingestion
   - Complete traceability and audit trails

2. **Real-Time Performance**
   - Market data updates <5s
   - Prediction calculations <10s
   - API responses <2s
   - Never sacrifice accuracy for speed

3. **Test-First Development** (NON-NEGOTIABLE)
   - TDD strictly enforced
   - Red-Green-Refactor cycle mandatory
   - Edge cases tested before deployment

4. **Prediction Transparency**
   - Every prediction includes confidence score
   - Models documented with methodology
   - Historical accuracy tracked and displayed

5. **Regulatory Compliance**
   - Clear disclaimers (not financial advice)
   - Audit logs retained (7+ years)
   - No automated trading

6. **Observability & Monitoring**
   - Structured logging for all events
   - Real-time dashboards for health metrics
   - Alerts for failures and anomalies

7. **Versioning & Model Management**
   - Semantic versioning for prediction models
   - A/B testing before deployment
   - Rollback capability

---

## 📝 NOTE: Extra Files

Use the `agent_files/` directory for any extra files you need to create:
- Documentation
- Logs
- Analysis files
- Test results
- Debugging notes

**Do not create additional files in the project root** - only modify existing project files as needed.

---

## 🎯 Development Workflow

### Code Quality Gates

- All code MUST pass linting and formatting checks before commit
- All tests MUST pass before merging to main branch
- Code reviews MUST verify: test coverage, error handling, logging, documentation
- Performance regressions MUST be identified and justified before deployment

### Review Process

- Pull requests MUST include: feature description, test plan, performance impact
- Prediction algorithm changes MUST include accuracy metrics and baseline comparison
- Data model changes MUST include migration scripts and rollback procedures
- Security-sensitive changes require extra scrutiny

---

## 📚 Additional Resources

- **Constitution**: `.specify/memory/constitution.md` - Core principles and standards
- **Specifications**: `specs/` - Feature specifications using Specify framework
- **Agent Files**: `agent_files/` - Claude-generated documentation and logs
- **Current Status**: `current-status.md` - Session continuity tracking (when created)
- **Changelog and history**: `agent_files/changelog.log` - history of all changes in the project by calude code ai.cl

---

**Version**: 1.0.0 | **Last Updated**: 2025-10-31

---

# important-instruction-reminders

- Do what has been asked; nothing more, nothing less
- NEVER create files unless absolutely necessary for achieving your goal
- ALWAYS prefer editing an existing file to creating a new one
- NEVER proactively create documentation files (*.md) or README files unless explicitly requested
- Only use emojis if the user explicitly requests it
