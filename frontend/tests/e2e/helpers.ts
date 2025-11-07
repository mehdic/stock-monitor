import { Page, expect } from '@playwright/test';

/**
 * E2E Test Helpers
 *
 * Reusable utilities for authentication, data setup, and cleanup
 */

// Test user credentials
export const TEST_USER = {
  email: 'e2e-test@example.com',
  password: 'TestPassword123!',
  firstName: 'E2E',
  lastName: 'TestUser',
};

/**
 * Authenticate a test user
 * Handles login flow and waits for successful authentication
 */
export async function login(page: Page, email = TEST_USER.email, password = TEST_USER.password) {
  await page.goto('/login');

  const emailInput = page.locator('input[type="email"], input[name="email"]');
  const passwordInput = page.locator('input[type="password"], input[name="password"]');
  const submitButton = page.locator('button[type="submit"]');

  await emailInput.fill(email);
  await passwordInput.fill(password);
  await submitButton.click();

  // Wait for successful login (redirect to dashboard)
  await page.waitForURL(/\/dashboard/, { timeout: 10000 }).catch(() => {
    console.log('Dashboard redirect timed out');
  });

  // Verify authentication token exists
  const hasToken = await page.evaluate(() => {
    return !!localStorage.getItem('authToken') || !!sessionStorage.getItem('authToken');
  });

  if (!hasToken) {
    console.warn('No auth token found after login');
  }

  return hasToken;
}

/**
 * Register a new test user
 */
export async function register(page: Page, userData = TEST_USER) {
  await page.goto('/register');

  await page.locator('input[name="firstName"], input[placeholder*="First"]').fill(userData.firstName);
  await page.locator('input[name="lastName"], input[placeholder*="Last"]').fill(userData.lastName);
  await page.locator('input[type="email"], input[name="email"]').fill(userData.email);
  await page.locator('input[type="password"], input[name="password"]').first().fill(userData.password);

  // Confirm password if field exists
  const confirmPasswordField = page.locator('input[name="confirmPassword"], input[placeholder*="Confirm"]');
  if (await confirmPasswordField.isVisible({ timeout: 1000 }).catch(() => false)) {
    await confirmPasswordField.fill(userData.password);
  }

  await page.locator('button[type="submit"]').click();

  // Wait for registration success (might redirect to login or dashboard)
  await page.waitForURL(/\/(login|dashboard|verify-email)/, { timeout: 10000 });
}

/**
 * Logout current user
 */
export async function logout(page: Page) {
  // Look for logout button/link
  const logoutButton = page.locator('button:has-text("Logout"), button:has-text("Sign Out"), a:has-text("Logout")');

  if (await logoutButton.isVisible({ timeout: 2000 }).catch(() => false)) {
    await logoutButton.click();
    await page.waitForURL(/\/login/, { timeout: 5000 });
  } else {
    // Try navigating to logout endpoint
    await page.goto('/logout');
    await page.waitForURL(/\/login/, { timeout: 5000 });
  }

  // Clear auth tokens
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
}

/**
 * Upload a CSV file to portfolio
 */
export async function uploadPortfolioCSV(page: Page, csvContent: string, filename = 'test-portfolio.csv') {
  await page.goto('/portfolio');

  // Create a CSV file blob and set it on file input
  const fileInput = page.locator('input[type="file"]');

  // Create a temporary file for upload
  await fileInput.setInputFiles({
    name: filename,
    mimeType: 'text/csv',
    buffer: Buffer.from(csvContent),
  });

  // Wait for upload to complete (look for success message or table update)
  await page.waitForTimeout(2000);
}

/**
 * Generate sample CSV portfolio data
 */
export function generateSamplePortfolioCSV(): string {
  return `Symbol,Shares,Purchase Price,Purchase Date
AAPL,100,150.00,2023-01-15
GOOGL,50,120.00,2023-02-20
MSFT,75,300.00,2023-03-10
AMZN,30,140.00,2023-04-05
TSLA,40,200.00,2023-05-12`;
}

/**
 * Wait for API response
 */
export async function waitForAPIResponse(page: Page, urlPattern: string | RegExp, timeout = 10000) {
  return page.waitForResponse(
    response => {
      const url = response.url();
      const matches = typeof urlPattern === 'string'
        ? url.includes(urlPattern)
        : urlPattern.test(url);
      return matches && response.status() === 200;
    },
    { timeout }
  );
}

/**
 * Check for console errors
 */
export function setupConsoleErrorTracking(page: Page): string[] {
  const errors: string[] = [];

  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      errors.push(msg.text());
    }
  });

  return errors;
}

/**
 * Wait for element to be visible and enabled
 */
export async function waitForInteractiveElement(page: Page, selector: string, timeout = 10000) {
  const element = page.locator(selector);
  await expect(element).toBeVisible({ timeout });
  await expect(element).toBeEnabled({ timeout });
  return element;
}

/**
 * Navigate to a page and wait for it to be ready
 */
export async function navigateAndWaitForReady(page: Page, path: string) {
  await page.goto(path);
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(500); // Small buffer for JS initialization
}

/**
 * Clean up test data (call after tests)
 */
export async function cleanupTestData(page: Page) {
  // Navigate to settings or admin page to delete test data
  // This would need to be implemented based on backend capabilities
  console.log('Cleanup: Test data removal would happen here');
}

/**
 * Mock API response
 */
export async function mockAPIResponse(page: Page, urlPattern: string | RegExp, responseData: any) {
  await page.route(urlPattern, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(responseData),
    });
  });
}

/**
 * Verify page has no accessibility violations (basic check)
 */
export async function checkBasicAccessibility(page: Page) {
  // Check for basic accessibility requirements
  const hasMainLandmark = await page.locator('main, [role="main"]').count() > 0;
  const hasHeading = await page.locator('h1, h2').count() > 0;
  const allImagesHaveAlt = await page.locator('img:not([alt])').count() === 0;

  return {
    hasMainLandmark,
    hasHeading,
    allImagesHaveAlt,
    isAccessible: hasMainLandmark && hasHeading,
  };
}

/**
 * Take screenshot with consistent naming
 */
export async function takeDebugScreenshot(page: Page, name: string) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  await page.screenshot({ path: `test-results/screenshot-${name}-${timestamp}.png`, fullPage: true });
}

/**
 * Wait for WebSocket connection
 */
export async function waitForWebSocketConnection(page: Page, timeout = 10000) {
  return page.waitForEvent('websocket', { timeout });
}

/**
 * Verify table has data
 */
export async function verifyTableHasData(page: Page, tableSelector: string, minRows = 1) {
  const table = page.locator(tableSelector);
  await expect(table).toBeVisible();

  const rows = table.locator('tbody tr, tr[data-row]');
  const rowCount = await rows.count();

  expect(rowCount).toBeGreaterThanOrEqual(minRows);
  return rowCount;
}

/**
 * Fill form fields by labels
 */
export async function fillFormByLabels(page: Page, fields: Record<string, string>) {
  for (const [label, value] of Object.entries(fields)) {
    const field = page.locator(`input[name="${label}"], textarea[name="${label}"]`);
    if (await field.isVisible({ timeout: 1000 }).catch(() => false)) {
      await field.fill(value);
    } else {
      // Try to find by label text
      const labelElement = page.locator(`label:has-text("${label}")`);
      if (await labelElement.isVisible({ timeout: 1000 }).catch(() => false)) {
        const fieldId = await labelElement.getAttribute('for');
        if (fieldId) {
          await page.locator(`#${fieldId}`).fill(value);
        }
      }
    }
  }
}
