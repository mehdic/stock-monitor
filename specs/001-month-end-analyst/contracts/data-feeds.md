# External Data Feeds Integration Contracts

**Version**: 1.0.0
**Last Updated**: 2025-10-30

## Overview

The Month-End Market Analyst system integrates with external data providers for:
1. **End-of-Day Prices**: Daily closing prices, volume, splits, dividends
2. **Fundamental Data**: Financial statements, ratios, business metrics
3. **Analyst Estimates**: EPS forecasts, revisions, recommendations
4. **Corporate Calendar**: Earnings dates, dividend dates, splits
5. **FX Rates**: Currency conversion for multi-currency portfolios

This document defines integration contracts, error handling strategies, rate limiting, caching, and health monitoring for each data source.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                   StockMonitor Application                       │
│  ┌─────────────────┐  ┌──────────────────┐  ┌────────────────┐ │
│  │ Data Ingestion  │  │  Cache Layer     │  │  Health Check  │ │
│  │    Service      │→ │  (Redis 7 days)  │← │    Service     │ │
│  └─────────────────┘  └──────────────────┘  └────────────────┘ │
│           ↓                                           ↑          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │             PostgreSQL TimescaleDB                       │   │
│  │  • prices (hypertable, indexed by symbol+date)           │   │
│  │  • fundamentals (quarterly/annual snapshots)             │   │
│  │  • estimates (historical revisions tracking)             │   │
│  │  • calendar (upcoming events)                            │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                          ↓           ↑
            ┌─────────────────────────────────────┐
            │   External Data Providers (APIs)    │
            │  • Alpha Vantage (Primary)          │
            │  • IEX Cloud (Fallback)             │
            │  • ECB/Fed (FX Rates)               │
            └─────────────────────────────────────┘
```

---

## Data Provider: Alpha Vantage (Primary)

**Base URL**: `https://www.alphavantage.co/query`
**Documentation**: https://www.alphavantage.co/documentation/
**Rate Limits**: 5 API calls per minute (free tier), 75 calls per minute (premium)

### Authentication

API key passed as query parameter:
```
https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=AAPL&apikey=YOUR_API_KEY
```

**Environment Variable**: `ALPHA_VANTAGE_API_KEY`

---

### Contract 1: End-of-Day Prices

**Endpoint**: `TIME_SERIES_DAILY`

**Request**:
```
GET https://www.alphavantage.co/query
  ?function=TIME_SERIES_DAILY
  &symbol=AAPL
  &outputsize=full
  &apikey=YOUR_API_KEY
```

**Response Schema**:
```json
{
  "Meta Data": {
    "1. Information": "Daily Prices (open, high, low, close) and Volumes",
    "2. Symbol": "AAPL",
    "3. Last Refreshed": "2025-10-30",
    "4. Output Size": "Full size",
    "5. Time Zone": "US/Eastern"
  },
  "Time Series (Daily)": {
    "2025-10-30": {
      "1. open": "175.50",
      "2. high": "178.20",
      "3. low": "174.80",
      "4. close": "177.45",
      "5. volume": "52341920"
    },
    "2025-10-29": {
      "1. open": "173.20",
      "2. high": "176.00",
      "3. low": "172.90",
      "4. close": "175.30",
      "5. volume": "48234567"
    }
  }
}
```

**Mapping to Database**:
```sql
CREATE TABLE prices (
  id BIGSERIAL PRIMARY KEY,
  symbol VARCHAR(10) NOT NULL,
  date DATE NOT NULL,
  open DECIMAL(12,4) NOT NULL,
  high DECIMAL(12,4) NOT NULL,
  low DECIMAL(12,4) NOT NULL,
  close DECIMAL(12,4) NOT NULL,
  volume BIGINT NOT NULL,
  adjusted_close DECIMAL(12,4),
  split_coefficient DECIMAL(8,4) DEFAULT 1.0,
  dividend_amount DECIMAL(8,4) DEFAULT 0.0,
  source VARCHAR(50) DEFAULT 'ALPHA_VANTAGE',
  ingested_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(symbol, date, source)
);

-- Convert to TimescaleDB hypertable for time-series optimization
SELECT create_hypertable('prices', 'date');

-- Index for fast symbol lookups
CREATE INDEX idx_prices_symbol_date ON prices(symbol, date DESC);
```

