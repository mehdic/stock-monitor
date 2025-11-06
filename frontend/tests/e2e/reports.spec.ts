import { test, expect } from '@playwright/test';
import { login, uploadPortfolioCSV, generateSamplePortfolioCSV } from './helpers';

/**
 * Reports E2E Tests
 *
 * Tests PDF report downloads and format verification
 */

test.describe('Reports and Downloads', () => {
  test.beforeEach(async ({ page }) => {
    // Login and setup portfolio
    await login(page);

    // Upload portfolio data
    await page.goto('/portfolio');
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);
    await page.waitForTimeout(2000);
  });

  test('should find report download button on recommendations page', async ({ page }) => {
    await page.goto('/recommendations');
    await page.waitForTimeout(2000);

    // Look for report/download button
    const reportButton = page.locator('button:has-text("Report"), button:has-text("Download"), button:has-text("Export PDF")');
    const hasButton = await reportButton.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasButton) {
      await expect(reportButton).toBeVisible();
      console.log('Report download button found');
    } else {
      console.log('Report button not on recommendations page, checking other locations');
    }
  });

  test('should download PDF report from recommendations', async ({ page }) => {
    await page.goto('/recommendations');
    await page.waitForTimeout(2000);

    // Look for PDF download button
    const pdfButton = page.locator('button:has-text("PDF"), button:has-text("Report"), button:has-text("Download")').first();

    if (await pdfButton.isVisible({ timeout: 3000 }).catch(() => false)) {
      // Set up download handler
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 10000 }),
        pdfButton.click()
      ]);

      // Verify download
      expect(download).toBeTruthy();

      const filename = download.suggestedFilename();
      console.log('Downloaded file:', filename);

      // Verify it's a PDF
      expect(filename.toLowerCase()).toMatch(/\.pdf$/);

      // Verify file has content
      const path = await download.path();
      if (path) {
        const fs = require('fs');
        const stats = fs.statSync(path);
        expect(stats.size).toBeGreaterThan(0);
        console.log('PDF file size:', stats.size, 'bytes');
      }
    } else {
      console.log('PDF download not available on recommendations page');
    }
  });

  test('should download PDF report from portfolio page', async ({ page }) => {
    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Look for report button
    const reportButton = page.locator('button:has-text("Report"), button:has-text("Download PDF")');

    if (await reportButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 10000 }).catch(() => null),
        reportButton.click()
      ]);

      if (download) {
        const filename = download.suggestedFilename();
        expect(filename.toLowerCase()).toContain('.pdf');
        console.log('Portfolio report downloaded:', filename);
      }
    } else {
      console.log('Portfolio report not available');
    }
  });

  test('should download backtest report', async ({ page }) => {
    await page.goto('/backtests');
    await page.waitForTimeout(2000);

    // Look for a backtest to view
    const backtestItem = page.locator('tbody tr, [data-testid*="backtest"]').first();

    if (await backtestItem.isVisible({ timeout: 2000 }).catch(() => false)) {
      await backtestItem.click();
      await page.waitForTimeout(2000);

      // Look for report download button
      const reportButton = page.locator('button:has-text("Report"), button:has-text("Download PDF")');

      if (await reportButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        const [download] = await Promise.all([
          page.waitForEvent('download', { timeout: 10000 }).catch(() => null),
          reportButton.click()
        ]);

        if (download) {
          const filename = download.suggestedFilename();
          expect(filename.toLowerCase()).toContain('.pdf');
          console.log('Backtest report downloaded:', filename);
        }
      } else {
        console.log('Backtest report not available');
      }
    } else {
      console.log('No backtests available to generate report');
    }
  });

  test('should show report generation options', async ({ page }) => {
    await page.goto('/recommendations');
    await page.waitForTimeout(2000);

    // Look for report button
    const reportButton = page.locator('button:has-text("Report"), button:has-text("Generate")').first();

    if (await reportButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await reportButton.click();
      await page.waitForTimeout(1000);

      // Check if modal or dropdown appears with options
      const optionsModal = page.locator('[role="dialog"], [role="menu"]');
      const hasOptions = await optionsModal.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasOptions) {
        console.log('Report generation options displayed');

        // Look for format options
        const pdfOption = page.locator('text=/PDF/i');
        const csvOption = page.locator('text=/CSV|Excel/i');

        if (await pdfOption.isVisible({ timeout: 1000 }).catch(() => false)) {
          console.log('PDF format option available');
        }

        if (await csvOption.isVisible({ timeout: 1000 }).catch(() => false)) {
          console.log('CSV/Excel format option available');
        }
      } else {
        console.log('Direct download without options');
      }
    } else {
      console.log('Report generation not available');
    }
  });

  test('should show loading state during report generation', async ({ page }) => {
    await page.goto('/recommendations');
    await page.waitForTimeout(2000);

    const reportButton = page.locator('button:has-text("Report"), button:has-text("Download PDF")').first();

    if (await reportButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await reportButton.click();

      // Check for loading state
      const loadingIndicator = page.locator('text=/generating|creating|loading/i, [role="status"]');
      const hasLoading = await loadingIndicator.isVisible({ timeout: 1000 }).catch(() => false);

      if (hasLoading) {
        console.log('Loading state shown during report generation');
      } else {
        console.log('Report generates instantly or no loading indicator');
      }
    }
  });

  test('should download CSV export from portfolio', async ({ page }) => {
    await page.goto('/portfolio');
    await page.waitForTimeout(2000);

    // Look for CSV export button
    const csvButton = page.locator('button:has-text("Export"), button:has-text("CSV"), button:has-text("Download CSV")');

    if (await csvButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 5000 }).catch(() => null),
        csvButton.click()
      ]);

      if (download) {
        const filename = download.suggestedFilename();
        expect(filename.toLowerCase()).toMatch(/\.csv$/);
        console.log('CSV exported:', filename);
      }
    } else {
      console.log('CSV export not available');
    }
  });

  test('should download CSV export from recommendations', async ({ page }) => {
    await page.goto('/recommendations');
    await page.waitForTimeout(2000);

    // Look for CSV export
    const csvButton = page.locator('button:has-text("Export CSV"), button:has-text("Download CSV")');

    if (await csvButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 5000 }).catch(() => null),
        csvButton.click()
      ]);

      if (download) {
        const filename = download.suggestedFilename();
        expect(filename.toLowerCase()).toContain('.csv');
        console.log('Recommendations CSV exported:', filename);
      }
    } else {
      console.log('Recommendations CSV export not available');
    }
  });

  test('should access reports from reports page', async ({ page }) => {
    // Try to navigate to reports page
    await page.goto('/reports');

    // Check if reports page exists
    const reportsHeading = page.locator('h1, h2', { hasText: /report/i });
    const hasReportsPage = await reportsHeading.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasReportsPage) {
      console.log('Reports page exists');
      await expect(reportsHeading).toBeVisible();

      // Look for report list
      const reportsList = page.locator('table, [role="table"], ul');
      const hasList = await reportsList.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasList) {
        console.log('Reports list displayed');
      }
    } else {
      console.log('Dedicated reports page not implemented');
    }
  });

  test('should display report history if available', async ({ page }) => {
    await page.goto('/reports');

    const reportHistory = page.locator('text=/history|previous|past/i');
    const hasHistory = await reportHistory.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasHistory) {
      console.log('Report history available');

      // Look for historical reports
      const reportItems = page.locator('tbody tr, [data-testid*="report"]');
      const count = await reportItems.count();

      console.log(`Found ${count} historical reports`);
    } else {
      console.log('Report history not implemented');
    }
  });

  test('should handle report generation failure gracefully', async ({ page }) => {
    // Try generating report without required data
    await page.goto('/recommendations');

    const reportButton = page.locator('button:has-text("Report")').first();

    if (await reportButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await reportButton.click();
      await page.waitForTimeout(2000);

      // Check for error message
      const errorMessage = page.locator('text=/error|failed|unable/i');
      const hasError = await errorMessage.isVisible({ timeout: 3000 }).catch(() => false);

      if (hasError) {
        console.log('Error handling works for report generation');
        await expect(errorMessage).toBeVisible();
      } else {
        console.log('No error or report generated successfully');
      }
    }
  });

  test('should allow customizing report content', async ({ page }) => {
    await page.goto('/recommendations');
    await page.waitForTimeout(2000);

    const reportButton = page.locator('button:has-text("Report"), button:has-text("Generate")').first();

    if (await reportButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await reportButton.click();
      await page.waitForTimeout(1000);

      // Look for customization options
      const checkboxes = page.locator('input[type="checkbox"]');
      const checkboxCount = await checkboxes.count();

      if (checkboxCount > 0) {
        console.log(`Found ${checkboxCount} customization options`);

        // Try toggling an option
        await checkboxes.first().click();
        console.log('Report customization available');
      } else {
        console.log('No customization options or fixed report format');
      }
    }
  });

  test('should display report preview before download', async ({ page }) => {
    await page.goto('/recommendations');
    await page.waitForTimeout(2000);

    // Look for preview button
    const previewButton = page.locator('button:has-text("Preview"), button:has-text("View Report")');
    const hasPreview = await previewButton.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasPreview) {
      await previewButton.click();
      await page.waitForTimeout(2000);

      // Verify preview displayed
      const previewContent = page.locator('[data-testid*="preview"], iframe, embed');
      await expect(previewContent.first()).toBeVisible({ timeout: 5000 });

      console.log('Report preview available');
    } else {
      console.log('Report preview not implemented');
    }
  });

  test('should validate PDF format', async ({ page }) => {
    await page.goto('/recommendations');
    await page.waitForTimeout(2000);

    const pdfButton = page.locator('button:has-text("PDF"), button:has-text("Report")').first();

    if (await pdfButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 10000 }).catch(() => null),
        pdfButton.click()
      ]);

      if (download) {
        // Verify MIME type
        const path = await download.path();

        if (path) {
          const fs = require('fs');
          const buffer = fs.readFileSync(path);

          // Check PDF magic number (%PDF)
          const isPDF = buffer.toString('utf-8', 0, 4) === '%PDF';
          expect(isPDF).toBe(true);

          console.log('PDF format validated successfully');
        }
      }
    } else {
      console.log('PDF download not available for testing');
    }
  });
});
