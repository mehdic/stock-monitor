# Technology Research: Month-End Market Analyst

## 1. Market Data Integration

**Decision**: Alpha Vantage with IEX Cloud as fallback

**Rationale**: Alpha Vantage provides comprehensive end-of-day pricing, fundamentals, and technical indicators with a generous free tier (500 calls/day) suitable for month-end workflows. IEX Cloud offers superior reliability and real-time data as a paid fallback option for scaling. Both provide RESTful APIs with JSON responses and strong Java client library support.

**Alternatives Considered**:
- **Polygon.io**: Excellent data quality and coverage, but higher cost structure ($199+/month). Best for high-frequency trading applications.
- **Yahoo Finance API**: Free and widely used, but unofficial/unstable with no SLA guarantees. Risk of breaking changes without notice.
- **Finnhub**: Good fundamental data coverage, competitive pricing, but limited historical depth for backtesting.

**Best Practices**:
- Implement circuit breaker pattern for API failures with automatic fallback
- Cache daily prices aggressively (24-hour TTL minimum)
- Use bulk endpoints where available to minimize API calls
- Store raw API responses for audit trails and reprocessing
- Monitor rate limits proactively with buffer margins (80% threshold)
- Implement exponential backoff with jitter for rate limit errors

---

## 2. Time-Series Data Storage

**Decision**: PostgreSQL with TimescaleDB extension

**Rationale**: TimescaleDB provides time-series optimizations (compression, continuous aggregates, retention policies) while maintaining PostgreSQL's ACID guarantees and relational model. This is critical for financial data accuracy and audit requirements. Avoids operational complexity of dedicated time-series databases while providing 10-100x compression and faster time-range queries through native hypertables.

**Alternatives Considered**:
- **InfluxDB**: Purpose-built for time-series, excellent write performance, but lacks relational joins needed for portfolio/security relationships. Additional operational overhead.
- **TimescaleDB vs PostgreSQL only**: Plain PostgreSQL works but lacks automatic partitioning and compression. TimescaleDB is PostgreSQL-compatible with minimal migration risk.
- **MongoDB Time-Series Collections**: Good for unstructured data, but ACID limitations and weaker consistency model unsuitable for financial calculations.

**Best Practices**:
- Create hypertables partitioned by 1-month intervals for price data
- Use continuous aggregates for pre-computed monthly/quarterly metrics
- Implement compression policies for data older than 90 days
- Define retention policies aligned with regulatory requirements (7+ years)
- Use JSONB columns for flexible metadata storage (splits, dividends)
- Index on (security_id, timestamp DESC) for efficient time-range queries

---

## 3. Factor Calculation Library

**Decision**: Apache Commons Math + custom implementation

**Rationale**: Apache Commons Math provides battle-tested statistical functions (percentiles, distributions, regression) under Apache 2.0 license. Custom implementation for domain-specific metrics (earnings yield, B/M ratio) ensures calculation transparency and auditability required for investment decisions. ta4j is overly complex for fundamental factor calculations.

**Alternatives Considered**:
- **ta4j**: Comprehensive technical analysis library with indicators, but heavyweight for fundamental metrics. Best for technical trading strategies.
- **Pure custom implementation**: Full control and transparency, but reinventing statistical functions introduces bug risk and maintenance burden.
- **JQuantLib**: Java port of QuantLib, excellent for derivatives pricing but overkill for equity factor calculations.

**Best Practices**:
- Wrap Apache Commons Math in domain-specific factor calculation services
- Document all factor formulas with academic references (Fama-French papers)
- Unit test factor calculations against known benchmark values
- Version factor calculation logic for historical reproducibility
- Log intermediate calculation steps for debugging and audit
- Handle missing data gracefully (use median imputation or skip security)

---

## 4. Report Generation

**Decision**: Flying Saucer (OpenHTMLtoPDF)

**Rationale**: Flying Saucer renders XHTML/CSS to PDF with excellent layout control and modern CSS support. HTML templates are easier for designers/analysts to modify than programmatic PDF construction. LGPL 2.1 license is compatible with commercial use. Superior typography and table rendering compared to alternatives.