**Validation Rules**:
- `open`, `high`, `low`, `close` must be positive
- `high` >= `low`
- `close` within range [low, high]
- `volume` >= 0
- Reject if price change > 50% from previous day (likely data error) → manual review

**Update Frequency**:
- **Market Hours**: Intraday updates not used (end-of-day system)
- **After Market Close**: Update daily at 5:30 PM ET (30 minutes after market close)
- **Staleness Threshold**: 24 hours (block recommendations if not updated by next market open)

---

### Contract 2: Fundamental Data

**Endpoint**: `OVERVIEW` + `INCOME_STATEMENT` + `BALANCE_SHEET` + `CASH_FLOW`

**Request (Company Overview)**:
```
GET https://www.alphavantage.co/query
  ?function=OVERVIEW
  &symbol=AAPL
  &apikey=YOUR_API_KEY
```

**Response Schema (Excerpt)**:
```json
{
  "Symbol": "AAPL",
  "AssetType": "Common Stock",
  "Name": "Apple Inc",
  "Exchange": "NASDAQ",
  "Currency": "USD",
  "Sector": "TECHNOLOGY",
  "Industry": "ELECTRONIC COMPUTERS",
  "MarketCapitalization": "2750000000000",
  "PERatio": "28.5",
  "EPS": "6.23",
  "PriceToBookRatio": "45.2",
  "ReturnOnEquityTTM": "1.475",
  "GrossProfitTTM": "169148000000",
  "DividendYield": "0.0045",
  "AnalystTargetPrice": "185.50",
  "52WeekHigh": "182.45",
  "52WeekLow": "148.20"
}
```

**Request (Income Statement)**:
```
GET https://www.alphavantage.co/query
  ?function=INCOME_STATEMENT
  &symbol=AAPL
  &apikey=YOUR_API_KEY
```

**Response Schema (Excerpt)**:
```json
{
  "symbol": "AAPL",
  "annualReports": [
    {
      "fiscalDateEnding": "2024-09-30",
      "reportedCurrency": "USD",
      "totalRevenue": "391035000000",
      "grossProfit": "169148000000",
      "operatingIncome": "114301000000",
      "netIncome": "96995000000",
      "ebitda": "123136000000"
    }
  ],
  "quarterlyReports": [
    {
      "fiscalDateEnding": "2024-09-30",
      "reportedCurrency": "USD",
      "totalRevenue": "94930000000",
      "grossProfit": "42658000000",
      "netIncome": "22956000000"
    }
  ]
}
```

**Mapping to Database**:
```sql
CREATE TABLE fundamentals (
  id BIGSERIAL PRIMARY KEY,
  symbol VARCHAR(10) NOT NULL,
  fiscal_period VARCHAR(10) NOT NULL, -- 'Q1', 'Q2', 'Q3', 'Q4', 'FY'
  fiscal_year INTEGER NOT NULL,
  fiscal_date_ending DATE NOT NULL,
  currency VARCHAR(3) NOT NULL,

  -- Income Statement
  total_revenue DECIMAL(20,2),
  gross_profit DECIMAL(20,2),
  operating_income DECIMAL(20,2),
  net_income DECIMAL(20,2),
  ebitda DECIMAL(20,2),
  eps DECIMAL(10,4),

  -- Balance Sheet
  total_assets DECIMAL(20,2),
  total_liabilities DECIMAL(20,2),
  total_equity DECIMAL(20,2),
  cash_and_equivalents DECIMAL(20,2),

  -- Cash Flow
  operating_cash_flow DECIMAL(20,2),
  capital_expenditures DECIMAL(20,2),
  free_cash_flow DECIMAL(20,2),

  -- Calculated Ratios
  gross_margin DECIMAL(8,4), -- gross_profit / total_revenue
  operating_margin DECIMAL(8,4),
  net_margin DECIMAL(8,4),
  roe DECIMAL(8,4), -- net_income / total_equity
  roic DECIMAL(8,4),
  debt_to_equity DECIMAL(8,4),

  source VARCHAR(50) DEFAULT 'ALPHA_VANTAGE',
  ingested_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(symbol, fiscal_period, fiscal_year, source)
);

CREATE INDEX idx_fundamentals_symbol_date ON fundamentals(symbol, fiscal_date_ending DESC);
```

**Validation Rules**:
- `total_revenue`, `net_income`, `total_assets` must be present
- Calculated ratios verified against reported ratios (tolerance: 1%)
- Reject if ratios are outliers (e.g., gross margin > 100% or < -50%)
- Publication delay: Assume 10-K/10-Q available 45 days after quarter end

