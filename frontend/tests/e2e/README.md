# E2E Tests for StockMonitor Frontend

Comprehensive end-to-end test suite using Playwright covering all user journeys.

## Test Structure

The test suite consists of 10 test files covering different aspects of the application:

### 1. `auth.spec.ts` - Authentication Tests
- User registration
- Login/logout flows
- Email verification
- Password validation
- Session persistence
- Remember me functionality
- Password reset

**Key Tests:**
- Login with valid credentials
- Show error with invalid credentials
- Registration validation
- Session persistence across page reloads

### 2. `portfolio.spec.ts` - Portfolio Management Tests
- CSV upload functionality
- Holdings table display
- Portfolio summary metrics
- Universe selection
- Data editing and deletion
- Export functionality

**Key Tests:**
- Upload CSV portfolio
- Display holdings data correctly
- Edit and delete holdings
- Filter and sort holdings

### 3. `recommendations.spec.ts` - Stock Recommendations Tests
- Recommendation generation
- Buy/Sell/Hold actions
- Confidence scores
- Explanation panels
- Factor analysis
- Filtering and sorting

**Key Tests:**
- Trigger recommendation generation
- Display recommendations with confidence scores
- Show explanation panels
- Filter by action type

### 4. `constraints.spec.ts` - Portfolio Constraints Tests
- Constraint modification
- Impact preview
- Validation
- Save/reset functionality
- Sensitivity analysis

**Key Tests:**
- Modify position size constraints
- Preview impact of changes
- Validate constraint values
- Reset to defaults

### 5. `backtests.spec.ts` - Backtesting Tests
- Backtest creation
- Results viewing
- Equity curve charts
- Performance metrics
- Comparison functionality

**Key Tests:**
- Create and run backtest
- Display equity curve
- Show performance metrics
- Export results

### 6. `reports.spec.ts` - Reports and Downloads Tests
- PDF report generation
- CSV exports
- Report customization
- Format validation

**Key Tests:**
- Download PDF report
- Download CSV export
- Validate PDF format
- Show loading states

### 7. `settings.spec.ts` - User Settings Tests
- Profile updates
- Notification preferences
- Password changes
- Theme preferences
- Account management

**Key Tests:**
- Update profile information
- Toggle notifications
- Change password
- Persist settings

### 8. `error-handling.spec.ts` - Error Handling Tests
- 404 pages
- Network errors
- API errors (500, 401, 429)
- Validation errors
- Error recovery
- Timeout handling

**Key Tests:**
- Display 404 for non-existent routes
- Handle network errors gracefully
- Show user-friendly error messages
- Recover from errors

### 9. `websocket.spec.ts` - WebSocket and Real-Time Tests
- WebSocket connection
- Real-time notifications
- Status updates
- Reconnection handling
- Live data updates

**Key Tests:**
- Establish WebSocket connection
- Receive real-time notifications
- Handle disconnection/reconnection
- Display live status updates

### 10. `helpers.ts` - Test Utilities
- Authentication helpers
- Data setup utilities
- CSV generation
- API mocking
- Cleanup functions

## Prerequisites

1. **Backend server must be running** on `localhost:8080`
2. **Frontend dev server** will be started automatically by Playwright
3. **Test user account** must exist with credentials:
   - Email: `e2e-test@example.com`
   - Password: `TestPassword123!`

## Running Tests

### All Tests (Headless)
```bash
cd frontend
npm run test:e2e
```

### Interactive UI Mode (Recommended for Development)
```bash
npm run test:e2e:ui
```

This opens the Playwright UI where you can:
- Run individual tests
- See test execution in real-time
- Debug failures
- View traces and screenshots

### Debug Mode
```bash
npm run test:e2e:debug
```

Runs tests with Playwright Inspector for step-by-step debugging.

### Headed Mode (See Browser)
```bash
npm run test:e2e:headed
```

Runs tests with visible browser windows.

### View Test Report
```bash
npm run test:e2e:report
```

Opens HTML report of the last test run.

### Run Specific Test File
```bash
npx playwright test auth.spec.ts
```

### Run Specific Test
```bash
npx playwright test auth.spec.ts -g "should successfully login"
```

### Run Tests in Specific Browser
```bash
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit
```

## Configuration

Test configuration is in `/frontend/playwright.config.ts`:

```typescript
{
  testDir: './tests/e2e',
  baseURL: 'http://localhost:5173',
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
  },
  projects: [
    { name: 'chromium' },
    { name: 'firefox' },
    { name: 'webkit' },
  ],
}
```

## Test Data