**Alternatives Considered**:
- **iText**: Industry standard with powerful features, but AGPL license requires commercial licensing ($3000+/year) for proprietary applications.
- **Apache PDFBox**: Apache licensed and low-level control, but complex API requiring manual layout calculations. Poor HTML rendering.
- **JasperReports**: Feature-rich reporting, but XML template format is cumbersome and steep learning curve.

**Best Practices**:
- Use Thymeleaf or Freemarker for HTML template generation
- Design templates with CSS print media queries
- Embed charts as SVG or high-DPI PNGs (300+ DPI)
- Test rendering across page breaks (tables, charts)
- Generate PDFs asynchronously to avoid blocking user requests
- Include metadata (title, author, creation date) for document management

---

## 5. Email Service

**Decision**: AWS SES (Simple Email Service)

**Rationale**: AWS SES provides enterprise-grade deliverability with 99.9% uptime SLA at lowest cost ($0.10/1000 emails). Native AWS integration if using EC2/ECS deployment. Built-in bounce/complaint handling meets compliance requirements. Supports DKIM/SPF authentication for inbox delivery.

**Alternatives Considered**:
- **SendGrid**: Excellent API and documentation, superior analytics dashboard, but 10x higher cost ($19.95+/month). Best for marketing emails.
- **Mailgun**: Developer-friendly API and good deliverability, mid-range pricing, but less mature than SendGrid/SES.
- **SMTP Direct**: No cost but poor deliverability (spam filters), no bounce handling, requires server configuration and IP reputation management.

**Best Practices**:
- Implement double opt-in for subscription confirmation
- Use SES configuration sets for bounce/complaint tracking
- Store unsubscribe preferences in database with audit trail
- Include plain-text version alongside HTML for spam filter compatibility
- Use templating system (Thymeleaf) for consistent branding
- Rate limit email sending (14 emails/second SES default)
- Implement retry queue for temporary failures (4xx errors)

---

## 6. Scheduled Job Processing

**Decision**: Spring @Scheduled annotations + Spring Boot

**Rationale**: Spring @Scheduled provides declarative cron-based scheduling with minimal configuration for T-3, T-1, T workflow. Sufficient for single-instance deployments with simple orchestration needs. Easy to test and monitor through Spring actuator endpoints. Upgrade path to Spring Batch if complexity increases.

**Alternatives Considered**:
- **Quartz Scheduler**: Industry standard with clustering support and persistent job storage, but overkill for monthly workflows. Best for high-frequency or distributed job scheduling.
- **Spring Batch**: Powerful for complex ETL workflows with chunk processing and restart capabilities, but heavyweight for simple scheduled tasks. Use if job steps exceed 5+ stages.
- **Kubernetes CronJobs**: Containerized scheduling with retry policies, but adds deployment complexity and requires K8s infrastructure.

**Best Practices**:
- Use cron expressions with timezone configuration (America/New_York for US markets)
- Implement idempotency checks (verify month not already processed)
- Add @Async for parallel execution of independent sub-tasks
- Use distributed locks (ShedLock) if scaling to multiple instances
- Configure fixed-delay vs fixed-rate based on job duration variability
- Expose job metrics via Micrometer for monitoring
- Implement job failure alerting through logging and email notifications

---

## 7. Real-Time Notifications

**Decision**: Server-Sent Events (SSE) with Spring WebFlux

**Rationale**: SSE provides unidirectional server-to-client push over standard HTTP, eliminating need for WebSocket handshake complexity. Automatic reconnection built into browser EventSource API. Simpler than WebSockets for read-only notification use case (job progress, report ready). No additional protocol or port configuration required.

**Alternatives Considered**:
- **WebSockets (Spring WebSocket)**: Bidirectional communication and full-duplex, but unnecessary for one-way notifications. Requires additional STOMP/SockJS configuration and proxy support.
- **Polling**: Simple implementation but inefficient (wasted requests) and higher latency (5-10s delay typical). Acceptable only as SSE fallback.
- **Push Notifications (Firebase)**: Mobile-focused, requires external service dependency, adds complexity for web-only application.

