// @ts-check
const { test, expect } = require('@playwright/test');
const { db } = require('./db');

function parseReviewHref(href) {
  const m = href.match(/\/review\/(\d+)\?token=([^"&]+)/);
  if (!m) throw new Error('could not parse review href: ' + href);
  return { id: Number(m[1]), token: decodeURIComponent(m[2]) };
}

// Create a fresh draft via the console and return its id+token.
async function newDraft(page, message) {
  await page.goto('/');
  await page.locator('#message').fill(message);
  await page.locator('#send').click();
  const href = await page.locator('a.reviewbtn').first().getAttribute('href');
  return parseReviewHref(href);
}

test.describe('Review page — edit / publish / delete', () => {
  test('editing fields on the review page persists to the DB', async ({ page }) => {
    const { id, token } = await newDraft(page, 'Need details https://example.com/e');

    await page.goto(`/review/${id}?token=${token}`);
    await page.locator('#title').fill('Edited Jazz Night');
    await page.locator('#location').fill('Downtown Hall');
    await page.locator('#postalCode').fill('10001');
    await page.getByRole('button', { name: /save changes/i }).click();

    const e = await db.event(id);
    expect(e.title).toBe('Edited Jazz Night');
    expect(e.location).toBe('Downtown Hall');
    expect(e.postal_code).toBe('10001');
  });

  test('publishing requires a postal code, then sets published + derives geo', async ({ page }) => {
    const { id, token } = await newDraft(page, 'Gala night https://example.com/g');
    await page.goto(`/review/${id}?token=${token}`);

    // Submit WITHOUT a postal code → stays draft, shows an error.
    await page.getByRole('button', { name: /submit|publish/i }).click();
    let e = await db.event(id);
    expect(e.status, 'no postal → still draft').toBe('draft');

    // Add a known postal (10001 is in the bundled centroid set) → publish succeeds + geo derived.
    await page.goto(`/review/${id}?token=${token}`);
    await page.locator('#postalCode').fill('10001');
    await page.getByRole('button', { name: /save changes/i }).click();
    await page.goto(`/review/${id}?token=${token}`);
    await page.getByRole('button', { name: /submit|publish/i }).click();

    e = await db.event(id);
    expect(e.status).toBe('published');
    expect(e.postal_code).toBe('10001');
    expect(e.latitude, 'latitude derived from postal centroid').not.toBeNull();
    expect(e.longitude, 'longitude derived from postal centroid').not.toBeNull();
  });

  test('deleting removes the event + metadata and the token stops resolving', async ({ page }) => {
    const { id, token } = await newDraft(page, 'Temp event https://example.com/t');
    expect(await db.event(id)).not.toBeNull();

    await page.goto(`/review/${id}?token=${token}`);
    await page.getByRole('button', { name: /delete/i }).click();

    // Row gone + metadata gone.
    expect(await db.event(id), 'event row deleted').toBeNull();
    expect(await db.metadata(id), 'metadata deleted').toHaveLength(0);

    // Token no longer resolves.
    const resp = await page.goto(`/review/${id}?token=${token}`);
    expect(resp.status()).toBe(404);
  });
});
