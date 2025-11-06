import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright config for manual server mode
 *
 * Use this when running tests against a manually-started dev server:
 * 1. Start backend: cd backend && mvn spring-boot:run
 * 2. Start frontend: cd frontend && npm run dev
 * 3. Run tests: npx playwright test --config=playwright.config.manual.ts
 */
export default defineConfig({
  testDir: './tests/e2e',

  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',

  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  // No webServer - server must be started manually
});
