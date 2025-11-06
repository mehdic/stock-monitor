# StockMonitor

**Month-End Stock Recommendation System**

A comprehensive financial application for generating data-driven stock recommendations based on multi-factor analysis.

## Features

### Core Functionality
- **Portfolio Management**: Upload and manage stock holdings with CSV import
- **Multi-Factor Analysis**: Value, Momentum, Quality, and Revisions factors with sector normalization
- **Constraint Tuning**: Customize risk parameters with real-time impact preview
- **Month-End Recommendations**: Automated recommendation generation with transparency
- **Performance Attribution**: Track P&L, benchmark comparison, top contributors/detractors
- **Backtesting**: Historical strategy evaluation with CAGR, Sharpe ratio, max drawdown
- **Exclusion Management**: Understand why stocks were excluded from recommendations
- **Data Freshness Monitoring**: Real-time tracking of external data source health

### Technical Highlights
- **Test-First Development**: Comprehensive test coverage (contract, integration, unit tests)
- **Sector Normalization**: Z-score calculation for cross-sector factor comparison
- **Versioning**: Full audit trail for constraint changes
- **"No Trade" Logic**: Smart recommendations when costs exceed expected benefits
- **Real-Time Updates**: WebSocket integration for live recommendation status
- **External Data Integration**: Alpha Vantage, IEX Cloud, ECB FX rates
- **Security**: JWT authentication, role-based access control (OWNER, VIEWER, SERVICE)

## Tech Stack

### Backend
- **Java 17** with Spring Boot 3.2
- **PostgreSQL 15** + TimescaleDB for time-series data
- **Redis** for caching
- **Spring Security** with JWT
- **Spring Batch** for scheduled jobs
- **Liquibase** for database migrations

### Frontend
- **React 18.2** with TypeScript
- **Vite** build tool
- **React Query** (@tanstack/react-query) for data fetching
- **Tailwind CSS** for styling
- **WebSocket** (STOMP) for real-time updates

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- PostgreSQL 15
- Redis

### Development Setup

1. **Clone repository**
   ```bash
   git clone <repository-url>
   cd StockMonitor
   ```

2. **Start infrastructure**
   ```bash
   docker-compose up -d postgres redis
   ```

3. **Backend**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

4. **Frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. **Access application**
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080
   - API Docs: http://localhost:8080/swagger-ui.html

### Production Deployment

```bash
docker-compose -f docker/docker-compose.prod.yml up -d
```

## Architecture

### Backend Structure
```
backend/
├── src/main/java/com/stockmonitor/
│   ├── controller/      # REST API endpoints
│   ├── service/         # Business logic
│   ├── engine/          # Recommendation & backtest engines
│   ├── model/           # JPA entities
│   ├── dto/             # Data transfer objects
│   ├── repository/      # Database access
│   ├── security/        # Authentication & authorization
│   ├── validation/      # Data validation & anomaly detection
│   ├── integration/     # External data providers
│   ├── batch/           # Scheduled jobs
│   └── monitoring/      # Alerting & observability
```

### Frontend Structure
```
frontend/
├── src/
│   ├── pages/           # Main application pages
│   ├── components/      # Reusable UI components
│   ├── services/        # API clients
│   └── hooks/           # Custom React hooks
```

## Key Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - Login
- `POST /api/auth/verify-email` - Email verification

### Portfolio Management
- `GET /api/portfolios/{id}` - Get portfolio details
- `POST /api/portfolios/{id}/holdings/upload` - Upload holdings CSV
- `GET /api/portfolios/{id}/performance` - Performance metrics

### Recommendations
- `POST /api/runs` - Trigger recommendation run (OWNER only)
- `GET /api/runs/{id}` - Get run details
- `GET /api/runs/{id}/recommendations` - Get recommendations
- `GET /api/runs/{id}/exclusions` - Get excluded stocks

### Constraints
- `GET /api/constraints/defaults` - Default constraints
- `PUT /api/portfolios/{id}/constraints` - Update constraints (OWNER only)
- `POST /api/portfolios/{id}/constraints/preview` - Preview impact
- `POST /api/portfolios/{id}/constraints/reset` - Reset to defaults

### Analysis
- `GET /api/portfolios/{id}/factors` - Factor scores (sector-normalized)
- `GET /api/holdings/{id}/factors` - Detailed factor breakdown

### Backtesting
- `POST /api/backtests` - Run backtest (OWNER only)
- `GET /api/backtests/{id}` - Get backtest results

### Data Sources
- `GET /api/data-sources` - All data sources with health status
- `GET /api/data-sources/{id}/health` - Detailed health info

## Testing

```bash
# Backend tests
cd backend
./mvnw test                    # All tests
./mvnw verify                  # Integration tests

# Frontend tests
cd frontend
npm test                       # Unit tests
npm run test:e2e              # E2E tests (Playwright)
```

## Environment Variables

Create `.env` file in project root:

```env
# Database
DB_PASSWORD=your_postgres_password

# External APIs
ALPHA_VANTAGE_API_KEY=your_key
IEX_CLOUD_API_KEY=your_key

# Security
JWT_SECRET=your_jwt_secret
```

## Contributing

See [CLAUDE.md](CLAUDE.md) for development guidelines and AI assistant instructions.

## License

Proprietary - All rights reserved

## Support

For issues and questions, contact: support@stockmonitor.com

---

**Version**: 1.0.0
**Last Updated**: 2025-11-01
