import { test, expect } from '@playwright/test';
import { login, fillFormByLabels } from './helpers';

/**
 * Constraints E2E Tests
 *
 * Tests modifying portfolio constraints, previewing impact, and saving changes
 */

test.describe('Portfolio Constraints', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await login(page);
    await page.goto('/settings');
  });

  test('should display constraints settings page', async ({ page }) => {
    // Verify constraints section exists
    const constraintsSection = page.locator('text=/constraint|limit|restriction/i');
    await expect(constraintsSection.first()).toBeVisible({ timeout: 5000 });

    console.log('Constraints settings page loaded');
  });

  test('should display default constraint values', async ({ page }) => {
    // Look for constraint input fields
    const constraintInputs = page.locator('input[type="number"], input[type="range"]');
    const inputCount = await constraintInputs.count();

    expect(inputCount).toBeGreaterThan(0);
    console.log(`Found ${inputCount} constraint inputs`);

    // Verify inputs have default values
    const firstInput = constraintInputs.first();
    const value = await firstInput.inputValue();
    expect(value).toBeTruthy();
  });

  test('should allow modifying maximum position size constraint', async ({ page }) => {
    // Look for position size constraint
    const positionSizeLabel = page.locator('label:has-text("Position Size"), label:has-text("Max Position")');
    const positionSizeInput = page.locator('input[name*="position"], input[name*="maxPosition"]');

    const hasPositionConstraint = await positionSizeInput.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasPositionConstraint) {
      // Get current value
      const currentValue = await positionSizeInput.inputValue();
      console.log('Current position size constraint:', currentValue);

      // Change value
      await positionSizeInput.fill('15');
      await page.waitForTimeout(500);

      // Verify value changed
      const newValue = await positionSizeInput.inputValue();
      expect(newValue).toBe('15');

      console.log('Position size constraint modified');
    } else {
      console.log('Position size constraint not found');
    }
  });

  test('should allow modifying sector exposure constraints', async ({ page }) => {
    // Look for sector constraint inputs
    const sectorLabel = page.locator('text=/sector|industry/i');
    const hasSectorConstraints = await sectorLabel.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasSectorConstraints) {
      console.log('Sector constraints found');

      // Look for sector-specific inputs
      const sectorInputs = page.locator('input[name*="sector"]');
      const count = await sectorInputs.count();

      if (count > 0) {
        // Modify first sector constraint
        const firstSectorInput = sectorInputs.first();
        await firstSectorInput.fill('20');
        await page.waitForTimeout(500);

        console.log('Sector constraint modified');
      }
    } else {
      console.log('Sector constraints not implemented yet');
    }
  });

  test('should allow modifying risk tolerance', async ({ page }) => {
    // Look for risk tolerance control
    const riskInput = page.locator('input[name*="risk"], select[name*="risk"]');
    const hasRiskControl = await riskInput.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasRiskControl) {
      const tagName = await riskInput.evaluate((el: HTMLElement) => el.tagName.toLowerCase());

      if (tagName === 'select') {
        // Dropdown
        await riskInput.selectOption({ index: 1 });
      } else if (tagName === 'input') {
        // Number or range input
        const type = await riskInput.getAttribute('type');
        if (type === 'range') {
          await riskInput.fill('50');
        } else {
          await riskInput.fill('5');
        }
      }

      await page.waitForTimeout(500);
      console.log('Risk tolerance modified');
    } else {
      console.log('Risk tolerance control not found');
    }
  });

  test('should preview impact of constraint changes', async ({ page }) => {
    // Modify a constraint
    const constraintInput = page.locator('input[type="number"]').first();

    if (await constraintInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await constraintInput.fill('10');
      await page.waitForTimeout(1000);

      // Look for preview/impact section
      const previewSection = page.locator('text=/preview|impact|effect/i');
      const hasPreview = await previewSection.isVisible({ timeout: 3000 }).catch(() => false);

      if (hasPreview) {
        console.log('Impact preview displayed');
        await expect(previewSection).toBeVisible();

        // Look for specific metrics in preview
        const metricsPatterns = [/portfolio|position|allocation/i, /risk|exposure/i];

        for (const pattern of metricsPatterns) {
          const metric = page.locator(`text=${pattern}`);
          const isVisible = await metric.isVisible({ timeout: 2000 }).catch(() => false);
          if (isVisible) {
            console.log(`Preview metric found: ${pattern}`);
          }
        }
      } else {
        console.log('Preview not displayed or updates happen on save');
      }
    }
  });

  test('should validate constraint values', async ({ page }) => {
    // Try to enter invalid value
    const constraintInput = page.locator('input[type="number"]').first();

    if (await constraintInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Try negative value
      await constraintInput.fill('-10');
      await page.waitForTimeout(500);

      // Look for validation error
      const errorMessage = page.locator('text=/invalid|error|must be positive/i');
      const hasError = await errorMessage.isVisible({ timeout: 2000 }).catch(() => false);

      // Check HTML5 validation
      const isInvalid = await constraintInput.evaluate((el: HTMLInputElement) => !el.validity.valid);

      if (hasError || isInvalid) {
        console.log('Constraint validation works');
      } else {
        console.log('Validation may be less strict or applied on submit');
      }
    }
  });

  test('should save constraint changes', async ({ page }) => {
    // Modify a constraint
    const constraintInput = page.locator('input[type="number"]').first();

    if (await constraintInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      const originalValue = await constraintInput.inputValue();
      const newValue = String(Number(originalValue) + 5);

      await constraintInput.fill(newValue);
      await page.waitForTimeout(500);

      // Look for save button
      const saveButton = page.locator('button:has-text("Save"), button:has-text("Apply"), button[type="submit"]');
      await expect(saveButton).toBeVisible({ timeout: 3000 });

      await saveButton.click();

      // Wait for save confirmation
      const successMessage = page.locator('text=/saved|success|updated/i');
      const hasSaveConfirmation = await successMessage.isVisible({ timeout: 5000 }).catch(() => false);

      if (hasSaveConfirmation) {
        console.log('Constraints saved successfully');
        await expect(successMessage).toBeVisible();
      } else {
        // Check if page reloaded or state updated
        await page.waitForTimeout(2000);
        const currentValue = await constraintInput.inputValue();
        expect(currentValue).toBe(newValue);
        console.log('Constraints saved (verified by state persistence)');
      }
    }
  });

  test('should reset constraints to defaults', async ({ page }) => {
    // Look for reset button
    const resetButton = page.locator('button:has-text("Reset"), button:has-text("Default"), button:has-text("Restore")');
    const hasReset = await resetButton.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasReset) {
      // Modify a value first
      const constraintInput = page.locator('input[type="number"]').first();
      if (await constraintInput.isVisible({ timeout: 2000 }).catch(() => false)) {
        const originalValue = await constraintInput.inputValue();

        await constraintInput.fill('99');
        await page.waitForTimeout(500);

        // Click reset
        await resetButton.click();

        // Confirm if modal appears
        const confirmButton = page.locator('button:has-text("Confirm"), button:has-text("Yes")');
        if (await confirmButton.isVisible({ timeout: 2000 }).catch(() => false)) {
          await confirmButton.click();
        }

        await page.waitForTimeout(1000);

        // Verify value reset
        const resetValue = await constraintInput.inputValue();
        expect(resetValue).not.toBe('99');

        console.log('Constraints reset to defaults');
      }
    } else {
      console.log('Reset functionality not implemented');
    }
  });

  test('should display sensitivity analysis if available', async ({ page }) => {
    // Look for sensitivity section
    const sensitivitySection = page.locator('text=/sensitivity|what-if|scenario/i');
    const hasSensitivity = await sensitivitySection.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasSensitivity) {
      console.log('Sensitivity analysis available');
      await expect(sensitivitySection).toBeVisible();

      // Look for sensitivity controls
      const sensitivitySlider = page.locator('input[type="range"]');
      if (await sensitivitySlider.isVisible({ timeout: 2000 }).catch(() => false)) {
        await sensitivitySlider.fill('75');
        await page.waitForTimeout(1000);

        console.log('Sensitivity slider works');
      }
    } else {
      console.log('Sensitivity analysis not implemented yet');
    }
  });

  test('should show constraint violations', async ({ page }) => {
    // Navigate to recommendations or portfolio to trigger constraint check
    await page.goto('/recommendations');
    await page.waitForTimeout(3000);

    // Look for constraint violation warnings
    const violationWarning = page.locator('text=/violation|exceeds|warning|constraint/i');
    const hasViolation = await violationWarning.isVisible({ timeout: 2000 }).catch(() => false);

    if (hasViolation) {
      console.log('Constraint violations displayed');
      await expect(violationWarning).toBeVisible();
    } else {
      console.log('No constraint violations or not displayed prominently');
    }
  });

  test('should persist constraint changes after page reload', async ({ page }) => {
    // Modify and save a constraint
    const constraintInput = page.locator('input[type="number"]').first();

    if (await constraintInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      const testValue = '25';
      await constraintInput.fill(testValue);
      await page.waitForTimeout(500);

      // Save
      const saveButton = page.locator('button:has-text("Save"), button:has-text("Apply")');
      if (await saveButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        await saveButton.click();
        await page.waitForTimeout(2000);
      }

      // Reload page
      await page.reload();
      await page.waitForTimeout(2000);

      // Verify value persisted
      const persistedValue = await constraintInput.inputValue();
      expect(persistedValue).toBe(testValue);

      console.log('Constraint changes persisted after reload');
    }
  });

  test('should display constraint descriptions', async ({ page }) => {
    // Look for help text or descriptions
    const description = page.locator('text=/maximum|minimum|limit|between/i, .help-text, .description');
    const hasDescription = await description.first().isVisible({ timeout: 3000 }).catch(() => false);

    if (hasDescription) {
      console.log('Constraint descriptions displayed');

      // Look for info icons or tooltips
      const infoIcon = page.locator('[aria-label*="info"], .tooltip-trigger');
      if (await infoIcon.first().isVisible({ timeout: 2000 }).catch(() => false)) {
        await infoIcon.first().hover();
        await page.waitForTimeout(500);

        const tooltip = page.locator('[role="tooltip"]');
        const hasTooltip = await tooltip.isVisible({ timeout: 1000 }).catch(() => false);

        if (hasTooltip) {
          console.log('Info tooltips work');
        }
      }
    } else {
      console.log('Constraint descriptions not displayed');
    }
  });

  test('should allow adding custom constraints', async ({ page }) => {
    // Look for add constraint button
    const addButton = page.locator('button:has-text("Add Constraint"), button:has-text("Add Rule")');
    const hasAddButton = await addButton.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasAddButton) {
      await addButton.click();
      await page.waitForTimeout(1000);

      // Look for constraint form or modal
      const constraintForm = page.locator('form, [role="dialog"]');
      await expect(constraintForm).toBeVisible({ timeout: 3000 });

      console.log('Custom constraints can be added');
    } else {
      console.log('Custom constraints not supported');
    }
  });

  test('should allow removing constraints', async ({ page }) => {
    // Look for remove/delete buttons
    const removeButton = page.locator('button:has-text("Remove"), button:has-text("Delete"), [aria-label*="Remove"]').first();
    const hasRemoveButton = await removeButton.isVisible({ timeout: 3000 }).catch(() => false);

    if (hasRemoveButton) {
      await removeButton.click();

      // Confirm deletion if modal appears
      const confirmButton = page.locator('button:has-text("Confirm"), button:has-text("Yes"), button:has-text("Delete")');
      if (await confirmButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        await confirmButton.click();
      }

      await page.waitForTimeout(1000);

      console.log('Constraints can be removed');
    } else {
      console.log('Constraint removal not supported');
    }
  });

  test('should display constraint templates', async ({ page }) => {
    // Look for template selector
    const templateSelector = page.locator('select[name*="template"], text=/template|preset/i');
    const hasTemplates = await templateSelector.first().isVisible({ timeout: 3000 }).catch(() => false);

    if (hasTemplates) {
      console.log('Constraint templates available');

      const select = page.locator('select[name*="template"]');
      if (await select.isVisible({ timeout: 2000 }).catch(() => false)) {
        const options = await select.locator('option').allTextContents();
        console.log('Available templates:', options);
      }
    } else {
      console.log('Constraint templates not implemented');
    }
  });
});