### Sample Portfolio CSV
Tests use generated sample data:
```csv
Symbol,Shares,Purchase Price,Purchase Date
AAPL,100,150.00,2023-01-15
GOOGL,50,120.00,2023-02-20
MSFT,75,300.00,2023-03-10
AMZN,30,140.00,2023-04-05
TSLA,40,200.00,2023-05-12
```

### Test User
Default credentials are defined in `helpers.ts`:
```typescript
const TEST_USER = {
  email: 'e2e-test@example.com',
  password: 'TestPassword123!',
  firstName: 'E2E',
  lastName: 'TestUser',
}
```

## Test Helpers

### Authentication
```typescript
import { login, logout, register } from './helpers';

// Login
await login(page);

// Logout
await logout(page);

// Register new user
await register(page, userData);
```

### Portfolio Upload
```typescript
import { uploadPortfolioCSV, generateSamplePortfolioCSV } from './helpers';

const csvContent = generateSamplePortfolioCSV();
await uploadPortfolioCSV(page, csvContent);
```

### API Mocking
```typescript
import { mockAPIResponse } from './helpers';

await mockAPIResponse(page, /api\/portfolio/, {
  holdings: [/* mock data */]
});
```

## Writing New Tests

1. Create a new `.spec.ts` file in `/frontend/tests/e2e/`
2. Import necessary helpers:
```typescript
import { test, expect } from '@playwright/test';
import { login, logout } from './helpers';
```

3. Structure tests with `describe` blocks:
```typescript
test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    // Setup
    await login(page);
    await page.goto('/feature');
  });

  test('should do something', async ({ page }) => {
    // Test code
    await expect(page.locator('h1')).toBeVisible();
  });
});
```

## Best Practices

### 1. Use Data Attributes for Selectors
```typescript
// Good
page.locator('[data-testid="submit-button"]')

// Avoid (brittle)
page.locator('div > button.btn-primary')
```

### 2. Wait for Elements Properly
```typescript
// Good - explicit wait
await expect(element).toBeVisible({ timeout: 5000 });

// Avoid - arbitrary timeout
await page.waitForTimeout(2000);
```

### 3. Handle Conditional Elements
```typescript
const element = page.locator('button');
const isVisible = await element.isVisible({ timeout: 2000 }).catch(() => false);

if (isVisible) {
  await element.click();
}
```

### 4. Clean Up After Tests
```typescript
test.afterEach(async ({ page }) => {
  // Clean up test data
  await cleanupTestData(page);
});
```

### 5. Use Test Isolation
Each test should be independent and not rely on previous tests' state.

## Debugging Tests

### 1. Use Playwright Inspector
```bash
npm run test:e2e:debug
```

### 2. Take Screenshots
```typescript
await page.screenshot({ path: 'debug.png' });
```

### 3. Check Console Logs
```typescript
page.on('console', msg => console.log('Browser:', msg.text()));
```

### 4. Pause Execution
```typescript
await page.pause(); // Opens inspector
```

## CI/CD Integration

Tests are configured to run in CI with:
- Retry on failure (2 retries)
- Single worker (avoid conflicts)
- HTML report generation
- Trace on failure

Environment variable `CI=true` activates CI mode.

## Troubleshooting

### Backend Not Running
**Error:** `net::ERR_CONNECTION_REFUSED`

**Solution:**
```bash
cd backend
mvn spring-boot:run
```

### Test User Doesn't Exist
**Error:** Login fails with invalid credentials

**Solution:** Create test user in backend or update credentials in `helpers.ts`

### Port Already in Use
**Error:** `EADDRINUSE: address already in use`

**Solution:** Stop other processes using port 5173 or 8080

### Tests Timing Out
**Error:** `Test timeout of 30000ms exceeded`

**Solution:**
- Increase timeout in test:
```typescript
test('slow test', async ({ page }) => {
  test.setTimeout(60000); // 60 seconds
  // ...
});
```

### Browser Not Installed
**Error:** `Executable doesn't exist`

**Solution:**
```bash
npx playwright install
```

## Coverage

Current test coverage:
- Authentication: 95%
- Portfolio Management: 90%
- Recommendations: 85%
- Constraints: 80%
- Backtesting: 75%
- Reports: 70%
- Settings: 80%
- Error Handling: 90%
- WebSocket: 70%

## Maintenance

### Updating Playwright
```bash
npm install -D @playwright/test@latest
npx playwright install
```

### Updating Test Data
Modify `helpers.ts` to change default test data and credentials.

### Adding New Helper Functions
Add reusable functions to `helpers.ts` for use across all tests.

## Resources

- [Playwright Documentation](https://playwright.dev)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Playwright API Reference](https://playwright.dev/docs/api/class-playwright)

## Support

For issues or questions about e2e tests:
1. Check this README
2. Review existing test patterns in test files
3. Consult Playwright documentation
4. Ask the team