**Update Frequency**:
- **Earnings Season**: Daily checks during earnings season (4x per year)
- **Off-Season**: Weekly checks
- **Staleness Threshold**: 120 days (block recommendations if no update in 4 months)

---

### Contract 3: Analyst Estimates and Revisions

**Endpoint**: `EARNINGS` (includes historical EPS estimates)

**Request**:
```
GET https://www.alphavantage.co/query
  ?function=EARNINGS
  &symbol=AAPL
  &apikey=YOUR_API_KEY
```

**Response Schema (Excerpt)**:
```json
{
  "symbol": "AAPL",
  "annualEarnings": [
    {
      "fiscalDateEnding": "2024-09-30",
      "reportedEPS": "6.23"
    }
  ],
  "quarterlyEarnings": [
    {
      "fiscalDateEnding": "2024-09-30",
      "reportedDate": "2024-10-31",
      "reportedEPS": "1.64",
      "estimatedEPS": "1.59",
      "surprise": "0.05",
      "surprisePercentage": "3.145"
    }
  ]
}
```

**Note**: Alpha Vantage provides historical actuals and surprises, but not forward estimates or analyst counts. For forward estimates, fallback to IEX Cloud or supplement with manual feeds.

**Mapping to Database**:
```sql
CREATE TABLE analyst_estimates (
  id BIGSERIAL PRIMARY KEY,
  symbol VARCHAR(10) NOT NULL,
  estimate_date DATE NOT NULL, -- Date estimate was recorded
  fiscal_period VARCHAR(10) NOT NULL,
  fiscal_year INTEGER NOT NULL,
  fiscal_date_ending DATE NOT NULL,

  -- Forward Estimates
  mean_eps_estimate DECIMAL(10,4),
  high_eps_estimate DECIMAL(10,4),
  low_eps_estimate DECIMAL(10,4),
  analyst_count INTEGER,

  -- Revisions (change from previous week/month)
  revisions_up_1m INTEGER DEFAULT 0,
  revisions_down_1m INTEGER DEFAULT 0,
  revisions_up_3m INTEGER DEFAULT 0,
  revisions_down_3m INTEGER DEFAULT 0,

  -- Historical Surprises
  reported_eps DECIMAL(10,4),
  surprise_amount DECIMAL(10,4),
  surprise_percent DECIMAL(8,4),

  source VARCHAR(50) DEFAULT 'ALPHA_VANTAGE',
  ingested_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(symbol, estimate_date, fiscal_period, fiscal_year, source)
);

CREATE INDEX idx_estimates_symbol_date ON analyst_estimates(symbol, estimate_date DESC);
```

**Validation Rules**:
- `mean_eps_estimate` within range [low, high]
- `analyst_count` > 0 if estimates present
- Reject if surprise > 50% (likely data error)
- Track revision direction (up/down) by comparing estimates week-over-week

**Update Frequency**:
- **Daily**: Track estimate changes during earnings season
- **Weekly**: Track revisions during off-season
- **Staleness Threshold**: 30 days (block recommendations if estimates are stale)

---

### Contract 4: Corporate Calendar (Earnings Dates)

**Endpoint**: `EARNINGS_CALENDAR` (requires premium API key)

**Request**:
```
GET https://www.alphavantage.co/query
  ?function=EARNINGS_CALENDAR
  &horizon=3month
  &apikey=YOUR_API_KEY
```

**Response Format**: CSV

**Example Response**:
```csv
symbol,name,reportDate,fiscalDateEnding,estimate,currency
AAPL,Apple Inc,2024-10-31,2024-09-30,1.59,USD
MSFT,Microsoft Corporation,2024-10-24,2024-09-30,2.35,USD
```

**Mapping to Database**:
```sql
CREATE TABLE corporate_calendar (
  id BIGSERIAL PRIMARY KEY,
  symbol VARCHAR(10) NOT NULL,
  event_type VARCHAR(20) NOT NULL, -- 'EARNINGS', 'DIVIDEND', 'SPLIT'
  event_date DATE NOT NULL,
  fiscal_period VARCHAR(10),
  fiscal_date_ending DATE,
  estimate DECIMAL(10,4), -- EPS estimate if earnings event
  confirmed BOOLEAN DEFAULT FALSE, -- TRUE if company confirmed, FALSE if projected
  source VARCHAR(50) DEFAULT 'ALPHA_VANTAGE',
  ingested_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(symbol, event_type, event_date, source)
);

CREATE INDEX idx_calendar_symbol_date ON corporate_calendar(symbol, event_date);
CREATE INDEX idx_calendar_upcoming ON corporate_calendar(event_date) WHERE event_date >= CURRENT_DATE;
```

