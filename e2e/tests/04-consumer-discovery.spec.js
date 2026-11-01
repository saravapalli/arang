// @ts-check
const { test, expect, request } = require('@playwright/test')
const { db } = require('./db')

/**
 * T13 / TC-16, TC-17, TC-21 — Consumer Discovery SPA E2E over the same-origin app (:8080).
 * Drives the two-screen flow (landing → discovery), asserts the list + OSM map render, the detail
 * modal + Google Maps link work, the interest (heart) save persists via the API, and verifies the
 * published event is served by /api/discovery. Requires the app + MySQL running with the SPA built
 * into static/ (./gradlew copySpa -PbuildSpa ...). A published Ashland event (e.g. the Garba flyer)
 * must exist; the spec checks the discovery API and skips gracefully if none is present.
 */

test.beforeAll(async () => {
  await db.ping()
})

test.describe('Consumer discovery SPA', () => {
  test('two-screen flow: choose a town, see events on the map, open detail, save interest', async ({ page, baseURL }) => {
    // Preflight: does the Ashland discovery query return at least one usable event?
    const api = await request.newContext({ baseURL })
    const res = await api.get('/api/discovery?location=01721')
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    const usable = (body.results || []).filter((r) => r.title && r.lat != null && r.lng != null)
    test.skip(usable.length === 0, 'no published+geocoded Ashland event seeded; skipping UI assertions')

    // --- SCREEN 1: landing (town chooser) ---
    await page.goto('/')
    await expect(page.locator('.landing h1')).toContainText('your town')
    // choose Ashland, MA
    await page.locator('.chip', { hasText: 'Ashland' }).click()

    // --- SCREEN 2: discovery (list + OSM map) --- (TC-16)
    await expect(page.locator('.discovery-shell')).toBeVisible()
    await expect(page.locator('.leaflet-map')).toBeVisible()
    // the real event appears in the rail
    const firstCard = page.locator('.event-card').first()
    await expect(firstCard).toBeVisible()
    // OSM tiles actually load
    await expect(page.locator('.leaflet-tile').first()).toBeVisible({ timeout: 10_000 })

    // --- detail modal + Google Maps link --- (TC-17)
    await firstCard.click()
    const modal = page.locator('.detail-modal')
    await expect(modal).toBeVisible()
    const maps = modal.locator('a.maps-link')
    await expect(maps).toHaveAttribute('href', /google\.com\/maps/)
    // close
    await page.locator('.modal-image button').click()

    // --- interest save (heart) persists via /api/interests --- (TC-17/TC-21)
    // Use the PAGE's own request context so the arang_uid cookie set by the heart click is sent.
    await firstCard.locator('.card-save').click()
    await expect.poll(async () => {
      const r = await page.request.get('/api/interests')
      const j = await r.json()
      return (j.categories || []).length
    }, { timeout: 8000 }).toBeGreaterThan(0)

    await api.dispose()
  })

  test('discovery API serves only published events for the town', async ({ baseURL }) => { // TC-21 (DB-adjacent)
    const api = await request.newContext({ baseURL })
    const res = await api.get('/api/discovery?location=01721')
    const body = await res.json()
    for (const r of body.results || []) {
      // every returned event must be published in the DB
      const row = await db.event(r.id)
      expect(row, `event ${r.id} exists`).not.toBeNull()
      expect(row.status).toBe('published')
    }
    await api.dispose()
  })
})
