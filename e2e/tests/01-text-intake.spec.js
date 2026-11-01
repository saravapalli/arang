// @ts-check
const { test, expect } = require('@playwright/test');
const { db } = require('./db');

// Parse "/review/{id}?token=xyz" out of the console's "Open review page" link.
function parseReviewHref(href) {
  const m = href.match(/\/review\/(\d+)\?token=([^"&]+)/);
  if (!m) throw new Error('could not parse review href: ' + href);
  return { id: Number(m[1]), token: decodeURIComponent(m[2]) };
}

test.beforeAll(async () => {
  // Preflight: make sure MySQL is reachable, else fail fast with a clear message.
  await db.ping();
});

test.describe('Producer console — text intake', () => {
  test('typing an event creates a draft persisted with provenance + URL metadata', async ({ page }) => {
    await page.goto('/console');
    await expect(page.locator('h1')).toContainText('Arang Producer Console');

    const msg = 'Live jazz Friday 8pm at The Cellar, tickets https://example.com/jazz';
    await page.locator('#message').fill(msg);
    await page.locator('#send').click();

    // The console posts, then renders a reply bubble with an "Open review page →" link.
    const reviewLink = page.locator('a.reviewbtn', { hasText: 'Open review page' });
    await expect(reviewLink).toBeVisible();
    const href = await reviewLink.getAttribute('href');
    const { id, token } = parseReviewHref(href);

    // --- DB verification: the draft row ---
    const e = await db.event(id);
    expect(e, 'event row should exist').not.toBeNull();
    expect(e.status).toBe('draft');
    expect(e.source_type).toBe('text');
    expect(e.origin_type).toBe('user');
    expect(e.raw_message).toBe(msg);            // raw captured verbatim
    expect(e.edit_token).toBe(token);           // token in the link matches the row
    expect(e.edit_token_expires_at).not.toBeNull();
    expect(e.submitted_by, 'submitted_by FK set').not.toBeNull();
    expect(e.country_code).toBe('US');

    // --- provenance: the user row the FK points at ---
    const u = await db.user(e.submitted_by);
    expect(u, 'submitter user row should exist').not.toBeNull();

    // --- URL extracted to event_metadata verbatim ---
    const md = await db.metadata(id);
    const urlRow = md.find((r) => r.kind === 'url');
    expect(urlRow, 'a url metadata row should exist').toBeTruthy();
    expect(urlRow.value).toBe('https://example.com/jazz');
  });

  test('the review page renders the draft for a valid token', async ({ page }) => {
    await page.goto('/console');
    await page.locator('#message').fill('Open mic night https://example.com/mic');
    await page.locator('#send').click();
    const href = await page.locator('a.reviewbtn').first().getAttribute('href');
    const { id, token } = parseReviewHref(href);

    const resp = await page.goto(`/review/${id}?token=${token}`);
    expect(resp.status()).toBe(200);
    await expect(page.locator('h1')).toContainText('Review your event');
    // postal field present (required to publish)
    await expect(page.locator('#postalCode')).toBeVisible();
  });

  test('an invalid token is rejected (404)', async ({ page }) => {
    await page.goto('/console');
    await page.locator('#message').fill('Something https://example.com/x');
    await page.locator('#send').click();
    const href = await page.locator('a.reviewbtn').first().getAttribute('href');
    const { id } = parseReviewHref(href);

    const resp = await page.goto(`/review/${id}?token=WRONGTOKEN`);
    expect(resp.status()).toBe(404);
  });
});
