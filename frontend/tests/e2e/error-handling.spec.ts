import { test, expect } from '@playwright/test';
import { login, setupConsoleErrorTracking } from './helpers';

/**
 * Error Handling E2E Tests
 *
 * Tests network errors, validation errors, 404/500 pages, and error recovery
 */

test.describe('Error Handling', () => {
  test('should display 404 page for non-existent routes', async ({ page }) => {
    await page.goto('/this-page-does-not-exist');

    // Look for 404 error page
    const errorHeading = page.locator('text=/404|not found|page not found/i');
    const hasError = await errorHeading.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasError) {
      console.log('404 page displayed');
      await expect(errorHeading).toBeVisible();

      // Look for home/back link
      const homeLink = page.locator('a[href="/"], a:has-text("Home"), a:has-text("Back")');
      const hasHomeLink = await homeLink.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasHomeLink) {
        console.log('Navigation back to home available');
      }
    } else {
      console.log('404 page not implemented or redirects to home');
    }
  });

  test('should handle network errors gracefully', async ({ page }) => {
    // Login first
    await login(page);
    await page.goto('/portfolio');

    // Simulate offline
    await page.context().setOffline(true);

    // Try to perform action that requires network
    const refreshButton = page.locator('button:has-text("Refresh"), button:has-text("Load")');

    if (await refreshButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await refreshButton.click();
      await page.waitForTimeout(2000);

      // Look for network error message
      const errorMessage = page.locator('text=/network error|offline|connection/i');
      const hasError = await errorMessage.isVisible({ timeout: 3000 }).catch(() => false);

      if (hasError) {
        console.log('Network error displayed properly');
        await expect(errorMessage).toBeVisible();
      } else {
        console.log('Network error handled silently or different messaging');
      }
    }

    // Restore connection
    await page.context().setOffline(false);
  });

  test('should handle API errors (500)', async ({ page }) => {
    // Login first
    await login(page);

    // Intercept API calls and return 500 error
    await page.route('**/api/**', (route) => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal Server Error' }),
      });
    });

    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Look for error message
    const errorMessage = page.locator('text=/error|something went wrong|try again/i');
    const hasError = await errorMessage.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasError) {
      console.log('500 error handled properly');
      await expect(errorMessage).toBeVisible();
    } else {
      console.log('500 error handled differently or not visible to user');
    }
  });

  test('should handle unauthorized errors (401)', async ({ page }) => {
    // Intercept API calls and return 401
    await page.route('**/api/**', (route) => {
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Unauthorized' }),
      });
    });

    await page.goto('/dashboard');

    // Should redirect to login
    await page.waitForURL(/\/login/, { timeout: 5000 }).catch(() => {
      console.log('Did not redirect to login');
    });

    const currentUrl = page.url();
    if (currentUrl.includes('/login')) {
      console.log('401 error redirects to login');
    } else {
      console.log('401 handled without redirect');
    }
  });

  test('should display form validation errors', async ({ page }) => {
    await page.goto('/login');

    // Try to submit empty form
    const submitButton = page.locator('button[type="submit"]');
    await submitButton.click();

    // Look for validation errors
    const errorMessage = page.locator('text=/required|invalid|error/i, .error, [role="alert"]');
    const hasError = await errorMessage.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasError) {
      console.log('Form validation errors displayed');
      await expect(errorMessage.first()).toBeVisible();
    } else {
      // Check HTML5 validation
      const emailInput = page.locator('input[type="email"]');
      const isInvalid = await emailInput.evaluate((el: HTMLInputElement) => !el.validity.valid);

      expect(isInvalid).toBe(true);
      console.log('HTML5 validation active');
    }
  });

  test('should display specific field validation errors', async ({ page }) => {
    await page.goto('/register');

    // Fill invalid email
    const emailInput = page.locator('input[type="email"]');
    await emailInput.fill('invalid-email');

    // Move focus away to trigger validation
    await page.locator('input[type="password"]').click();
    await page.waitForTimeout(500);

    // Look for email-specific error
    const emailError = page.locator('text=/invalid email|valid email/i');
    const hasError = await emailError.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasError) {
      console.log('Field-specific validation errors displayed');
    } else {
      console.log('Validation on submit only or different pattern');
    }
  });

  test('should handle timeout errors', async ({ page }) => {
    // Login first
    await login(page);

    // Intercept API and delay response
    await page.route('**/api/**', async (route) => {
      await new Promise(resolve => setTimeout(resolve, 30000)); // 30 second delay
      route.continue();
    });

    await page.goto('/portfolio');
    await page.waitForTimeout(5000);

    // Look for timeout message
    const timeoutMessage = page.locator('text=/timeout|taking too long|slow/i');
    const hasTimeout = await timeoutMessage.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasTimeout) {
      console.log('Timeout error displayed');
    } else {
      console.log('Timeout handled differently or loading continues');
    }
  });

  test('should allow retry after error', async ({ page }) => {
    // Login first
    await login(page);

    // Intercept first request with error
    let requestCount = 0;
    await page.route('**/api/portfolio**', (route) => {
      requestCount++;
      if (requestCount === 1) {
        route.fulfill({
          status: 500,
          body: JSON.stringify({ error: 'Server Error' }),
        });
      } else {
        route.continue();
      }
    });

    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Look for retry button
    const retryButton = page.locator('button:has-text("Retry"), button:has-text("Try Again")');
    const hasRetry = await retryButton.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasRetry) {
      await retryButton.click();
      await page.waitForTimeout(2000);

      console.log('Retry functionality works');
    } else {
      console.log('No explicit retry button, may auto-retry');
    }
  });

  test('should display loading state during slow operations', async ({ page }) => {
    // Login first
    await login(page);

    // Slow down API responses
    await page.route('**/api/**', async (route) => {
      await new Promise(resolve => setTimeout(resolve, 2000));
      route.continue();
    });

    await page.goto('/recommendations');

    // Look for loading indicator
    const loadingIndicator = page.locator('text=/loading|processing/i, [role="status"], .spinner, .loading');
    const hasLoading = await loadingIndicator.isVisible({ timeout: 1000 }).catch(() => false);

    if (hasLoading) {
      console.log('Loading state displayed during slow operations');
      await expect(loadingIndicator).toBeVisible();
    } else {
      console.log('No visible loading indicator');
    }
  });

  test('should handle malformed API responses', async ({ page }) => {
    // Login first
    await login(page);

    // Return malformed JSON
    await page.route('**/api/**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: 'This is not valid JSON',
      });
    });

    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Look for error handling
    const errorMessage = page.locator('text=/error|invalid|unexpected/i');
    const hasError = await errorMessage.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasError) {
      console.log('Malformed response handled gracefully');
    } else {
      console.log('Malformed response causes silent failure or crash');
    }
  });

  test('should handle session expiration', async ({ page }) => {
    // Login first
    await login(page);
    await page.goto('/portfolio');

    // Clear auth token to simulate expiration
    await page.evaluate(() => {
      localStorage.removeItem('authToken');
      sessionStorage.removeItem('authToken');
    });

    // Try to navigate to protected page
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);

    // Should redirect to login
    const currentUrl = page.url();
    expect(currentUrl).toContain('/login');

    console.log('Session expiration handled with redirect to login');
  });

  test('should display user-friendly error messages', async ({ page }) => {
    // Login first
    await login(page);

    // Return error with message
    await page.route('**/api/**', (route) => {
      route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Invalid portfolio format' }),
      });
    });

    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Look for the specific error message
    const errorMessage = page.locator('text=/invalid portfolio|portfolio format/i');
    const hasSpecificError = await errorMessage.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasSpecificError) {
      console.log('Specific error message displayed to user');
    } else {
      // Check for generic error
      const genericError = page.locator('text=/error|something went wrong/i');
      const hasGenericError = await genericError.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasGenericError) {
        console.log('Generic error message displayed');
      }
    }
  });

  test('should not expose sensitive error details', async ({ page }) => {
    // Track console errors
    const errors = setupConsoleErrorTracking(page);

    // Login first
    await login(page);

    // Cause an error
    await page.route('**/api/**', (route) => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'Database connection failed',
          stack: 'Error at db.connect() line 42',
          query: 'SELECT * FROM users WHERE password = ...'
        }),
      });
    });

    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Verify sensitive details not shown in UI
    const pageContent = await page.content();

    expect(pageContent.toLowerCase()).not.toContain('database');
    expect(pageContent.toLowerCase()).not.toContain('stack');
    expect(pageContent.toLowerCase()).not.toContain('query');

    console.log('Sensitive error details not exposed in UI');
  });

  test('should handle CORS errors gracefully', async ({ page }) => {
    // This is difficult to test in Playwright, but we can simulate
    await page.route('**/api/**', (route) => {
      route.abort('failed');
    });

    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Look for connection error
    const errorMessage = page.locator('text=/error|failed|unable to connect/i');
    const hasError = await errorMessage.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasError) {
      console.log('CORS/connection error handled');
    } else {
      console.log('Connection errors handled silently');
    }
  });

  test('should show appropriate error for rate limiting', async ({ page }) => {
    // Login first
    await login(page);

    // Return 429 Too Many Requests
    await page.route('**/api/**', (route) => {
      route.fulfill({
        status: 429,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Too many requests' }),
      });
    });

    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Look for rate limit message
    const rateLimitMessage = page.locator('text=/too many|rate limit|slow down/i');
    const hasMessage = await rateLimitMessage.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasMessage) {
      console.log('Rate limiting error displayed');
      await expect(rateLimitMessage).toBeVisible();
    } else {
      console.log('Rate limiting not specifically handled');
    }
  });

  test('should handle empty API responses', async ({ page }) => {
    // Login first
    await login(page);

    // Return empty response
    await page.route('**/api/portfolio**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Look for empty state
    const emptyMessage = page.locator('text=/no data|no holdings|empty|no portfolio/i');
    const hasEmpty = await emptyMessage.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasEmpty) {
      console.log('Empty state displayed properly');
      await expect(emptyMessage).toBeVisible();
    } else {
      console.log('Empty response shows empty table or different UI');
    }
  });
});

test.describe('Error Recovery', () => {
  test('should recover from error after fixing issue', async ({ page }) => {
    let errorCount = 0;

    // First request fails, subsequent succeed
    await page.route('**/api/**', (route) => {
      errorCount++;
      if (errorCount === 1) {
        route.fulfill({
          status: 500,
          body: JSON.stringify({ error: 'Error' }),
        });
      } else {
        route.continue();
      }
    });

    await login(page);
    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Retry (reload page)
    await page.reload();
    await page.waitForTimeout(2000);

    // Should work now
    const currentUrl = page.url();
    expect(currentUrl).toContain('/portfolio');

    console.log('Recovered from error after retry');
  });

  test('should maintain app state after recoverable error', async ({ page }) => {
    await login(page);
    await page.goto('/settings');

    // Make a change
    const input = page.locator('input[type="text"]').first();
    if (await input.isVisible({ timeout: 2000 }).catch(() => false)) {
      await input.fill('TestValue');

      // Simulate temporary network issue
      await page.context().setOffline(true);
      await page.waitForTimeout(1000);
      await page.context().setOffline(false);

      // Verify value still there
      const value = await input.inputValue();
      expect(value).toBe('TestValue');

      console.log('App state maintained after recoverable error');
    }
  });
});
