# Month-End Market Analyst - Developer Quickstart Guide

This guide will get you up and running with the Month-End Market Analyst feature in 5 minutes.

## Prerequisites

Before starting, ensure you have the following installed:

### Required
- **Java 17 JDK** or later
  - Verify: `java -version`
  - Download: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or use [OpenJDK](https://jdk.java.net/)

- **Node.js 18+** and npm
  - Verify: `node -v && npm -v`
  - Download: [nodejs.org](https://nodejs.org/)

- **PostgreSQL 15** with TimescaleDB extension
  - Verify: `psql --version`
  - Download: [postgresql.org](https://www.postgresql.org/download/)
  - TimescaleDB: [timescale.com/install](https://docs.timescale.com/self-hosted/latest/install/)

- **Git**
  - Verify: `git --version`
  - Download: [git-scm.com](https://git-scm.com/)

### Recommended
- **Docker and Docker Compose** (for containerized PostgreSQL)
  - Verify: `docker --version && docker-compose --version`
  - Download: [docker.com](https://www.docker.com/products/docker-desktop)

- **IDE**
  - IntelliJ IDEA (recommended for Java development)
  - VS Code (with Java and frontend extensions)

---

## Quick Start (5 Minutes)

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd StockMonitor
```

### Step 2: Set Up Environment Variables

```bash
cp .env.example .env
```

Edit `.env` with your configuration (see [Configuration](#configuration) section below):

```bash
# Database
DATABASE_URL=postgresql://user:password@localhost:5432/stock_monitor
DATABASE_USER=postgres
DATABASE_PASSWORD=your_secure_password

# Backend
SERVER_PORT=8080
JWT_SECRET=your-jwt-secret-key-here

# Frontend
VITE_API_URL=http://localhost:8080/api

# Market Data Providers
ALPHA_VANTAGE_API_KEY=your_api_key_here
FINNHUB_API_KEY=your_api_key_here
```

### Step 3: Start PostgreSQL

#### Option A: Using Docker Compose (Recommended)

```bash
docker-compose up postgres -d
```

Wait for PostgreSQL to be ready (about 10 seconds):

```bash
docker-compose logs postgres | grep "database system is ready"
```

#### Option B: Using Local PostgreSQL Installation

```bash
# macOS with Homebrew
brew services start postgresql@15

# Linux
sudo systemctl start postgresql

# Windows
# Start PostgreSQL service from Services management
```

Verify connection:

```bash
psql -U postgres -d postgres -c "SELECT version();"
```

### Step 4: Run Database Migrations

#### Using Gradle (if configured)

```bash
./gradlew flywayMigrate
```

#### Using Maven (if configured)

```bash
mvn flyway:migrate
```

#### Manual Migration with psql

```bash
psql -U postgres -d stock_monitor -f schema/001-initial-schema.sql
psql -U postgres -d stock_monitor -f schema/002-timescaledb-setup.sql
```

### Step 5: Start Backend Server

#### Using Gradle

```bash
./gradlew bootRun
```

#### Using Maven

```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

Check health endpoint:

```bash
curl http://localhost:8080/api/health
```

### Step 6: Install Frontend Dependencies

```bash
cd frontend
npm install
```

### Step 7: Start Frontend Development Server

```bash
npm run dev
```

The frontend will start on `http://localhost:3000`

### Step 8: Access the Application

Open your browser and navigate to:

```
http://localhost:3000
```

**Congratulations!** You're now running the Month-End Market Analyst feature.

---

## Configuration

### Environment Variables (.env)

Create a `.env` file in the project root with the following variables:

```bash
# Database Configuration
DATABASE_URL=postgresql://user:password@localhost:5432/stock_monitor
DATABASE_USER=postgres
DATABASE_PASSWORD=your_secure_password
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=stock_monitor

# Backend Configuration
SERVER_PORT=8080
SPRING_PROFILE=dev
LOG_LEVEL=INFO

# Security
JWT_SECRET=your-very-secure-jwt-secret-key-min-32-chars
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Frontend
VITE_API_URL=http://localhost:8080/api
VITE_ENV=development

# Market Data Providers
ALPHA_VANTAGE_API_KEY=demo
FINNHUB_API_KEY=your_api_key_here
POLYGON_API_KEY=your_api_key_here

# Email Configuration (Optional)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM=noreply@stockmonitor.local

# Feature Flags
ENABLE_MARKET_ANALYSIS=true
ENABLE_PORTFOLIO_EXPORT=true
ENABLE_NOTIFICATIONS=true
```

### Application Configuration (application.yml)

Backend configuration file at `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: stock-monitor
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL13Dialect
        format_sql: true
  cache:
    type: redis
    redis:
      host: localhost
      port: 6379

server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /api

logging:
  level:
    root: ${LOG_LEVEL:INFO}
    com.stockmonitor: DEBUG

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION_MS:86400000}
  cors:
    allowedOrigins: ${CORS_ALLOWED_ORIGINS}
```

### API Keys for Market Data Providers

Get free API keys from:

- **Alpha Vantage**: [alphavantage.co](https://www.alphavantage.co/api/) (Free tier: 5 calls/min)
- **Finnhub**: [finnhub.io](https://finnhub.io/register) (Free tier: 60 calls/min)
- **Polygon**: [polygon.io](https://polygon.io/) (Free tier: 5 calls/min)

Update in `.env`:

```bash
ALPHA_VANTAGE_API_KEY=your_actual_key
FINNHUB_API_KEY=your_actual_key
POLYGON_API_KEY=your_actual_key
```

---

## Running Tests

### Backend Unit Tests

Using Gradle:

```bash
./gradlew test
```

Using Maven:

```bash
mvn test
```

View test results:

```bash
# Gradle
cat build/reports/tests/test/index.html

# Maven
cat target/surefire-reports/index.html
```

### Backend Integration Tests

Using Gradle:

```bash
./gradlew integrationTest
```

Using Maven:

```bash
mvn verify -P integration-tests
```

### Frontend Unit Tests

```bash
cd frontend
npm test
```

Run tests in watch mode:

```bash
npm test -- --watch
```

### Frontend E2E Tests

Using Playwright:

```bash
cd frontend
npm run test:e2e
```

Using Cypress (if configured):

```bash
cd frontend
npm run cypress:open
```

### Run All Tests

```bash
# Backend + Frontend
./scripts/test-all.sh

# Or manually
./gradlew test && cd frontend && npm test
```

---

## Sample Data

### Seed Development Database

Load sample portfolios and test data:

```bash
./gradlew loadSampleData
```

Or manually:

```bash
psql -U postgres -d stock_monitor -f scripts/seed-data.sql
```

### Pre-Configured Test Accounts

The seed data includes test users:

| Email | Password | Role | Portfolio |
|-------|----------|------|-----------|
| analyst@test.com | TestPass123! | Analyst | Tech Stocks |
| admin@test.com | AdminPass123! | Admin | All Portfolios |
| user@test.com | UserPass123! | User | Sample Portfolio |

### Generate Mock Market Data

For development without external API calls:

```bash
./gradlew generateMockData
```

This creates mock market data for the last 30 days.

### Reset Database to Sample State

```bash
# Drop and recreate with fresh seed data
./gradlew resetDatabase loadSampleData
```

---

## Common Issues

### Port Already in Use

#### Port 8080 (Backend)

Check what's using port 8080:

```bash
# macOS/Linux
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

Kill the process or change the port in `.env`:

```bash
SERVER_PORT=8081
```

#### Port 3000 (Frontend)

```bash
# macOS/Linux
lsof -i :3000

# Windows
netstat -ano | findstr :3000
```

Change port in `frontend/vite.config.ts`:

```javascript
export default defineConfig({
  server: {
    port: 3001
  }
})
```

#### Port 5432 (PostgreSQL)

```bash
lsof -i :5432
```

Change in `docker-compose.yml`:

```yaml
services:
  postgres:
    ports:
      - "5433:5432"  # Use 5433 instead
```

And update `.env`:

```bash
DATABASE_PORT=5433
```

### Database Connection Failures

**Error: `java.sql.SQLException: Connection refused`**

```bash
# Check if PostgreSQL is running
psql -U postgres -c "SELECT 1"

# If using Docker, verify container is running
docker ps | grep postgres

# Check database exists
psql -U postgres -l | grep stock_monitor
```

**Create database if missing:**

```bash
psql -U postgres -c "CREATE DATABASE stock_monitor;"
psql -U postgres -c "CREATE EXTENSION IF NOT EXISTS timescaledb;" -d stock_monitor
```

### CORS Errors During Development

**Error: `Access to XMLHttpRequest blocked by CORS policy`**

Verify `CORS_ALLOWED_ORIGINS` in `.env` includes your frontend URL:

```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

Check backend configuration accepts the origin:

```bash
curl -i -X OPTIONS http://localhost:8080/api/health \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET"
```

### API Rate Limit Testing

For development without consuming real API rate limits, use mock data:

```bash
# Disable real API calls in application.yml
app:
  market-data:
    use-mock: true
    mock-delay-ms: 500  # Simulate API latency

# Or override via environment
export USE_MOCK_MARKET_DATA=true
```

### Node Module Cache Issues

```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### Gradle Daemon Issues

```bash
./gradlew --stop
./gradlew clean build
```

---

## Development Workflow

### Branch Naming Conventions

Follow these conventions when creating branches:

```
feature/feature-name
bugfix/bug-description
refactor/refactor-description
docs/documentation-update
test/test-addition
```

Examples:

```bash
git checkout -b feature/portfolio-analysis
git checkout -b bugfix/date-formatting-issue
git checkout -b docs/api-documentation
```

### Commit Message Format

Use the conventional commits format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:

```bash
git commit -m "feat(portfolio): add month-end analysis endpoint"
git commit -m "fix(database): correct timestamp timezone handling"
git commit -m "docs(api): update authentication documentation"
```

### Running Pre-Commit Hooks

Install and run pre-commit checks:

```bash
# Install pre-commit hooks
./scripts/install-hooks.sh

# Or manually
cp .githooks/* .git/hooks/
chmod +x .git/hooks/*
```

Run manually before committing:

```bash
./gradlew checkstyle spotbugs
cd frontend && npm run lint
```

### Code Formatting

#### Backend (Java)

Using Google Java Format:

```bash
./gradlew googleJavaFormat
```

#### Frontend (JavaScript/TypeScript)

Using Prettier:

```bash
cd frontend
npm run format
npm run format:check
```

Using ESLint:

```bash
cd frontend
npm run lint
npm run lint:fix
```

---

## Useful Commands

### Database Operations

Reset database (careful - destroys data):

```bash
./gradlew flywayClean flywayMigrate
```

Backup database:

```bash
pg_dump -U postgres stock_monitor > backup-$(date +%Y%m%d-%H%M%S).sql
```

Restore database:

```bash
psql -U postgres stock_monitor < backup-20240101-120000.sql
```

Open database console:

```bash
psql -U postgres -d stock_monitor
```

### Cache Operations

Clear Redis cache:

```bash
redis-cli FLUSHALL
```

Or via Docker:

```bash
docker exec redis-container redis-cli FLUSHALL
```

### Viewing Logs

Backend logs (Gradle):

```bash
./gradlew bootRun 2>&1 | tee app.log
```

Backend logs (specific file):

```bash
tail -f logs/stock-monitor.log
```

Frontend logs:

```bash
cd frontend
npm run dev 2>&1 | tee frontend.log
```

Docker container logs:

```bash
docker-compose logs -f postgres
docker-compose logs -f redis
```

### Health Check Endpoints

Backend health:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/readiness
curl http://localhost:8080/api/health/liveness
```

Database connectivity:

```bash
curl http://localhost:8080/api/health/db
```

Frontend dev server:

```bash
curl http://localhost:3000
```

### Gradle Useful Commands

List all tasks:

```bash
./gradlew tasks
```

Clean build artifacts:

```bash
./gradlew clean
```

Build production jar:

```bash
./gradlew build -x test
```

Run specific test:

```bash
./gradlew test --tests=*PortfolioAnalysisTest
```

### Docker Useful Commands

View running containers:

```bash
docker-compose ps
```

Stop services:

```bash
docker-compose down
```

Stop and remove volumes (careful - destroys data):

```bash
docker-compose down -v
```

Rebuild images:

```bash
docker-compose build --no-cache
```

---

## Next Steps

1. **Familiarize yourself with the codebase**
   - Read `README.md` for project overview
   - Check `specs/001-month-end-analyst/spec.md` for feature details

2. **Set up IDE**
   - IntelliJ IDEA: Open project root
   - VS Code: Open root, install recommended extensions

3. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **Make your changes and commit**
   ```bash
   git add .
   git commit -m "feat: your feature description"
   ```

5. **Run tests before pushing**
   ```bash
   ./gradlew test && cd frontend && npm test
   ```

6. **Push and create a pull request**
   ```bash
   git push origin feature/your-feature-name
   ```

---

## Troubleshooting

For additional help:

1. Check project documentation in `/docs`
2. Review GitHub Issues for known problems
3. Contact the development team
4. Check log files for error details

### Enable Debug Logging

```bash
# Backend
export LOG_LEVEL=DEBUG
./gradlew bootRun

# Frontend
export DEBUG=stockmonitor:*
npm run dev
```

---

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [TimescaleDB Documentation](https://docs.timescale.com/)
- [Project Repository](https://github.com/your-org/stock-monitor)

---

Happy coding! If you have any questions, refer to the main documentation or reach out to the team.