**Validation Rules**:
- `event_date` must be in future (or up to 7 days past for recent events)
- Reject if same symbol has duplicate earnings date within 7 days
- Mark `confirmed = TRUE` if company has published earnings release date

**Update Frequency**:
- **Daily**: Refresh upcoming 90 days of calendar
- **Staleness Threshold**: 7 days (block recommendations if calendar not updated in 1 week)

**Earnings Blackout Logic**:
- Exclude stock from recommendations if earnings date within next 48 hours
- Re-include stock 24 hours after earnings release

---

## Data Provider: IEX Cloud (Fallback)

**Base URL**: `https://cloud.iexapis.com/stable`
**Documentation**: https://iexcloud.io/docs/api/
**Rate Limits**: 50,000 messages per month (free tier), 500K+ (paid tiers)

### Authentication

API key passed as query parameter or header:
```
GET https://cloud.iexapis.com/stable/stock/AAPL/quote?token=YOUR_IEX_TOKEN
```

**Environment Variable**: `IEX_CLOUD_API_KEY`

---

### Contract 5: End-of-Day Prices (Fallback)

**Endpoint**: `/stock/{symbol}/chart/{range}`

**Request**:
```
GET https://cloud.iexapis.com/stable/stock/AAPL/chart/1m?token=YOUR_IEX_TOKEN
```

**Response Schema**:
```json
[
  {
    "date": "2025-10-30",
    "open": 175.50,
    "high": 178.20,
    "low": 174.80,
    "close": 177.45,
    "volume": 52341920,
    "unadjustedVolume": 52341920,
    "change": 2.15,
    "changePercent": 1.23,
    "changeOverTime": 0.0123
  }
]
```

**Fallback Logic**:
- Primary: Alpha Vantage
- Fallback: If Alpha Vantage fails 3 consecutive times OR rate limit exceeded, switch to IEX Cloud
- Auto-recovery: Retry Alpha Vantage after 1 hour cooldown

**Validation**: Same rules as Alpha Vantage prices contract

---

### Contract 6: Fundamental Data (Fallback)

**Endpoint**: `/stock/{symbol}/stats` + `/stock/{symbol}/financials`

**Request (Key Stats)**:
```
GET https://cloud.iexapis.com/stable/stock/AAPL/stats?token=YOUR_IEX_TOKEN
```

**Response Schema (Excerpt)**:
```json
{
  "companyName": "Apple Inc.",
  "marketcap": 2750000000000,
  "week52high": 182.45,
  "week52low": 148.20,
  "week52change": 0.18,
  "sharesOutstanding": 15500000000,
  "float": 15400000000,
  "avg10Volume": 50000000,
  "avg30Volume": 52000000,
  "day200MovingAvg": 165.20,
  "day50MovingAvg": 172.50,
  "ttmEPS": 6.23,
  "ttmDividendRate": 0.96,
  "dividendYield": 0.0054,
  "nextDividendDate": "2024-11-15",
  "exDividendDate": "2024-11-08",
  "nextEarningsDate": "2025-01-29",
  "peRatio": 28.5,
  "beta": 1.25
}
```

**Mapping**: Same `fundamentals` table as Alpha Vantage, with `source = 'IEX_CLOUD'`

**Fallback Logic**: Same as prices fallback (primary Alpha Vantage, fallback IEX Cloud)

---

## Data Provider: FX Rates (ECB & Federal Reserve)

**Purpose**: Convert multi-currency holdings to user's base currency

### Contract 7: European Central Bank (ECB) FX Rates

**Base URL**: `https://data-api.ecb.europa.eu/service/data`
**Documentation**: https://data.ecb.europa.eu/help/api/overview
**Rate Limits**: No official limit, fair use policy

**Endpoint**: Exchange Rates (EXR)

**Request (Daily EUR rates)**:
```
GET https://data-api.ecb.europa.eu/service/data/EXR/D.USD+GBP+JPY+CAD+AUD.EUR.SP00.A
  ?format=jsondata
  &startPeriod=2025-10-01
  &endPeriod=2025-10-30
```

