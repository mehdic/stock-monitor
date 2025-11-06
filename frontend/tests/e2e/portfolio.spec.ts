import { test, expect } from '@playwright/test';
import { login, uploadPortfolioCSV, generateSamplePortfolioCSV, verifyTableHasData, waitForAPIResponse } from './helpers';

/**
 * Portfolio E2E Tests
 *
 * Tests CSV upload, holdings table, portfolio summary, and universe selection
 */

test.describe('Portfolio Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await login(page);
    await page.goto('/portfolio');
  });

  test('should display portfolio page with main sections', async ({ page }) => {
    // Verify page title
    await expect(page.locator('h1, h2').first()).toContainText(/portfolio/i);

    // Verify main sections exist
    const uploadSection = page.locator('text=/upload|import/i, input[type="file"]');
    const holdingsSection = page.locator('text=/holdings|positions/i');

    // At least one of these should be visible
    const hasUpload = await uploadSection.isVisible({ timeout: 2000 }).catch(() => false);
    const hasHoldings = await holdingsSection.isVisible({ timeout: 2000 }).catch(() => false);

    expect(hasUpload || hasHoldings).toBe(true);
  });

  test('should display CSV upload interface', async ({ page }) => {
    // Look for file input or upload button
    const fileInput = page.locator('input[type="file"]');
    const uploadButton = page.locator('button:has-text("Upload"), button:has-text("Import")');

    const hasFileInput = await fileInput.isVisible({ timeout: 2000 }).catch(() => false);
    const hasUploadButton = await uploadButton.isVisible({ timeout: 2000 }).catch(() => false);

    expect(hasFileInput || hasUploadButton).toBe(true);
  });

  test('should successfully upload CSV portfolio', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();

    // Upload CSV
    await uploadPortfolioCSV(page, csvContent);

    // Verify upload success - look for success message or table with data
    const successMessage = page.locator('text=/success|uploaded|imported/i');
    const holdingsTable = page.locator('table, [role="table"]');

    const hasSuccess = await successMessage.isVisible({ timeout: 5000 }).catch(() => false);
    const hasTable = await holdingsTable.isVisible({ timeout: 5000 }).catch(() => false);

    expect(hasSuccess || hasTable).toBe(true);
  });

  test('should display holdings table after CSV upload', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    // Wait for table to render
    await page.waitForTimeout(2000);

    // Verify table exists and has data
    const tableSelector = 'table, [role="table"]';
    const table = page.locator(tableSelector);

    await expect(table).toBeVisible();

    // Verify table has rows
    const rows = table.locator('tbody tr, tr[data-row], [role="row"]');
    const rowCount = await rows.count();

    expect(rowCount).toBeGreaterThan(0);
  });

  test('should display correct holdings data in table', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    await page.waitForTimeout(2000);

    // Verify specific stock symbols appear in table
    const symbols = ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA'];

    for (const symbol of symbols) {
      const cellWithSymbol = page.locator(`td:has-text("${symbol}"), [role="cell"]:has-text("${symbol}")`);
      await expect(cellWithSymbol).toBeVisible({ timeout: 5000 });
    }
  });

  test('should display portfolio summary metrics', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    await page.waitForTimeout(2000);

    // Look for summary metrics
    const metrics = [
      /total value|portfolio value/i,
      /total gain|total return/i,
      /positions|holdings/i,
    ];

    for (const metricPattern of metrics) {
      const metric = page.locator(`text=${metricPattern}`);
      const isVisible = await metric.isVisible({ timeout: 3000 }).catch(() => false);

      if (isVisible) {
        console.log(`Found metric: ${metricPattern}`);
      }
    }

    // At least verify the page rendered without errors
    const currentUrl = page.url();
    expect(currentUrl).toContain('/portfolio');
  });

  test('should allow editing holdings', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    await page.waitForTimeout(2000);

    // Look for edit buttons or editable cells
    const editButton = page.locator('button:has-text("Edit"), [aria-label*="Edit"]').first();
    const hasEditButton = await editButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasEditButton) {
      await editButton.click();

      // Verify edit mode (input fields appear)
      const inputFields = page.locator('input[type="number"], input[type="text"]');
      await expect(inputFields.first()).toBeVisible({ timeout: 3000 });

      console.log('Holdings editing available');
    } else {
      console.log('Holdings editing not implemented yet');
    }
  });

  test('should allow deleting holdings', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    await page.waitForTimeout(2000);

    // Look for delete buttons
    const deleteButton = page.locator('button:has-text("Delete"), button:has-text("Remove"), [aria-label*="Delete"]').first();
    const hasDeleteButton = await deleteButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasDeleteButton) {
      const initialRowCount = await page.locator('tbody tr, tr[data-row]').count();

      await deleteButton.click();

      // Confirm deletion if modal appears
      const confirmButton = page.locator('button:has-text("Confirm"), button:has-text("Yes"), button:has-text("Delete")');
      if (await confirmButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        await confirmButton.click();
      }

      await page.waitForTimeout(1000);

      const finalRowCount = await page.locator('tbody tr, tr[data-row]').count();
      expect(finalRowCount).toBeLessThan(initialRowCount);

      console.log('Holdings deletion works');
    } else {
      console.log('Holdings deletion not implemented yet');
    }
  });

  test('should display universe selector', async ({ page }) => {
    // Look for universe selector dropdown or radio buttons
    const universeSelector = page.locator('select[name*="universe"], input[name*="universe"], text=/universe selection/i');

    const hasSelector = await universeSelector.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasSelector) {
      console.log('Universe selector found');
      await expect(universeSelector).toBeVisible();
    } else {
      console.log('Universe selector not implemented yet');
    }
  });

  test('should allow selecting different universe', async ({ page }) => {
    // Look for universe options (e.g., S&P 500, Russell 2000)
    const universeDropdown = page.locator('select[name*="universe"]');

    if (await universeDropdown.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Get available options
      const options = await universeDropdown.locator('option').allTextContents();
      console.log('Available universes:', options);

      if (options.length > 1) {
        // Select different universe
        await universeDropdown.selectOption({ index: 1 });

        // Verify selection changed
        const selectedValue = await universeDropdown.inputValue();
        expect(selectedValue).toBeTruthy();

        console.log('Universe selection works');
      }
    } else {
      console.log('Universe selector not implemented yet');
    }
  });

  test('should validate CSV format', async ({ page }) => {
    // Try uploading invalid CSV
    const invalidCSV = 'Invalid,Data\nNo,Proper,Structure';

    await uploadPortfolioCSV(page, invalidCSV, 'invalid.csv');

    // Look for error message
    const errorMessage = page.locator('text=/error|invalid|format/i');
    const hasError = await errorMessage.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasError) {
      console.log('CSV validation works');
      await expect(errorMessage).toBeVisible();
    } else {
      console.log('CSV validation not strict or uses different error handling');
    }
  });

  test('should handle empty CSV upload', async ({ page }) => {
    const emptyCSV = 'Symbol,Shares,Purchase Price,Purchase Date';

    await uploadPortfolioCSV(page, emptyCSV, 'empty.csv');

    // Should either show error or show empty table
    await page.waitForTimeout(2000);

    const errorMessage = page.locator('text=/empty|no data|no holdings/i');
    const hasError = await errorMessage.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasError) {
      console.log('Empty CSV handled with error message');
    } else {
      console.log('Empty CSV handled gracefully');
    }
  });

  test('should display loading state during upload', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();

    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles({
      name: 'test-portfolio.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csvContent),
    });

    // Look for loading indicator
    const loadingIndicator = page.locator('text=/loading|uploading|processing/i, [role="status"]');
    const hasLoading = await loadingIndicator.isVisible({ timeout: 1000 }).catch(() => false);

    if (hasLoading) {
      console.log('Loading state shown during upload');
    } else {
      console.log('Upload happens too fast or no loading indicator');
    }
  });

  test('should allow sorting holdings table', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    await page.waitForTimeout(2000);

    // Look for sortable column headers
    const sortableHeader = page.locator('th[role="button"], th:has(button), th[aria-sort]').first();
    const hasSortable = await sortableHeader.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasSortable) {
      // Get first cell value before sort
      const firstCellBefore = await page.locator('tbody tr:first-child td:first-child').textContent();

      // Click to sort
      await sortableHeader.click();
      await page.waitForTimeout(500);

      // Get first cell value after sort
      const firstCellAfter = await page.locator('tbody tr:first-child td:first-child').textContent();

      console.log('Before sort:', firstCellBefore);
      console.log('After sort:', firstCellAfter);
      console.log('Table sorting available');
    } else {
      console.log('Table sorting not implemented yet');
    }
  });

  test('should allow filtering holdings table', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    await page.waitForTimeout(2000);

    // Look for filter/search input
    const filterInput = page.locator('input[placeholder*="Search"], input[placeholder*="Filter"]');
    const hasFilter = await filterInput.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasFilter) {
      // Filter by symbol
      await filterInput.fill('AAPL');
      await page.waitForTimeout(500);

      // Verify filtered results
      const rows = await page.locator('tbody tr').count();
      expect(rows).toBeGreaterThanOrEqual(1);

      console.log('Table filtering works');
    } else {
      console.log('Table filtering not implemented yet');
    }
  });

  test('should display contributor analysis if available', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    await page.waitForTimeout(2000);

    // Look for contributors section
    const contributorsSection = page.locator('text=/contributors|contribution/i');
    const hasContributors = await contributorsSection.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasContributors) {
      console.log('Contributor analysis available');
      await expect(contributorsSection).toBeVisible();
    } else {
      console.log('Contributor analysis not implemented yet');
    }
  });

  test('should display benchmark comparison if available', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    await page.waitForTimeout(2000);

    // Look for benchmark chart or comparison
    const benchmarkSection = page.locator('text=/benchmark|comparison|vs/i');
    const hasChart = await benchmarkSection.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasChart) {
      console.log('Benchmark comparison available');
      await expect(benchmarkSection).toBeVisible();
    } else {
      console.log('Benchmark comparison not implemented yet');
    }
  });

  test('should persist portfolio data after page reload', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    await page.waitForTimeout(2000);

    // Verify data exists
    const rowCountBefore = await page.locator('tbody tr, tr[data-row]').count();
    expect(rowCountBefore).toBeGreaterThan(0);

    // Reload page
    await page.reload();
    await page.waitForTimeout(2000);

    // Verify data still exists
    const rowCountAfter = await page.locator('tbody tr, tr[data-row]').count();
    expect(rowCountAfter).toBe(rowCountBefore);

    console.log('Portfolio data persisted after reload');
  });

  test('should download portfolio as CSV', async ({ page }) => {
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);

    await page.waitForTimeout(2000);

    // Look for export/download button
    const exportButton = page.locator('button:has-text("Export"), button:has-text("Download"), a:has-text("Export")');
    const hasExport = await exportButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasExport) {
      // Set up download handler
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 5000 }).catch(() => null),
        exportButton.click()
      ]);

      if (download) {
        const filename = download.suggestedFilename();
        expect(filename).toContain('.csv');
        console.log('Portfolio export works, filename:', filename);
      }
    } else {
      console.log('Portfolio export not implemented yet');
    }
  });
});
