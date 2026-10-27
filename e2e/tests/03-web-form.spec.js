// @ts-check
const { test, expect } = require('@playwright/test');
const { db } = require('./db');

test.describe('Direct-entry web form (/submit)', () => {
  test('submitting the form creates a user-origin draft and redirects to review', async ({ page }) => {
    const before = await db.eventCount();

    await page.goto('/submit');
    await expect(page.locator('h1')).toContainText('Submit an event');

    await page.locator('#title').fill('Farmers Market');
    await page.locator('#location').fill('Town Square');
    await page.locator('#postalCode').fill('10001');
    await page.locator('#details').fill('Fresh produce; info https://example.com/market');
    await page.getByRole('button', { name: /create draft/i }).click();

    // Redirects to the token-gated review page.
    await expect(page).toHaveURL(/\/review\/\d+\?token=/);
    await expect(page.locator('h1')).toContainText('Review your event');

    const m = page.url().match(/\/review\/(\d+)\?token=/);
    const id = Number(m[1]);

    const e = await db.event(id);
    expect(e, 'event row should exist').not.toBeNull();
    expect(e.origin_type).toBe('user');
    expect(e.submitted_by, 'web form has no WhatsApp user → submitted_by null').toBeNull();
    expect(e.status).toBe('draft');
    expect(e.title).toBe('Farmers Market');
    expect(e.location).toBe('Town Square');
    expect(e.postal_code).toBe('10001');
    expect(e.edit_token, 'reviewable via a generated token').not.toBeNull();

    expect(await db.eventCount()).toBe(before + 1);

    // The details URL is captured as metadata.
    const md = await db.metadata(id);
    expect(md.some((r) => r.kind === 'url' && r.value === 'https://example.com/market')).toBeTruthy();
  });
});