**Response Schema** (SDMX JSON):
```json
{
  "dataSets": [
    {
      "observations": {
        "0:0:0:0:0": [1.0850],  // EUR/USD
        "0:1:0:0:0": [0.8620],  // EUR/GBP
        "0:2:0:0:0": [162.45],  // EUR/JPY
        "0:3:0:0:0": [1.4820],  // EUR/CAD
        "0:4:0:0:0": [1.6350]   // EUR/AUD
      }
    }
  ],
  "structure": {
    "dimensions": {
      "observation": [
        {
          "id": "TIME_PERIOD",
          "values": [
            {"id": "2025-10-30"}
          ]
        },
        {
          "id": "CURRENCY",
          "values": [
            {"id": "USD"},
            {"id": "GBP"},
            {"id": "JPY"},
            {"id": "CAD"},
            {"id": "AUD"}
          ]
        }
      ]
    }
  }
}
```

**Mapping to Database**:
```sql
CREATE TABLE fx_rates (
  id BIGSERIAL PRIMARY KEY,
  date DATE NOT NULL,
  base_currency VARCHAR(3) NOT NULL,
  quote_currency VARCHAR(3) NOT NULL,
  rate DECIMAL(12,6) NOT NULL, -- Exchange rate (base/quote)
  source VARCHAR(50) DEFAULT 'ECB',
  ingested_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(date, base_currency, quote_currency, source)
);

-- Index for fast currency conversion lookups
CREATE INDEX idx_fx_rates_currencies_date ON fx_rates(base_currency, quote_currency, date DESC);
```

**Conversion Logic**:
- Store rates as EUR/XXX (e.g., EUR/USD = 1.0850)
- To convert USD → EUR: 1 / (EUR/USD)
- To convert USD → GBP: (EUR/GBP) / (EUR/USD)
- Cross-rates calculated via EUR pivot

**Update Frequency**:
- **Daily**: Update at 4:00 PM CET (after ECB publishes reference rates)
- **Staleness Threshold**: 48 hours (warning only, do not block recommendations)

---

### Contract 8: Federal Reserve FX Rates (USD Rates)

**Base URL**: `https://www.federalreserve.gov/datadownload`
**Documentation**: https://www.federalreserve.gov/releases/h10/default.htm
**Rate Limits**: No official limit

**Endpoint**: H.10 Foreign Exchange Rates

**Request** (CSV download):
```
GET https://www.federalreserve.gov/datadownload/Output.aspx
  ?rel=H10
  &series=60b32914e38a86178e3d95ff8d2ffff4  # USD/EUR
  &lastobs=30
  &from=10/01/2025
  &to=10/30/2025
  &filetype=csv
```

**Response Format**: CSV

**Example Response**:
```csv
Series Description,Unit,Multiplier,Currency,Time Period,Value
Euro,USD per EUR,1,EUR,2025-10-30,1.0850
```

**Usage**: Supplement ECB rates with Federal Reserve USD rates for redundancy

**Fallback Logic**:
- Primary: ECB for EUR-based rates
- Fallback: Federal Reserve for USD-based rates
- Cross-check: If both sources available, validate rates match within 0.1%

---

## Rate Limiting Strategy

### Alpha Vantage

**Free Tier**: 5 calls per minute, 500 calls per day
**Premium Tier**: 75 calls per minute, 150,000 calls per day

**Implementation**:
```java
@Component
public class AlphaVantageRateLimiter {
    private final RateLimiter perMinuteLimiter = RateLimiter.create(4.5); // 4.5 calls/min (buffer)
    private final AtomicInteger dailyCallCount = new AtomicInteger(0);
    private final int DAILY_LIMIT = 500;

    public void acquirePermit() throws RateLimitException {
        // Per-minute rate limiting
        if (!perMinuteLimiter.tryAcquire(1, TimeUnit.SECONDS)) {
            throw new RateLimitException("Alpha Vantage rate limit: 5 calls/min exceeded");
        }

        // Daily rate limiting
        if (dailyCallCount.incrementAndGet() > DAILY_LIMIT) {
            throw new RateLimitException("Alpha Vantage daily limit exceeded");
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Reset at midnight
    public void resetDailyCounter() {
        dailyCallCount.set(0);
    }
}
```

