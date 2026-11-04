// @ts-check
const { test, expect, request } = require('@playwright/test')
const { db } = require('./db')

/**
 * P2.1 Town Entry & Coverage (ARANG-TTP-P2-1, task T18). Drives the real SPA + API on the dev app and
 * verifies rows in the arang_dev database. Covers: town-name search, digits hint, live vs coming-soon,
 * the waitlist save and its persistence, remember-town, and change-town clearing it.
 * Run with ARANG_BASE_URL=http://localhost:8081 and ARANG_DB_NAME=arang_dev.
 */

test.describe('P2.1 town entry', () => {
  test('town-name search shows a live town and opens it', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('body')).toContainText('Arangly')
    const input = page.locator('.town-picker input, input[aria-label="Find your town"], input').first()
    await input.fill('ashland')
    // a suggestion for Ashland appears and opens the town
    const suggestion = page.locator('.suggest button, .town-chip', { hasText: 'Ashland' }).first()
    await expect(suggestion).toBeVisible({ timeout: 5000 })
    await suggestion.click()
    await expect(page).toHaveURL(/\/towns\/ashland-ma/)
  })

  test('digits-only input shows the type-a-town hint, not "not found"', async ({ page }) => {
    await page.goto('/')
    const input = page.locator('input').first()
    await input.fill('01721')
    await page.waitForTimeout(600)
    const body = await page.innerText('body')
    expect(/type a town name/i.test(body)).toBeTruthy()
    expect(/not found in massachusetts/i.test(body)).toBeFalsy()
  })

  test('a real but not-live town offers the coming-soon waitlist and it persists', async ({ page }) => {
    const email = `e2e-${Date.now()}@example.com`
    await page.goto('/')
    const input = page.locator('input').first()
    await input.fill('glouce')
    const gloucester = page.locator('.suggest button', { hasText: 'Gloucester' }).first()
    await expect(gloucester).toBeVisible({ timeout: 5000 })
    await gloucester.click()
    // coming-soon form
    const emailField = page.locator('input[type="email"], .soon-row input').first()
    await expect(emailField).toBeVisible()
    await emailField.fill(email)
    await page.locator('.soon-row button, button', { hasText: /notify/i }).first().click()
    // "you're on the list" appears only after the save
    await expect(page.locator('body')).toContainText(/on the list/i, { timeout: 8000 })
    // the row exists in the database
    await expect.poll(async () => db.waitlistCountForEmail(email), { timeout: 8000 }).toBeGreaterThan(0)
  })

  test('remember-town opens the town on return, and change-town clears it', async ({ page }) => {
    // Choose Ashland through the UI so the SPA saves it to localStorage.
    await page.goto('/')
    const input = page.locator('input').first()
    await input.fill('ashland')
    await page.locator('.suggest button', { hasText: 'Ashland' }).first().click()
    await expect(page).toHaveURL(/\/towns\/ashland-ma/)
    // return to root → remembered town opens
    await page.goto('/')
    await expect(page).toHaveURL(/\/towns\/ashland-ma/, { timeout: 5000 })
    // change town → landing, and a reload stays on the landing
    await page.getByRole('button', { name: /change town/i }).first().click()
    await expect(page).toHaveURL(/\/$/, { timeout: 5000 })
    await page.goto('/')
    await expect(page).toHaveURL(/\/$/)
  })
})
