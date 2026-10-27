// @ts-check
const { defineConfig } = require('@playwright/test');

/**
 * Arang E2E config. Tests drive the real UI on http://localhost:8080 and then verify rows in the
 * MySQL `arang` database via a mysql2 helper. Run the app + MySQL first (see README.md).
 *
 * Overridable via env: ARANG_BASE_URL, ARANG_DB_HOST/PORT/USER/PASSWORD/NAME.
 */
module.exports = defineConfig({
  testDir: './tests',
  timeout: 30_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,          // tests share one DB; run serially for deterministic assertions
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.ARANG_BASE_URL || 'http://localhost:8080',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } },
  ],
});