**Batch Optimization**:
- Batch symbol requests: Fetch multiple symbols in parallel (up to 5 concurrent requests)
- Prioritize active holdings: Fetch prices for portfolio holdings first, universe constituents second
- Schedule background jobs during off-peak hours (2-4 AM ET)

---

### IEX Cloud

**Free Tier**: 50,000 messages per month (1 message = 1 data point)
**Paid Tier**: 500,000+ messages per month

**Message Cost Examples**:
- `/stock/{symbol}/quote`: 1 message
- `/stock/{symbol}/chart/1m`: ~21 messages (21 trading days)
- `/stock/{symbol}/stats`: 1 message

**Implementation**:
```java
@Component
public class IEXCloudRateLimiter {
    private final AtomicInteger monthlyMessageCount = new AtomicInteger(0);
    private final int MONTHLY_LIMIT = 50000;

    public void trackMessages(int messageCount) throws RateLimitException {
        if (monthlyMessageCount.addAndGet(messageCount) > MONTHLY_LIMIT) {
            throw new RateLimitException("IEX Cloud monthly limit exceeded");
        }
    }

    @Scheduled(cron = "0 0 0 1 * ?") // Reset on 1st of month
    public void resetMonthlyCounter() {
        monthlyMessageCount.set(0);
    }
}
```

---

## Error Handling and Retry Logic

### HTTP Error Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Process response |
| 400 | Bad Request | Log error, do not retry (invalid symbol/parameter) |
| 401 | Unauthorized | API key invalid/expired, alert admin, do not retry |
| 403 | Forbidden | API key suspended or rate limit, switch to fallback provider |
| 404 | Not Found | Symbol not supported by provider, mark as unavailable |
| 429 | Too Many Requests | Wait for rate limit reset (check `Retry-After` header), retry after cooldown |
| 500 | Internal Server Error | Retry with exponential backoff (3 attempts max) |
| 503 | Service Unavailable | Provider maintenance, retry after 5 minutes (3 attempts max) |

---

### Retry Strategy

**Exponential Backoff**:
```java
@Component
public class DataFeedRetryHandler {
    private static final int MAX_RETRIES = 3;
    private static final int BASE_DELAY_MS = 1000;

    public <T> T executeWithRetry(Supplier<T> apiCall, String dataSource) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                return apiCall.get();
            } catch (HttpServerErrorException | HttpServiceUnavailableException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw new DataFeedException("Max retries exceeded for " + dataSource, e);
                }
                int delayMs = BASE_DELAY_MS * (int) Math.pow(2, attempt); // 1s, 2s, 4s
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new DataFeedException("Retry interrupted", ie);
                }
            } catch (HttpClientErrorException e) {
                // Don't retry 4xx errors (invalid request)
                throw new DataFeedException("Client error for " + dataSource, e);
            }
        }
        throw new DataFeedException("Failed to fetch data from " + dataSource);
    }
}
```

---

### Circuit Breaker Pattern

**Purpose**: Prevent cascading failures by stopping requests to failing provider

**Implementation**:
```java
@Component
public class DataFeedCircuitBreaker {
    private final Map<String, CircuitBreakerState> states = new ConcurrentHashMap<>();
    private static final int FAILURE_THRESHOLD = 5;
    private static final int RESET_TIMEOUT_MS = 60000; // 1 minute

    public boolean isOpen(String dataSource) {
        CircuitBreakerState state = states.get(dataSource);
        if (state == null) return false;

        if (state.state == State.OPEN) {
            // Check if timeout has passed
            if (System.currentTimeMillis() - state.lastFailureTime > RESET_TIMEOUT_MS) {
                state.state = State.HALF_OPEN;
                return false; // Allow one test request
            }
            return true; // Circuit still open
        }
        return false;
    }

    public void recordSuccess(String dataSource) {
        CircuitBreakerState state = states.computeIfAbsent(dataSource, k -> new CircuitBreakerState());
        state.failureCount = 0;
        state.state = State.CLOSED;
    }

    public void recordFailure(String dataSource) {
        CircuitBreakerState state = states.computeIfAbsent(dataSource, k -> new CircuitBreakerState());
        state.failureCount++;
        state.lastFailureTime = System.currentTimeMillis();

        if (state.failureCount >= FAILURE_THRESHOLD) {
            state.state = State.OPEN;
            logger.warn("Circuit breaker OPEN for {}", dataSource);
        }
    }

    enum State { CLOSED, OPEN, HALF_OPEN }

    static class CircuitBreakerState {
        State state = State.CLOSED;
        int failureCount = 0;
        long lastFailureTime = 0;
    }
}
```

