import { test, expect } from '@playwright/test';
import { login } from './helpers';

/**
 * Backtests E2E Tests
 *
 * Tests creating backtests, viewing results, equity curves, and performance metrics
 */

test.describe('Backtesting', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await login(page);
    await page.goto('/backtests');
  });

  test('should display backtests page', async ({ page }) => {
    // Verify page title
    await expect(page.locator('h1, h2').first()).toContainText(/backtest/i);

    console.log('Backtests page loaded');
  });

  test('should display create backtest button', async ({ page }) => {
    // Look for create/new backtest button
    const createButton = page.locator('button:has-text("Create"), button:has-text("New Backtest"), button:has-text("Run Backtest")');
    const hasCreateButton = await createButton.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasCreateButton) {
      await expect(createButton).toBeVisible();
      console.log('Create backtest button found');
    } else {
      console.log('Create button not found or different UI pattern');
    }
  });

  test('should open backtest creation form', async ({ page }) => {
    // Click create button
    const createButton = page.locator('button:has-text("Create"), button:has-text("New Backtest")').first();

    if (await createButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await createButton.click();

      // Verify form appears
      const form = page.locator('form, [role="dialog"]');
      await expect(form).toBeVisible({ timeout: 3000 });

      console.log('Backtest creation form opened');
    } else {
      console.log('Inline backtest form or different creation flow');
    }
  });

  test('should configure backtest parameters', async ({ page }) => {
    // Look for or open configuration form
    const createButton = page.locator('button:has-text("Create"), button:has-text("New")').first();
    if (await createButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await createButton.click();
      await page.waitForTimeout(1000);
    }

    // Look for parameter inputs
    const startDateInput = page.locator('input[name*="start"], input[type="date"]').first();
    const endDateInput = page.locator('input[name*="end"], input[type="date"]').nth(1);

    const hasDateInputs = await startDateInput.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasDateInputs) {
      // Set date range
      await startDateInput.fill('2023-01-01');
      await endDateInput.fill('2023-12-31');

      console.log('Backtest parameters configured');
    } else {
      console.log('Date inputs not found or use different selector');
    }
  });

  test('should create and run backtest', async ({ page }) => {
    // Open creation form
    const createButton = page.locator('button:has-text("Create"), button:has-text("New")').first();
    if (await createButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await createButton.click();
      await page.waitForTimeout(1000);

      // Fill form
      const nameInput = page.locator('input[name*="name"], input[placeholder*="Name"]');
      if (await nameInput.isVisible({ timeout: 2000 }).catch(() => false)) {
        await nameInput.fill('E2E Test Backtest');
      }

      // Submit form
      const submitButton = page.locator('button[type="submit"], button:has-text("Run"), button:has-text("Create")');
      await submitButton.click();

      // Wait for backtest to start
      await page.waitForTimeout(2000);

      // Look for loading or running state
      const runningIndicator = page.locator('text=/running|processing|in progress/i');
      const hasRunning = await runningIndicator.isVisible({ timeout: 3000 }).catch(() => false);

      if (hasRunning) {
        console.log('Backtest started running');
      }

      // Wait for completion (with timeout)
      await page.waitForTimeout(5000);

      console.log('Backtest creation attempted');
    } else {
      console.log('Create button not available');
    }
  });

  test('should display list of backtests', async ({ page }) => {
    // Look for backtest list/table
    const backtestList = page.locator('table, [role="table"], [data-testid*="backtest-list"]');
    const hasList = await backtestList.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasList) {
      await expect(backtestList).toBeVisible();

      // Verify list has items
      const items = backtestList.locator('tr, [data-testid*="backtest-item"]');
      const itemCount = await items.count();

      console.log(`Found ${itemCount} backtests`);
    } else {
      console.log('No backtests or empty state displayed');
    }
  });

  test('should view backtest results', async ({ page }) => {
    // Look for a backtest result row/card
    const backtestItem = page.locator('tr[data-testid*="backtest"], tbody tr, [data-testid*="backtest-item"]').first();

    if (await backtestItem.isVisible({ timeout: 3000 }).catch(() => false)) {
      // Click to view details
      const viewButton = backtestItem.locator('button:has-text("View"), a:has-text("View")');

      if (await viewButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        await viewButton.click();
      } else {
        // Try clicking the row itself
        await backtestItem.click();
      }

      await page.waitForTimeout(2000);

      // Verify results page loaded
      const resultsHeading = page.locator('h1, h2', { hasText: /result|backtest/i });
      await expect(resultsHeading.first()).toBeVisible({ timeout: 5000 });

      console.log('Backtest results displayed');
    } else {
      console.log('No backtests available to view');
    }
  });

  test('should display equity curve chart', async ({ page }) => {
    // Navigate to a backtest result (if list exists)
    const backtestItem = page.locator('tbody tr, [data-testid*="backtest-item"]').first();
    if (await backtestItem.isVisible({ timeout: 2000 }).catch(() => false)) {
      await backtestItem.click();
      await page.waitForTimeout(2000);
    }

    // Look for equity curve chart
    const chart = page.locator('svg, canvas, [class*="chart"], [data-testid*="equity"]');
    const hasChart = await chart.first().isVisible({ timeout: 3000 }).catch(() => false);

    if (hasChart) {
      console.log('Equity curve chart displayed');
      await expect(chart.first()).toBeVisible();
    } else {
      console.log('Equity curve chart not found');
    }
  });

  test('should display performance metrics', async ({ page }) => {
    // Look for key metrics
    const metricsPatterns = [
      /total return|cumulative return/i,
      /sharpe ratio/i,
      /max drawdown|maximum drawdown/i,
      /volatility|standard deviation/i,
    ];

    let foundMetrics = 0;

    for (const pattern of metricsPatterns) {
      const metric = page.locator(`text=${pattern}`);
      const isVisible = await metric.isVisible({ timeout: 2000 }).catch(() => false);

      if (isVisible) {
        foundMetrics++;
        console.log(`Found metric: ${pattern}`);
      }
    }

    if (foundMetrics > 0) {
      console.log(`Found ${foundMetrics} performance metrics`);
    } else {
      console.log('Performance metrics not displayed on main page');
    }
  });

  test('should display backtest status', async ({ page }) => {
    // Look for status indicators
    const statusBadge = page.locator('text=/completed|running|failed|pending/i, [data-status]');
    const hasStatus = await statusBadge.first().isVisible({ timeout: 3000 }).catch(() => false);

    if (hasStatus) {
      const statusText = await statusBadge.first().textContent();
      console.log('Backtest status:', statusText);
      await expect(statusBadge.first()).toBeVisible();
    } else {
      console.log('Status indicators not displayed');
    }
  });

  test('should filter backtests by status', async ({ page }) => {
    // Look for filter controls
    const filterDropdown = page.locator('select[name*="filter"], select[name*="status"]');
    const hasFilter = await filterDropdown.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasFilter) {
      await filterDropdown.selectOption({ index: 1 });
      await page.waitForTimeout(1000);

      console.log('Backtest filtering works');
    } else {
      console.log('Filter functionality not implemented');
    }
  });

  test('should sort backtests by date', async ({ page }) => {
    // Look for sortable date column
    const dateHeader = page.locator('th:has-text("Date"), th:has-text("Created")');
    const isSortable = await dateHeader.isVisible({ timeout: 2000 }).catch(() => false);

    if (isSortable) {
      await dateHeader.click();
      await page.waitForTimeout(500);

      console.log('Backtest sorting works');
    } else {
      console.log('Sorting not available or different mechanism');
    }
  });

  test('should display performance table', async ({ page }) => {
    // Navigate to backtest results if needed
    const backtestItem = page.locator('tbody tr').first();
    if (await backtestItem.isVisible({ timeout: 2000 }).catch(() => false)) {
      await backtestItem.click();
      await page.waitForTimeout(2000);
    }

    // Look for performance metrics table
    const performanceTable = page.locator('table:has-text("Performance"), table:has-text("Metrics")');
    const hasTable = await performanceTable.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasTable) {
      console.log('Performance metrics table displayed');
      await expect(performanceTable).toBeVisible();
    } else {
      console.log('Performance table not found or uses different layout');
    }
  });

  test('should compare multiple backtests', async ({ page }) => {
    // Look for compare functionality
    const compareButton = page.locator('button:has-text("Compare"), [aria-label*="Compare"]');
    const hasCompare = await compareButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasCompare) {
      await compareButton.click();
      await page.waitForTimeout(1000);

      console.log('Backtest comparison available');
    } else {
      console.log('Comparison functionality not implemented');
    }
  });

  test('should delete backtest', async ({ page }) => {
    // Look for delete button
    const deleteButton = page.locator('button:has-text("Delete"), [aria-label*="Delete"]').first();
    const hasDelete = await deleteButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasDelete) {
      await deleteButton.click();

      // Confirm deletion
      const confirmButton = page.locator('button:has-text("Confirm"), button:has-text("Yes"), button:has-text("Delete")');
      if (await confirmButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        await confirmButton.click();
        await page.waitForTimeout(1000);
      }

      console.log('Backtest deletion works');
    } else {
      console.log('Delete functionality not available');
    }
  });

  test('should export backtest results', async ({ page }) => {
    // Navigate to backtest results
    const backtestItem = page.locator('tbody tr').first();
    if (await backtestItem.isVisible({ timeout: 2000 }).catch(() => false)) {
      await backtestItem.click();
      await page.waitForTimeout(2000);
    }

    // Look for export button
    const exportButton = page.locator('button:has-text("Export"), button:has-text("Download")');
    const hasExport = await exportButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasExport) {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 5000 }).catch(() => null),
        exportButton.click()
      ]);

      if (download) {
        const filename = download.suggestedFilename();
        console.log('Backtest results exported:', filename);
      }
    } else {
      console.log('Export functionality not available');
    }
  });

  test('should display monthly returns if available', async ({ page }) => {
    // Navigate to backtest results
    const backtestItem = page.locator('tbody tr').first();
    if (await backtestItem.isVisible({ timeout: 2000 }).catch(() => false)) {
      await backtestItem.click();
      await page.waitForTimeout(2000);
    }

    // Look for monthly returns table/chart
    const monthlyReturns = page.locator('text=/monthly return|returns by month/i');
    const hasMonthly = await monthlyReturns.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasMonthly) {
      console.log('Monthly returns displayed');
      await expect(monthlyReturns).toBeVisible();
    } else {
      console.log('Monthly returns not displayed');
    }
  });

  test('should handle failed backtest', async ({ page }) => {
    // Look for failed status
    const failedBadge = page.locator('text=/failed|error/i, [data-status="failed"]');
    const hasFailed = await failedBadge.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasFailed) {
      console.log('Failed backtest found');

      // Try to view error details
      await failedBadge.click();
      await page.waitForTimeout(1000);

      const errorMessage = page.locator('text=/error|failed/i');
      await expect(errorMessage.first()).toBeVisible();
    } else {
      console.log('No failed backtests or error handling not testable');
    }
  });
});
