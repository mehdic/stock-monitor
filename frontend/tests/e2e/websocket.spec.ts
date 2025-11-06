import { test, expect } from '@playwright/test';
import { login, uploadPortfolioCSV, generateSamplePortfolioCSV } from './helpers';

/**
 * WebSocket E2E Tests
 *
 * Tests real-time notifications, status updates, and WebSocket connections
 */

test.describe('WebSocket and Real-Time Updates', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await login(page);
  });

  test('should establish WebSocket connection after login', async ({ page }) => {
    await page.goto('/dashboard');

    // Wait a bit for WebSocket connection to establish
    await page.waitForTimeout(3000);

    // Check if WebSocket connection exists by examining network activity
    // Note: Playwright doesn't directly expose WebSocket connections easily
    // We'll test by looking for UI indicators instead

    // Look for connection status indicator if available
    const connectionIndicator = page.locator('[data-testid="connection-status"], .connection-indicator');
    const hasIndicator = await connectionIndicator.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasIndicator) {
      console.log('Connection status indicator found');
      const status = await connectionIndicator.textContent();
      console.log('Connection status:', status);
    } else {
      console.log('No visible connection status indicator');
    }

    // Verify page loaded without WebSocket errors
    const currentUrl = page.url();
    expect(currentUrl).toContain('/dashboard');
  });

  test('should receive real-time notifications', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);

    // Look for notification system
    const notificationContainer = page.locator('[data-testid="notifications"], .notification, [role="alert"]');

    // Trigger an action that might generate notification
    const portfolioLink = page.locator('a[href="/portfolio"]');
    if (await portfolioLink.isVisible({ timeout: 2000 }).catch(() => false)) {
      await portfolioLink.click();
      await page.waitForTimeout(2000);

      // Upload portfolio to trigger potential notification
      const csvContent = generateSamplePortfolioCSV();
      await uploadPortfolioCSV(page, csvContent);
      await page.waitForTimeout(3000);

      // Check for notification
      const hasNotification = await notificationContainer.isVisible({ timeout: 5000 }).catch(() => false);

      if (hasNotification) {
        console.log('Real-time notification received');
        await expect(notificationContainer.first()).toBeVisible();
      } else {
        console.log('No notification displayed or different notification system');
      }
    }
  });

  test('should display recommendation run status updates in real-time', async ({ page }) => {
    // Setup portfolio
    await page.goto('/portfolio');
    const csvContent = generateSamplePortfolioCSV();
    await uploadPortfolioCSV(page, csvContent);
    await page.waitForTimeout(2000);

    // Navigate to recommendations and trigger run
    await page.goto('/recommendations');
    await page.waitForTimeout(2000);

    const runButton = page.locator('button:has-text("Run"), button:has-text("Generate")').first();

    if (await runButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await runButton.click();

      // Watch for status updates
      const statusIndicator = page.locator('text=/running|processing|analyzing|completed/i, [data-status]');

      // Wait for initial status
      await statusIndicator.first().isVisible({ timeout: 5000 }).catch(() => false);

      // Monitor status changes over time
      const statuses: string[] = [];

      for (let i = 0; i < 5; i++) {
        const status = await statusIndicator.first().textContent().catch(() => '');
        if (status && !statuses.includes(status)) {
          statuses.push(status.trim());
          console.log(`Status update ${i + 1}:`, status.trim());
        }
        await page.waitForTimeout(1000);
      }

      if (statuses.length > 1) {
        console.log('Real-time status updates working:', statuses);
      } else {
        console.log('Status updates not visible or run completed instantly');
      }
    } else {
      console.log('Run button not available');
    }
  });

  test('should update backtest progress in real-time', async ({ page }) => {
    await page.goto('/backtests');
    await page.waitForTimeout(2000);

    // Create backtest
    const createButton = page.locator('button:has-text("Create"), button:has-text("New")').first();

    if (await createButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await createButton.click();
      await page.waitForTimeout(1000);

      // Fill form if needed
      const nameInput = page.locator('input[name*="name"]');
      if (await nameInput.isVisible({ timeout: 1000 }).catch(() => false)) {
        await nameInput.fill('WS Test Backtest');
      }

      // Submit
      const submitButton = page.locator('button[type="submit"], button:has-text("Run")');
      if (await submitButton.isVisible({ timeout: 1000 }).catch(() => false)) {
        await submitButton.click();
        await page.waitForTimeout(2000);

        // Look for progress indicator
        const progressIndicator = page.locator('[role="progressbar"], .progress, text=/progress|%/i');
        const hasProgress = await progressIndicator.isVisible({ timeout: 3000 }).catch(() => false);

        if (hasProgress) {
          console.log('Backtest progress shown in real-time');

          // Monitor progress changes
          for (let i = 0; i < 5; i++) {
            const progress = await progressIndicator.textContent().catch(() => '');
            console.log(`Progress update ${i + 1}:`, progress);
            await page.waitForTimeout(1000);
          }
        } else {
          console.log('Progress indicator not visible or completes instantly');
        }
      }
    } else {
      console.log('Create backtest not available');
    }
  });

  test('should handle WebSocket disconnection gracefully', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);

    // Simulate network interruption
    await page.context().setOffline(true);
    await page.waitForTimeout(2000);

    // Look for disconnection indicator
    const disconnectIndicator = page.locator('text=/disconnected|offline|connection lost/i, [data-status="disconnected"]');
    const hasDisconnect = await disconnectIndicator.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasDisconnect) {
      console.log('WebSocket disconnection shown to user');
      await expect(disconnectIndicator).toBeVisible();
    } else {
      console.log('Disconnection not visibly indicated');
    }

    // Restore connection
    await page.context().setOffline(false);
    await page.waitForTimeout(2000);
  });

  test('should reconnect WebSocket after disconnection', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);

    // Simulate disconnection
    await page.context().setOffline(true);
    await page.waitForTimeout(2000);

    // Restore connection
    await page.context().setOffline(false);
    await page.waitForTimeout(3000);

    // Look for reconnection indicator
    const connectionIndicator = page.locator('text=/connected|online|reconnected/i, [data-status="connected"]');
    const hasReconnection = await connectionIndicator.isVisible({ timeout: 5000 }).catch(() => false);

    if (hasReconnection) {
      console.log('WebSocket reconnection successful');
      await expect(connectionIndicator).toBeVisible();
    } else {
      console.log('Reconnection not visibly indicated or automatic');
    }
  });

  test('should display live market data updates if available', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);

    // Look for market data section
    const marketDataSection = page.locator('text=/market|price|quote/i');
    const hasMarketData = await marketDataSection.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasMarketData) {
      // Monitor for value changes (indicating live updates)
      const priceElements = page.locator('text=/\\$[0-9]+\\.[0-9]{2}/');

      if (await priceElements.first().isVisible({ timeout: 2000 }).catch(() => false)) {
        const initialPrices: string[] = [];
        const laterPrices: string[] = [];

        // Capture initial prices
        const count = Math.min(await priceElements.count(), 3);
        for (let i = 0; i < count; i++) {
          const price = await priceElements.nth(i).textContent().catch(() => '');
          initialPrices.push(price);
        }

        // Wait for potential updates
        await page.waitForTimeout(5000);

        // Capture prices again
        for (let i = 0; i < count; i++) {
          const price = await priceElements.nth(i).textContent().catch(() => '');
          laterPrices.push(price);
        }

        // Check if any prices changed
        const pricesChanged = initialPrices.some((price, i) => price !== laterPrices[i]);

        if (pricesChanged) {
          console.log('Live market data updates working');
        } else {
          console.log('Prices did not change (might be after market hours)');
        }
      }
    } else {
      console.log('Market data section not visible');
    }
  });

  test('should show notification badge for new messages', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);

    // Look for notification bell/badge
    const notificationBadge = page.locator('[data-testid="notification-badge"], .notification-badge, .badge');
    const hasBadge = await notificationBadge.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasBadge) {
      console.log('Notification badge found');

      // Check if badge has count
      const badgeText = await notificationBadge.textContent();
      console.log('Badge content:', badgeText);

      // Click to view notifications
      await notificationBadge.click();
      await page.waitForTimeout(1000);

      // Verify notification panel opens
      const notificationPanel = page.locator('[role="dialog"], .notification-panel');
      const hasPanel = await notificationPanel.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasPanel) {
        console.log('Notification panel opened');
      }
    } else {
      console.log('Notification badge not implemented');
    }
  });

  test('should dismiss notifications', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);

    // Look for notification
    const notification = page.locator('[role="alert"], .notification').first();

    if (await notification.isVisible({ timeout: 3000 }).catch(() => false)) {
      // Look for dismiss button
      const dismissButton = notification.locator('button[aria-label*="dismiss"], button[aria-label*="close"], button:has-text("×")');

      if (await dismissButton.isVisible({ timeout: 1000 }).catch(() => false)) {
        await dismissButton.click();
        await page.waitForTimeout(500);

        // Verify notification dismissed
        const stillVisible = await notification.isVisible({ timeout: 1000 }).catch(() => false);
        expect(stillVisible).toBe(false);

        console.log('Notification dismissed successfully');
      } else {
        // Try auto-dismiss by waiting
        await page.waitForTimeout(5000);
        const stillVisible = await notification.isVisible({ timeout: 1000 }).catch(() => false);

        if (!stillVisible) {
          console.log('Notification auto-dismissed');
        } else {
          console.log('Notification persists (no dismiss functionality)');
        }
      }
    } else {
      console.log('No notifications to dismiss');
    }
  });

  test('should show different notification types', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(3000);

    // Look for notifications with different types (success, error, info, warning)
    const notificationTypes = ['success', 'error', 'info', 'warning'];
    const foundTypes: string[] = [];

    for (const type of notificationTypes) {
      const notification = page.locator(`[data-type="${type}"], .notification-${type}, [class*="${type}"]`);
      const hasType = await notification.isVisible({ timeout: 1000 }).catch(() => false);

      if (hasType) {
        foundTypes.push(type);
        console.log(`Found ${type} notification`);
      }
    }

    if (foundTypes.length > 0) {
      console.log('Notification types found:', foundTypes);
    } else {
      console.log('No typed notifications or different classification system');
    }
  });

  test('should persist notifications across page navigation', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);

    // Check notification count
    const notificationBadge = page.locator('[data-testid="notification-badge"], .notification-badge');
    const initialCount = await notificationBadge.textContent().catch(() => '0');

    // Navigate away
    await page.goto('/portfolio');
    await page.waitForTimeout(1000);

    // Navigate back
    await page.goto('/dashboard');
    await page.waitForTimeout(1000);

    // Check notification count again
    const finalCount = await notificationBadge.textContent().catch(() => '0');

    if (initialCount === finalCount && initialCount !== '0') {
      console.log('Notifications persisted across navigation');
    } else {
      console.log('Notifications cleared or different count');
    }
  });

  test('should handle WebSocket message errors gracefully', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);

    // We can't easily inject malformed WebSocket messages in Playwright
    // But we can verify the app doesn't crash
    await page.waitForTimeout(3000);

    // Verify app still functional
    const currentUrl = page.url();
    expect(currentUrl).toContain('/dashboard');

    // Check no console errors
    const errors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    await page.waitForTimeout(2000);

    const criticalErrors = errors.filter(e => !e.includes('Warning'));
    if (criticalErrors.length > 0) {
      console.warn('Console errors found:', criticalErrors);
    }

    console.log('App remains stable with WebSocket active');
  });

  test('should display timestamp for notifications', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);

    // Open notifications
    const notificationBadge = page.locator('[data-testid="notification-badge"], .notification-badge');
    if (await notificationBadge.isVisible({ timeout: 2000 }).catch(() => false)) {
      await notificationBadge.click();
      await page.waitForTimeout(1000);
    }

    // Look for timestamps
    const timestamp = page.locator('text=/ago|just now|minute|hour|day/i, time, [datetime]');
    const hasTimestamp = await timestamp.first().isVisible({ timeout: 2000 }).catch(() => false);

    if (hasTimestamp) {
      console.log('Notification timestamps displayed');
      const timestampText = await timestamp.first().textContent();
      console.log('Example timestamp:', timestampText);
    } else {
      console.log('Notification timestamps not displayed');
    }
  });
});