---

## Caching Strategy

### Redis Cache Layer

**Purpose**: Reduce API calls and improve response times

**Cache Keys**:
```
prices:{symbol}:{date}           → Price data for symbol on date (TTL: 7 days)
fundamentals:{symbol}:{quarter}  → Fundamental data for symbol/quarter (TTL: 90 days)
estimates:{symbol}:{date}        → Analyst estimates snapshot (TTL: 7 days)
calendar:upcoming:{days}         → Upcoming calendar events (TTL: 1 day)
fx_rates:{base}:{quote}:{date}   → FX rate for currency pair (TTL: 7 days)
```

**Implementation**:
```java
@Service
public class DataFeedCacheService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Optional<PriceData> getCachedPrice(String symbol, LocalDate date) {
        String key = String.format("prices:%s:%s", symbol, date);
        PriceData data = (PriceData) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(data);
    }

    public void cachePrice(String symbol, LocalDate date, PriceData data) {
        String key = String.format("prices:%s:%s", symbol, date);
        redisTemplate.opsForValue().set(key, data, Duration.ofDays(7));
    }

    public void invalidatePrice(String symbol, LocalDate date) {
        String key = String.format("prices:%s:%s", symbol, date);
        redisTemplate.delete(key);
    }
}
```

**Cache Invalidation**:
- Prices: Invalidate after market close daily
- Fundamentals: Invalidate when new filing detected
- Estimates: Invalidate when revisions detected
- Calendar: Invalidate daily at midnight
- FX Rates: Invalidate daily at 5 PM CET

---

### Freshness Rules

| Data Type | Refresh Interval | Staleness Threshold | Action if Stale |
|-----------|------------------|---------------------|-----------------|
| Prices | Daily (5:30 PM ET) | 24 hours | Block recommendations |
| Fundamentals | Weekly / Daily in earnings season | 120 days | Block recommendations |
| Estimates | Daily in earnings season / Weekly off-season | 30 days | Block recommendations |
| Calendar | Daily | 7 days | Block recommendations |
| FX Rates | Daily (4 PM CET) | 48 hours | Warning only (use last known rate) |

**Implementation**:
```java
@Service
public class DataFreshnessService {
    public boolean isDataFresh(String dataSource) {
        DataSourceHealth health = getDataSourceHealth(dataSource);
        Duration staleness = Duration.between(health.getLastUpdateTime(), Instant.now());
        return staleness.compareTo(health.getStalenessThreshold()) < 0;
    }

    public List<String> getStaleSources() {
        return Arrays.stream(DataSourceType.values())
            .filter(source -> !isDataFresh(source.name()))
            .map(DataSourceType::name)
            .collect(Collectors.toList());
    }

    public boolean canRunRecommendations() {
        List<String> staleSources = getStaleSources();
        // Block if any critical source is stale (except FX rates)
        return staleSources.stream()
            .noneMatch(source -> !source.equals("FX_RATES"));
    }
}
```

---

## Health Check Monitoring

### Health Check Endpoints

Exposed via REST API (`GET /api/data-sources` and `/api/data-sources/{id}/health`)

**Health Status Calculation**:
```java
public enum HealthStatus {
    CURRENT,   // Data within freshness threshold, no recent errors
    STALE,     // Data exceeds staleness threshold
    ERROR      // Recent errors (>10% error rate in last hour)
}

@Service
public class DataSourceHealthService {
    public DataSourceHealth calculateHealth(String dataSource) {
        Instant lastUpdate = getLastSuccessfulUpdate(dataSource);
        Duration staleness = Duration.between(lastUpdate, Instant.now());
        double errorRate = getErrorRate(dataSource, Duration.ofHours(1));

        HealthStatus status;
        if (errorRate > 0.1) {
            status = HealthStatus.ERROR;
        } else if (staleness.compareTo(getStalenessThreshold(dataSource)) > 0) {
            status = HealthStatus.STALE;
        } else {
            status = HealthStatus.CURRENT;
        }

        return new DataSourceHealth(dataSource, status, lastUpdate, errorRate);
    }
}
```

---

### Monitoring Metrics

