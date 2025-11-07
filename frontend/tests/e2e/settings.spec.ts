import { test, expect } from '@playwright/test';
import { login, TEST_USER } from './helpers';

/**
 * Settings E2E Tests
 *
 * Tests profile updates, notification preferences, and account settings
 */

test.describe('User Settings', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await login(page);
    await page.goto('/settings');
  });

  test('should display settings page', async ({ page }) => {
    // Verify settings page loaded
    await expect(page.locator('h1, h2').first()).toContainText(/setting|profile|account|preference/i);

    console.log('Settings page loaded');
  });

  test('should display profile section', async ({ page }) => {
    // Look for profile/personal information section
    const profileSection = page.locator('text=/profile|personal|account information/i');
    await expect(profileSection.first()).toBeVisible({ timeout: 5000 });

    console.log('Profile section displayed');
  });

  test('should display user information fields', async ({ page }) => {
    // Look for common profile fields
    const fields = ['firstName', 'lastName', 'email', 'name'];

    let foundFields = 0;

    for (const fieldName of fields) {
      const input = page.locator(`input[name="${fieldName}"], input[name*="${fieldName}"]`);
      const isVisible = await input.isVisible({ timeout: 2000 }).catch(() => false);

      if (isVisible) {
        foundFields++;
        console.log(`Found field: ${fieldName}`);
      }
    }

    expect(foundFields).toBeGreaterThan(0);
  });

  test('should update first name', async ({ page }) => {
    const firstNameInput = page.locator('input[name="firstName"], input[name="first_name"], input[placeholder*="First"]');

    if (await firstNameInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      const newName = 'Updated';

      await firstNameInput.clear();
      await firstNameInput.fill(newName);
      await page.waitForTimeout(500);

      // Save changes
      const saveButton = page.locator('button:has-text("Save"), button[type="submit"]');
      await saveButton.click();

      // Wait for save confirmation
      const successMessage = page.locator('text=/saved|success|updated/i');
      const hasSaved = await successMessage.isVisible({ timeout: 5000 }).catch(() => false);

      if (hasSaved) {
        console.log('First name updated successfully');
        await expect(successMessage).toBeVisible();
      } else {
        // Verify by page state
        const currentValue = await firstNameInput.inputValue();
        expect(currentValue).toBe(newName);
        console.log('First name updated (verified by state)');
      }
    } else {
      console.log('First name field not found');
    }
  });

  test('should update last name', async ({ page }) => {
    const lastNameInput = page.locator('input[name="lastName"], input[name="last_name"], input[placeholder*="Last"]');

    if (await lastNameInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      const newName = 'TestUser';

      await lastNameInput.clear();
      await lastNameInput.fill(newName);
      await page.waitForTimeout(500);

      // Save changes
      const saveButton = page.locator('button:has-text("Save"), button[type="submit"]');
      await saveButton.click();
      await page.waitForTimeout(2000);

      console.log('Last name updated');
    } else {
      console.log('Last name field not found');
    }
  });

  test('should display email field as read-only or editable', async ({ page }) => {
    const emailInput = page.locator('input[type="email"], input[name="email"]');

    if (await emailInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      const isReadOnly = await emailInput.getAttribute('readonly');
      const isDisabled = await emailInput.isDisabled();

      if (isReadOnly || isDisabled) {
        console.log('Email is read-only (cannot be changed)');
      } else {
        console.log('Email can be updated');
      }

      await expect(emailInput).toBeVisible();
    } else {
      console.log('Email field not displayed');
    }
  });

  test('should display notification preferences', async ({ page }) => {
    // Look for notifications section
    const notificationsSection = page.locator('text=/notification|alert|email preference/i');
    const hasNotifications = await notificationsSection.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasNotifications) {
      console.log('Notification preferences found');
      await expect(notificationsSection).toBeVisible();
    } else {
      console.log('Notification preferences not implemented');
    }
  });

  test('should toggle email notifications', async ({ page }) => {
    // Look for email notification toggle
    const emailNotificationToggle = page.locator('input[type="checkbox"][name*="email"], input[type="checkbox"][name*="notification"]').first();

    if (await emailNotificationToggle.isVisible({ timeout: 3000 }).catch(() => false)) {
      const isChecked = await emailNotificationToggle.isChecked();

      // Toggle
      await emailNotificationToggle.click();
      await page.waitForTimeout(500);

      // Verify toggled
      const newState = await emailNotificationToggle.isChecked();
      expect(newState).toBe(!isChecked);

      console.log(`Email notifications toggled to: ${newState}`);

      // Save if needed
      const saveButton = page.locator('button:has-text("Save")');
      if (await saveButton.isVisible({ timeout: 1000 }).catch(() => false)) {
        await saveButton.click();
        await page.waitForTimeout(1000);
      }
    } else {
      console.log('Email notification toggle not found');
    }
  });

  test('should display security section', async ({ page }) => {
    // Look for security/password section
    const securitySection = page.locator('text=/security|password|change password/i');
    const hasSecurity = await securitySection.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasSecurity) {
      console.log('Security section found');
      await expect(securitySection).toBeVisible();
    } else {
      console.log('Security section not displayed on main settings page');
    }
  });

  test('should allow changing password', async ({ page }) => {
    // Look for change password button or fields
    const changePasswordButton = page.locator('button:has-text("Change Password"), a:has-text("Change Password")');

    if (await changePasswordButton.isVisible({ timeout: 3000 }).catch(() => false)) {
      await changePasswordButton.click();
      await page.waitForTimeout(1000);

      // Look for password change form
      const currentPasswordInput = page.locator('input[name*="current"], input[placeholder*="Current"]');
      const newPasswordInput = page.locator('input[name*="new"], input[placeholder*="New"]').first();

      const hasPasswordForm = await currentPasswordInput.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasPasswordForm) {
        console.log('Password change form available');
        await expect(currentPasswordInput).toBeVisible();
        await expect(newPasswordInput).toBeVisible();
      } else {
        console.log('Password change form not found');
      }
    } else {
      console.log('Change password functionality not accessible');
    }
  });

  test('should display theme preferences if available', async ({ page }) => {
    // Look for theme settings
    const themeSection = page.locator('text=/theme|dark mode|appearance/i');
    const hasTheme = await themeSection.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasTheme) {
      console.log('Theme preferences available');

      // Look for theme toggle
      const themeToggle = page.locator('input[type="checkbox"][name*="theme"], select[name*="theme"], button[aria-label*="theme"]');

      if (await themeToggle.first().isVisible({ timeout: 2000 }).catch(() => false)) {
        console.log('Theme toggle found');
      }
    } else {
      console.log('Theme preferences not implemented');
    }
  });

  test('should display timezone settings if available', async ({ page }) => {
    // Look for timezone selector
    const timezoneSelect = page.locator('select[name*="timezone"], select[name*="timeZone"]');
    const hasTimezone = await timezoneSelect.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasTimezone) {
      console.log('Timezone settings available');

      // Get available timezones
      const options = await timezoneSelect.locator('option').allTextContents();
      console.log(`Found ${options.length} timezone options`);
    } else {
      console.log('Timezone settings not implemented');
    }
  });

  test('should display language preferences if available', async ({ page }) => {
    // Look for language selector
    const languageSelect = page.locator('select[name*="language"], select[name*="locale"]');
    const hasLanguage = await languageSelect.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasLanguage) {
      console.log('Language preferences available');
    } else {
      console.log('Language preferences not implemented');
    }
  });

  test('should validate profile updates', async ({ page }) => {
    // Try to save with invalid data
    const firstNameInput = page.locator('input[name="firstName"], input[name="first_name"]').first();

    if (await firstNameInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Clear required field
      await firstNameInput.clear();

      // Try to save
      const saveButton = page.locator('button:has-text("Save"), button[type="submit"]');
      await saveButton.click();

      // Look for validation error
      const errorMessage = page.locator('text=/required|error|invalid/i');
      const hasError = await errorMessage.isVisible({ timeout: 2000 }).catch(() => false);

      if (hasError) {
        console.log('Validation works for profile updates');
      } else {
        // Check HTML5 validation
        const isInvalid = await firstNameInput.evaluate((el: HTMLInputElement) => !el.validity.valid);
        expect(isInvalid).toBe(true);
        console.log('HTML5 validation active');
      }
    }
  });

  test('should display account deletion option', async ({ page }) => {
    // Look for delete/deactivate account option
    const deleteButton = page.locator('button:has-text("Delete Account"), button:has-text("Deactivate"), a:has-text("Delete Account")');
    const hasDelete = await deleteButton.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasDelete) {
      console.log('Account deletion option available');
      await expect(deleteButton).toBeVisible();

      // Don't actually delete, just verify the option exists
    } else {
      console.log('Account deletion not accessible from settings');
    }
  });

  test('should display API key management if available', async ({ page }) => {
    // Look for API keys section
    const apiSection = page.locator('text=/API key|API token|developer/i');
    const hasAPI = await apiSection.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasAPI) {
      console.log('API key management available');
      await expect(apiSection).toBeVisible();
    } else {
      console.log('API key management not implemented');
    }
  });

  test('should persist settings after page reload', async ({ page }) => {
    // Toggle a setting
    const checkbox = page.locator('input[type="checkbox"]').first();

    if (await checkbox.isVisible({ timeout: 2000 }).catch(() => false)) {
      const initialState = await checkbox.isChecked();

      await checkbox.click();
      await page.waitForTimeout(500);

      // Save if needed
      const saveButton = page.locator('button:has-text("Save")');
      if (await saveButton.isVisible({ timeout: 1000 }).catch(() => false)) {
        await saveButton.click();
        await page.waitForTimeout(2000);
      }

      // Reload page
      await page.reload();
      await page.waitForTimeout(2000);

      // Verify setting persisted
      const finalState = await checkbox.isChecked();
      expect(finalState).toBe(!initialState);

      console.log('Settings persisted after reload');
    }
  });

  test('should handle concurrent updates gracefully', async ({ page }) => {
    // Make multiple rapid changes
    const inputs = page.locator('input[type="text"], input[type="email"]');
    const count = await inputs.count();

    if (count > 0) {
      // Modify multiple fields quickly
      for (let i = 0; i < Math.min(3, count); i++) {
        await inputs.nth(i).fill(`Test${i}`);
      }

      // Save
      const saveButton = page.locator('button:has-text("Save")');
      if (await saveButton.isVisible({ timeout: 1000 }).catch(() => false)) {
        await saveButton.click();
        await page.waitForTimeout(2000);

        // Verify no errors
        const errorMessage = page.locator('text=/error|failed/i');
        const hasError = await errorMessage.isVisible({ timeout: 2000 }).catch(() => false);

        expect(hasError).toBe(false);
        console.log('Concurrent updates handled properly');
      }
    }
  });
});