**Best Practices**:
- Implement automatic reconnection with exponential backoff in client
- Use message IDs for deduplication and replay from last event
- Set keep-alive heartbeat (30s interval) to prevent proxy timeouts
- Implement polling fallback for IE11 or corporate proxy environments
- Stream job progress events (0%, 25%, 50%, 75%, 100%) for UX
- Close SSE connections server-side after job completion
- Use Spring WebFlux reactive streams for efficient connection handling

---

## 8. Authentication & Authorization

**Decision**: JWT with Spring Security + OAuth2 preparation

**Rationale**: JWT provides stateless authentication suitable for RESTful APIs and future mobile clients. No server-side session storage reduces operational complexity. Spring Security's OAuth2 Resource Server support enables future integration with corporate SSO (Okta, Auth0) without authentication redesign. Short-lived access tokens (15 min) with refresh tokens balance security and UX.

**Alternatives Considered**:
- **Session-based auth**: Simpler implementation with Spring Session, but requires sticky sessions or distributed session store (Redis). Limits horizontal scaling and API-first architecture.
- **OAuth2 immediately**: More complex initial setup, requires IdP configuration, but best for multi-application ecosystems. Defer until SSO requirement confirmed.
- **Basic Auth over HTTPS**: Simplest option but requires credentials on every request and lacks token expiration. Unsuitable for production.

**Best Practices**:
- Use RS256 (asymmetric) instead of HS256 for signature verification
- Implement refresh token rotation to detect token theft
- Store JWTs in httpOnly cookies (XSS protection) not localStorage
- Include minimal claims (user_id, roles) to keep token size small
- Implement token revocation list for logout and security incidents
- Use Spring Security's @PreAuthorize for method-level authorization
- Enforce password complexity (12+ chars, mixed case, numbers, symbols)
- Implement rate limiting on login endpoints (5 attempts/15min)

---

## 9. Caching Strategy

**Decision**: Caffeine for application-level caching + Redis for distributed cache

**Rationale**: Caffeine provides high-performance local caching (10x faster than Guava) with automatic eviction and statistics integration. Use for factor calculations and reference data. Add Redis for distributed caching when scaling to multiple instances or caching across services (portfolio snapshots, user preferences). Hybrid approach balances performance and consistency.

**Alternatives Considered**:
- **Redis only**: Network latency (1-5ms) adds overhead for frequently accessed data. Best for distributed state and session storage.
- **Caffeine only**: Cannot share cache across instances, leading to redundant calculations. Acceptable for single-instance deployments.
- **Ehcache**: Feature-rich (persistence, replication), but heavier weight than Caffeine and less modern API. Legacy option.

**Best Practices**:
- Use Caffeine for read-heavy, compute-intensive operations (factor scores)
- Use Redis for session storage and cross-instance data sharing
- Implement cache-aside pattern for portfolio data
- Set TTL based on data volatility (daily prices: 24hr, factor scores: 1hr)
- Use cache key versioning for schema changes (v1:portfolio:123)
- Monitor cache hit rates via Micrometer (target 80%+ for effective caching)
- Implement circuit breaker for Redis failures with cache-aside fallback
- Use Spring Cache abstraction for provider-agnostic code

---

## 10. Observability Stack

**Decision**: Prometheus + Grafana for metrics, Logback + ELK for logging

**Rationale**: Prometheus provides time-series metrics with powerful PromQL queries and pull-based model. Grafana offers best-in-class visualization and alerting with pre-built Spring Boot dashboards. ELK Stack (Elasticsearch, Logstash, Kibana) provides centralized logging with full-text search for troubleshooting. Spring Boot actuator exports Prometheus metrics out-of-box.

**Alternatives Considered**:
- **ELK for everything**: Can store metrics in Elasticsearch, but Prometheus is more efficient for time-series and has better alerting. Use ELK for logs only.
- **CloudWatch (if AWS)**: Fully managed and integrated with AWS services, but limited query capabilities and higher cost. Good for AWS-native deployments.
- **Datadog/New Relic**: Comprehensive SaaS APM with excellent UX, but $15-$25/host/month pricing. Best for enterprises with budget.