**Prometheus Metrics**:
```
# Counter: Total API calls by provider and status
data_feed_api_calls_total{provider="alpha_vantage", status="success"} 1234
data_feed_api_calls_total{provider="alpha_vantage", status="error"} 5
data_feed_api_calls_total{provider="iex_cloud", status="success"} 67

# Gauge: Current rate limit usage
data_feed_rate_limit_usage{provider="alpha_vantage", period="daily"} 432
data_feed_rate_limit_usage{provider="iex_cloud", period="monthly"} 12450

# Histogram: API response time
data_feed_api_duration_seconds{provider="alpha_vantage", endpoint="daily_prices"} 0.345

# Gauge: Data staleness in hours
data_feed_staleness_hours{data_source="prices"} 2.5
data_feed_staleness_hours{data_source="fundamentals"} 168.0

# Counter: Cache hit/miss rates
data_feed_cache_requests_total{cache="prices", result="hit"} 5678
data_feed_cache_requests_total{cache="prices", result="miss"} 234
```

**Alerting Thresholds**:
- Alert if any data source staleness > threshold
- Alert if error rate > 10% for 5 consecutive minutes
- Alert if daily rate limit > 90% of quota
- Alert if circuit breaker opens for any provider

---

## Data Validation Schemas

### JSON Schema for Prices

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Price Data",
  "type": "object",
  "required": ["symbol", "date", "open", "high", "low", "close", "volume"],
  "properties": {
    "symbol": {
      "type": "string",
      "pattern": "^[A-Z]{1,5}$"
    },
    "date": {
      "type": "string",
      "format": "date"
    },
    "open": {
      "type": "number",
      "minimum": 0.01
    },
    "high": {
      "type": "number",
      "minimum": 0.01
    },
    "low": {
      "type": "number",
      "minimum": 0.01
    },
    "close": {
      "type": "number",
      "minimum": 0.01
    },
    "volume": {
      "type": "integer",
      "minimum": 0
    }
  },
  "allOf": [
    {
      "properties": {
        "high": {
          "minimum": {"$data": "1/low"}
        }
      }
    }
  ]
}
```

**Custom Validation**:
- `close` must be within [low, high]
- Price change from previous day must be < 50%
- Volume must be > 0 for trading day

---

### JSON Schema for Fundamentals

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Fundamental Data",
  "type": "object",
  "required": ["symbol", "fiscal_period", "fiscal_year", "total_revenue"],
  "properties": {
    "symbol": {
      "type": "string",
      "pattern": "^[A-Z]{1,5}$"
    },
    "fiscal_period": {
      "type": "string",
      "enum": ["Q1", "Q2", "Q3", "Q4", "FY"]
    },
    "fiscal_year": {
      "type": "integer",
      "minimum": 2000
    },
    "total_revenue": {
      "type": "number"
    },
    "gross_margin": {
      "type": "number",
      "minimum": -0.5,
      "maximum": 1.0
    },
    "roe": {
      "type": "number",
      "minimum": -2.0,
      "maximum": 5.0
    }
  }
}
```

---

## Testing and Mocking

### Mock Data for Development

```java
@Configuration
@Profile("dev")
public class MockDataFeedConfig {
    @Bean
    public DataFeedService mockDataFeedService() {
        return new MockDataFeedService();
    }
}

public class MockDataFeedService implements DataFeedService {
    @Override
    public PriceData fetchPrice(String symbol, LocalDate date) {
        // Return deterministic mock data
        return PriceData.builder()
            .symbol(symbol)
            .date(date)
            .open(100.0)
            .high(105.0)
            .low(98.0)
            .close(103.5)
            .volume(10000000L)
            .build();
    }
}
```

---

### Contract Tests

Use WireMock to test external API contracts:

```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
public class AlphaVantageContractTest {
    @Autowired
    private AlphaVantageClient alphaVantageClient;

    @Test
    public void testDailyPricesContract() {
        stubFor(get(urlPathEqualTo("/query"))
            .withQueryParam("function", equalTo("TIME_SERIES_DAILY"))
            .withQueryParam("symbol", equalTo("AAPL"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("alpha_vantage_daily_prices.json")));

        PriceData priceData = alphaVantageClient.fetchDailyPrice("AAPL", LocalDate.now());

        assertNotNull(priceData);
        assertEquals("AAPL", priceData.getSymbol());
        assertTrue(priceData.getClose() > 0);
    }
}
```

---

## Related Documentation

- REST API Specification: `rest-api.yaml`
- WebSocket Protocol: `websocket.md`
- Data Model Design: `../data-model.md` (to be created in Phase 1)
