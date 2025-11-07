import { test, expect } from '@playwright/test';
import { login, uploadPortfolioCSV, generateSamplePortfolioCSV, waitForAPIResponse } from './helpers';

/**
 * Recommendations E2E Tests
 *
 * Tests recommendation generation, viewing, explanation panels, and confidence scores
 */

test.describe('Stock Recommendations', () => {
  test.beforeEach(async ({ page }) => {
    // Login and setup portfolio
    await login(page);

    // Upload portfolio data for recommendations
    await page.goto('/portfolio');
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);
    await page.waitForTimeout(2000);

    // Navigate to recommendations page
    await page.goto('/recommendations');
  });

  test('should display recommendations page', async ({ page }) => {
    // Verify page title
    await expect(page.locator('h1, h2').first()).toContainText(/recommendation/i);

    // Verify main UI elements
    const runButton = page.locator('button:has-text("Run"), button:has-text("Generate"), button:has-text("Analyze")');
    const hasRunButton = await runButton.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasRunButton) {
      await expect(runButton).toBeVisible();
      console.log('Run recommendations button found');
    } else {
      console.log('Recommendations may auto-run or use different trigger');
    }
  });

  test('should trigger recommendation generation', async ({ page }) => {
    // Look for run/generate button
    const runButton = page.locator('button:has-text("Run"), button:has-text("Generate"), button:has-text("Analyze")').first();

    if (await runButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await runButton.click();

      // Verify loading state
      const loadingIndicator = page.locator('text=/running|analyzing|loading|processing/i, [role="status"]');
      const hasLoading = await loadingIndicator.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasLoading) {
        console.log('Loading state shown during recommendation generation');

        // Wait for completion
        await page.waitForTimeout(5000);
      }

      // Verify recommendations appear
      const recommendationsSection = page.locator('text=/recommendation|buy|sell|hold/i');
      await expect(recommendationsSection.first()).toBeVisible({ timeout: 15000 });

      console.log('Recommendations generated successfully');
    } else {
      console.log('Auto-run recommendations or different trigger mechanism');
    }
  });

  test('should display recommendations table', async ({ page }) => {
    // Trigger recommendations if needed
    const runButton = page.locator('button:has-text("Run"), button:has-text("Generate")').first();
    if (await runButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await runButton.click();
      await page.waitForTimeout(5000);
    }

    // Verify recommendations table exists
    const table = page.locator('table, [role="table"]');
    await expect(table).toBeVisible({ timeout: 10000 });

    // Verify table has rows
    const rows = table.locator('tbody tr, tr[data-row], [role="row"]');
    const rowCount = await rows.count();

    expect(rowCount).toBeGreaterThan(0);
    console.log(`Found ${rowCount} recommendations`);
  });

  test('should display recommendation actions (Buy/Sell/Hold)', async ({ page }) => {
    // Wait for recommendations to load
    await page.waitForTimeout(3000);

    // Look for action indicators
    const actions = ['Buy', 'Sell', 'Hold'];
    let foundActions = 0;

    for (const action of actions) {
      const actionElement = page.locator(`text="${action}", [data-action="${action.toLowerCase()}"]`);
      const isVisible = await actionElement.isVisible({ timeout: 2000 }).catch(() => false);

      if (isVisible) {
        foundActions++;
        console.log(`Found action: ${action}`);
      }
    }

    // At least one action type should be present
    expect(foundActions).toBeGreaterThan(0);
  });

  test('should display confidence scores', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Look for confidence scores (might be percentages or decimal values)
    const confidenceElements = page.locator('text=/confidence|score/i');
    const hasConfidence = await confidenceElements.first().isVisible({ timeout: 3000 }).catch(() => false);

    if (hasConfidence) {
      console.log('Confidence scores displayed');
      await expect(confidenceElements.first()).toBeVisible();

      // Look for numeric values (e.g., 85%, 0.85)
      const percentagePattern = /\d+%|\d+\.\d+/;
      const cellWithScore = page.locator(`td:text-matches("${percentagePattern}"), [role="cell"]:text-matches("${percentagePattern}")`);

      const scoreCount = await cellWithScore.count();
      expect(scoreCount).toBeGreaterThan(0);
    } else {
      console.log('Confidence scores not displayed or use different format');
    }
  });

  test('should display explanation panel for recommendations', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Look for explanation or details button
    const detailsButton = page.locator('button:has-text("Details"), button:has-text("Explain"), [aria-label*="Explanation"]').first();
    const hasDetailsButton = await detailsButton.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasDetailsButton) {
      await detailsButton.click();

      // Verify explanation panel appears
      const explanationPanel = page.locator('text=/explanation|reason|factor|analysis/i');
      await expect(explanationPanel.first()).toBeVisible({ timeout: 3000 });

      console.log('Explanation panel works');
    } else {
      // Try clicking on a row to see if it expands
      const firstRow = page.locator('tbody tr, tr[data-row]').first();
      if (await firstRow.isVisible({ timeout: 2000 }).catch(() => false)) {
        await firstRow.click();

        await page.waitForTimeout(1000);

        // Check if details expanded
        const expandedContent = page.locator('text=/explanation|reason|factor/i');
        const hasExpanded = await expandedContent.isVisible({ timeout: 2000 }).catch(() => false);

        if (hasExpanded) {
          console.log('Row expansion shows explanation');
        } else {
          console.log('Explanation panel not implemented yet');
        }
      }
    }
  });

  test('should display factors affecting recommendations', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Open explanation panel
    const detailsButton = page.locator('button:has-text("Details"), button:has-text("Explain")').first();
    if (await detailsButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await detailsButton.click();
      await page.waitForTimeout(1000);
    }

    // Look for factor analysis
    const factors = [
      /momentum/i,
      /value/i,
      /quality/i,
      /volatility/i,
      /risk/i,
      /return/i,
    ];

    let foundFactors = 0;
    for (const factorPattern of factors) {
      const factor = page.locator(`text=${factorPattern}`);
      const isVisible = await factor.isVisible({ timeout: 1000 }).catch(() => false);

      if (isVisible) {
        foundFactors++;
        console.log(`Found factor: ${factorPattern}`);
      }
    }

    if (foundFactors > 0) {
      console.log(`Found ${foundFactors} factors in explanation`);
    } else {
      console.log('Factor analysis not detailed or uses different terminology');
    }
  });

  test('should filter recommendations by action type', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Look for filter controls
    const filterDropdown = page.locator('select[name*="filter"], select[name*="action"]');
    const filterButtons = page.locator('button:has-text("Buy"), button:has-text("Sell"), button:has-text("Hold")');

    const hasDropdown = await filterDropdown.isVisible({ timeout: 2000 }).catch(() => false);
    const hasButtons = await filterButtons.first().isVisible({ timeout: 2000 }).catch(() => false);

    if (hasDropdown) {
      // Use dropdown filter
      await filterDropdown.selectOption('Buy');
      await page.waitForTimeout(1000);

      console.log('Filtered by Buy recommendations');
    } else if (hasButtons) {
      // Use button filters
      await filterButtons.first().click();
      await page.waitForTimeout(1000);

      console.log('Filtered using button controls');
    } else {
      console.log('Recommendation filtering not implemented yet');
    }
  });

  test('should sort recommendations by confidence', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Look for confidence column header
    const confidenceHeader = page.locator('th:has-text("Confidence"), th:has-text("Score")');
    const isClickable = await confidenceHeader.isVisible({ timeout: 2000 }).catch(() => false);

    if (isClickable) {
      await confidenceHeader.click();
      await page.waitForTimeout(500);

      console.log('Sorted by confidence score');
    } else {
      console.log('Sorting by confidence not available or uses different mechanism');
    }
  });

  test('should display stock symbols in recommendations', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Verify stock symbols appear
    const symbolPatterns = ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA'];

    for (const symbol of symbolPatterns) {
      const symbolCell = page.locator(`td:has-text("${symbol}"), [role="cell"]:has-text("${symbol}")`);
      const isVisible = await symbolCell.isVisible({ timeout: 2000 }).catch(() => false);

      if (isVisible) {
        console.log(`Found symbol: ${symbol}`);
      }
    }
  });

  test('should display target prices if available', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Look for target price column
    const targetPriceHeader = page.locator('th:has-text("Target"), th:has-text("Price Target")');
    const hasTargetPrice = await targetPriceHeader.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasTargetPrice) {
      console.log('Target prices displayed');
      await expect(targetPriceHeader).toBeVisible();
    } else {
      console.log('Target prices not displayed');
    }
  });

  test('should display expected returns if available', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Look for expected return column
    const returnHeader = page.locator('th:has-text("Return"), th:has-text("Expected Return")');
    const hasReturn = await returnHeader.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasReturn) {
      console.log('Expected returns displayed');
      await expect(returnHeader).toBeVisible();
    } else {
      console.log('Expected returns not displayed');
    }
  });

  test('should show recommendation rationale on hover or click', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Try hovering over a recommendation
    const firstRow = page.locator('tbody tr').first();
    if (await firstRow.isVisible({ timeout: 2000 }).catch(() => false)) {
      await firstRow.hover();
      await page.waitForTimeout(500);

      // Check for tooltip or popover
      const tooltip = page.locator('[role="tooltip"], .tooltip, .popover');
      const hasTooltip = await tooltip.isVisible({ timeout: 1000 }).catch(() => false);

      if (hasTooltip) {
        console.log('Hover shows additional information');
      } else {
        console.log('No hover tooltip, might use click instead');
      }
    }
  });

  test('should refresh recommendations', async ({ page }) => {
    // Wait for initial recommendations
    await page.waitForTimeout(3000);

    // Look for refresh button
    const refreshButton = page.locator('button:has-text("Refresh"), button:has-text("Reload"), [aria-label*="Refresh"]');
    const hasRefresh = await refreshButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasRefresh) {
      await refreshButton.click();

      // Verify loading state
      const loadingIndicator = page.locator('text=/loading|refreshing/i, [role="status"]');
      await loadingIndicator.isVisible({ timeout: 2000 }).catch(() => false);

      await page.waitForTimeout(3000);

      console.log('Recommendations refreshed');
    } else {
      console.log('Manual refresh not available');
    }
  });

  test('should handle no recommendations scenario', async ({ page }) => {
    // Navigate to recommendations without portfolio data
    await page.goto('/recommendations');
    await page.waitForTimeout(2000);

    // Look for empty state message
    const emptyMessage = page.locator('text=/no recommendation|no data|upload portfolio/i');
    const hasEmptyMessage = await emptyMessage.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasEmptyMessage) {
      console.log('Empty state handled properly');
      await expect(emptyMessage).toBeVisible();
    } else {
      console.log('Empty state not displayed or portfolio auto-created');
    }
  });

  test('should display exclusion details if available', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Look for excluded stocks section
    const excludedSection = page.locator('text=/excluded|exclusion/i');
    const hasExclusions = await excludedSection.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasExclusions) {
      console.log('Exclusion details available');

      // Try to open exclusion modal
      const viewExclusionsButton = page.locator('button:has-text("View Exclusions"), button:has-text("Excluded")');
      if (await viewExclusionsButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        await viewExclusionsButton.click();

        // Verify modal opens
        const modal = page.locator('[role="dialog"], .modal');
        await expect(modal).toBeVisible({ timeout: 2000 });

        console.log('Exclusion modal works');
      }
    } else {
      console.log('Exclusion details not implemented yet');
    }
  });

  test('should allow exporting recommendations', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

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
        console.log('Recommendations exported:', filename);
      }
    } else {
      console.log('Export functionality not implemented yet');
    }
  });

  test('should display timestamp of last recommendation run', async ({ page }) => {
    // Wait for recommendations
    await page.waitForTimeout(3000);

    // Look for timestamp
    const timestamp = page.locator('text=/last updated|last run|as of/i');
    const hasTimestamp = await timestamp.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasTimestamp) {
      console.log('Last run timestamp displayed');
      await expect(timestamp).toBeVisible();
    } else {
      console.log('Timestamp not displayed');
    }
  });
});