**Best Practices**:
- Export custom metrics via Micrometer (job duration, API latency, cache hits)
- Use structured logging with JSON format for easy parsing
- Tag logs with correlation IDs for request tracing
- Configure Prometheus scrape interval (15s) and retention (30 days)
- Set up Grafana alerts for critical metrics (job failures, API errors >1%)
- Use Logstash filters to parse and enrich logs before Elasticsearch
- Implement log sampling for high-volume endpoints (1% sample rate)
- Create runbook links in Grafana alerts for operational responses

---

## 11. Backtesting Engine

**Decision**: Custom implementation with Apache Commons Math

**Rationale**: Equity curve, Sharpe ratio, and drawdown calculations are straightforward statistical operations that don't require QuantLib's derivatives pricing complexity. Custom implementation provides full transparency for investment committee review and avoids licensing concerns. Apache Commons Math handles statistical functions (standard deviation, correlation).

**Alternatives Considered**:
- **QuantLib Java (JQuantLib)**: Comprehensive financial library with advanced risk metrics, but C++ JNI bindings are fragile and GPL license is restrictive. Overkill for equity backtesting.
- **Zipline (Python)**: Industry-standard backtesting from Quantopian, but requires Python interop (Jython or REST API) adding complexity. Best for quant teams already using Python.
- **ta4j**: Includes backtesting framework for technical strategies, but fundamental factor backtesting requires custom logic anyway.

**Best Practices**:
- Calculate returns in basis points to avoid floating-point precision issues
- Use logarithmic returns for compounding accuracy
- Implement rolling window calculations for time-series metrics
- Calculate Sharpe ratio with risk-free rate (10Y Treasury)
- Include maximum drawdown, Sortino ratio, and Calmar ratio
- Test backtest engine against known benchmark results (S&P 500)
- Store backtest results with metadata (parameters, date range, version)
- Validate no look-ahead bias in factor calculations
- Implement transaction cost modeling (0.1% per trade typical)

---

## 12. Frontend State Management

**Decision**: React Query (TanStack Query) for server state + Zustand for UI state

**Rationale**: React Query eliminates 90% of state management boilerplate by handling server-state caching, refetching, and synchronization automatically. Background refetch keeps data fresh without user intervention. Zustand provides lightweight (<1KB) global state for UI concerns (theme, sidebar collapse). This separation of server-state vs client-state is best practice for modern React applications.

**Alternatives Considered**:
- **Redux**: Battle-tested and comprehensive, but verbose boilerplate (actions, reducers, selectors) and performance overhead for simple apps. Best for complex applications with intricate state logic.
- **Zustand for everything**: Can manage server state, but requires manual implementation of caching, refetching, and optimistic updates that React Query provides.
- **Apollo Client**: Excellent for GraphQL APIs with normalized cache, but overkill for REST APIs and adds 30KB bundle size.

**Best Practices**:
- Use React Query for all server data (portfolios, securities, reports)
- Use Zustand for client-only state (UI preferences, modal state)
- Configure React Query staleTime based on data volatility (daily prices: 5min)
- Implement optimistic updates for create/update operations
- Use React Query's useQueries for parallel fetching
- Enable React Query DevTools in development for cache inspection
- Implement error boundaries for graceful error handling
- Use suspense mode for cleaner loading state management
- Prefetch data on hover for improved perceived performance

---

## Summary

These technology decisions prioritize:
- **Data Accuracy**: PostgreSQL ACID guarantees, TimescaleDB compression, audit trails
- **Performance**: Caffeine caching, React Query optimization, TimescaleDB hypertables
- **Regulatory Compliance**: Email opt-out management, audit logging, data retention policies
- **Operational Simplicity**: Spring Boot ecosystem, managed services (AWS SES), minimal moving parts
- **Cost Efficiency**: Open-source tooling, AWS SES pricing, free-tier data providers
- **Scalability Path**: JWT stateless auth, Redis distributed cache, Prometheus metrics

All decisions favor proven, well-documented technologies with strong Java/Spring ecosystem support to minimize integration risk and development time.
