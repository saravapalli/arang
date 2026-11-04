// Backend API client for the discovery SPA. In dev, Vite proxies /api to the Boot app
// (vite.config.js). The SET of towns now comes from site-config (siteConfig.js), not from
// the hard-coded locations list. We keep data/events.js only for photos + category labels.

const CATEGORY_LABEL = {
  music: 'Music', food: 'Food', family: 'Family', arts: 'Arts',
  outdoor: 'Outdoors', outdoors: 'Outdoors', wellness: 'Wellness', community: 'Family',
}
const CATEGORY_COLOR = {
  Music: '#ff694a', Food: '#43a875', Family: '#f2ac3c', Arts: '#a86de0',
  Outdoors: '#3189aa', Wellness: '#496ee8',
}

function mapOne(r) {
  // Derive day/date/time from the ISO startsAt.
  let day = '', date = '', time = ''
  if (r.startsAt) {
    const d = new Date(r.startsAt)
    day = d.toLocaleDateString('en-US', { weekday: 'short' }).toUpperCase()
    date = String(d.getDate())
    time = d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })
  }
  const rawCat = (r.categories && r.categories[0]) || 'community'
  const category = CATEGORY_LABEL[rawCat] || 'Family'
  return {
    id: r.id,
    title: r.title,
    category,
    day, date, time,
    place: r.location || '',
    distance: '', // real distance needs the query centroid (S-B)
    price: r.price || 'See flyer',
    lat: r.lat,
    lng: r.lng,
    color: CATEGORY_COLOR[category] || '#ff694a',
    image: r.image, // same-origin path served from the SPA public/ folder
    fromDb: true,
  }
}

export async function fetchRealEvents(postal, window) {
  try {
    const params = new URLSearchParams()
    if (postal) params.set('location', postal)
    if (window && window.from) params.set('from', window.from)
    if (window && window.to) params.set('to', window.to)
    const qs = params.toString()
    const res = await fetch(`/api/discovery${qs ? '?' + qs : ''}`)
    if (!res.ok) return []
    const body = await res.json()
    const rows = Array.isArray(body) ? body : (body.results || [])
    // Only usable rows: must have a title and real coordinates to pin + display.
    return rows.filter((r) => r.title && r.lat != null && r.lng != null).map(mapOne)
  } catch {
    return [] // backend not running → SPA just shows the example towns
  }
}

// --- Interests (REQ-10..12). Cookie-based identity is handled server-side (arang_uid). ---

const CAT_TO_VOCAB = {
  Music: 'music', Food: 'food', Family: 'family', Arts: 'arts',
  Outdoors: 'outdoor', Wellness: 'wellness',
}

/** Read the consumer's saved interest categories (vocabulary names). */
export async function getInterests() {
  try {
    const res = await fetch('/api/interests', { credentials: 'same-origin' })
    if (!res.ok) return []
    const body = await res.json()
    return body.categories || []
  } catch {
    return []
  }
}

/** Save the given interest categories (vocabulary names). Returns the saved list. */
export async function saveInterests(vocabCategories) {
  try {
    const res = await fetch('/api/interests', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ categories: vocabCategories }),
    })
    if (!res.ok) return null
    const body = await res.json()
    return body.categories || []
  } catch {
    return null
  }
}

/** Map a display category label (e.g. "Music") to a curated vocabulary name (e.g. "music"). */
export function toVocab(label) {
  return CAT_TO_VOCAB[label] || 'other'
}

// --- Time-window presets (UF-P2-1). Values are YYYY-MM-DD, matching the API's date parsing. ---
function iso(d) { return d.toISOString().slice(0, 10) }
function startOfWeek(base) { // Monday
  const d = new Date(base); const day = (d.getDay() + 6) % 7; d.setDate(d.getDate() - day); return d
}
export const WINDOW_PRESETS = [
  { id: 'this-week', label: 'This week' },
  { id: 'this-weekend', label: 'This weekend' },
  { id: 'next-week', label: 'Next week' },
  { id: 'this-month', label: 'This month' },
]
/** Resolve a preset id → { from, to } (or null for the default, which the API treats as this week). */
export function windowFor(presetId) {
  const now = new Date()
  if (presetId === 'this-week' || !presetId) {
    const mon = startOfWeek(now); const sun = new Date(mon); sun.setDate(mon.getDate() + 6)
    return { from: iso(mon), to: iso(sun) }
  }
  if (presetId === 'this-weekend') {
    const mon = startOfWeek(now); const sat = new Date(mon); sat.setDate(mon.getDate() + 5)
    const sun = new Date(mon); sun.setDate(mon.getDate() + 6)
    return { from: iso(sat), to: iso(sun) }
  }
  if (presetId === 'next-week') {
    const mon = startOfWeek(now); mon.setDate(mon.getDate() + 7)
    const sun = new Date(mon); sun.setDate(mon.getDate() + 6)
    return { from: iso(mon), to: iso(sun) }
  }
  if (presetId === 'this-month') {
    const first = new Date(now.getFullYear(), now.getMonth(), 1)
    const last = new Date(now.getFullYear(), now.getMonth() + 1, 0)
    return { from: iso(first), to: iso(last) }
  }
  return null
}

/** Build a Google Maps link for an event. The street address/venue text wins: stored coordinates are only a ZIP centroid (approximate), so linking to them sends people to the wrong spot. */
export function googleMapsUrl(ev) {
  const text = (ev.place || '').trim()
  if (text) {
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(text)}`
  }
  if (ev.lat != null && ev.lng != null) {
    return `https://www.google.com/maps/search/?api=1&query=${ev.lat},${ev.lng}`
  }
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(ev.title || '')}`
}

/**
 * Record demand for an uncovered place ("Notify me"). POST /api/waitlist (P2.1 T16).
 * `place` is a town slug OR free text (the typed town name).
 * Returns { ok, status } where status is the parsed response body so the caller can tell
 * the outcomes apart:
 *   200 -> { ok:true,  status:{ status:'saved' } }
 *   400 -> { ok:false, status:{ errors:{ email|place:'invalid' } } }
 *   429 -> { ok:false, status:{ retryAfterSeconds:N } }
 *   500 -> { ok:false, status:{ error:'not_saved' } }
 * On a network failure, ok is false and status is { error:'network' }.
 */
export async function joinWaitlist(place, email) {
  try {
    const res = await fetch('/api/waitlist', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ place, email }),
    })
    let body = {}
    try { body = await res.json() } catch { /* body may be empty */ }
    return { ok: res.ok, status: body, httpStatus: res.status }
  } catch {
    return { ok: false, status: { error: 'network' }, httpStatus: 0 }
  }
}

/**
 * Search live + coming-soon places. GET /api/places?q=<text> (P2.1 T16).
 * Returns the parsed body unchanged so the caller can branch on each case:
 *   200 -> { results:[{slug,name,state,live,exact}] }
 *   200 -> { results:[], hint:'type_town_name' }   (digits-only input)
 *   400 -> { error:'q_invalid' }                   (empty or > 80 chars)
 *   503 -> { error:'geo_unavailable' }              (place data down)
 * On a network failure, returns { error:'network' }.
 */
export async function searchPlaces(q) {
  try {
    const res = await fetch(`/api/places?q=${encodeURIComponent(q)}`, { credentials: 'same-origin' })
    let body = {}
    try { body = await res.json() } catch { /* ignore */ }
    return body
  } catch {
    return { error: 'network' }
  }
}

/**
 * Nearby towns for the discovery empty state. GET /api/towns/{slug}/nearby (P2.1 T17).
 * Returns an array of up to 3 towns [{slug,name,state,distanceMiles,eventCount}].
 * On 404 (unknown/hidden town) or any error, returns [].
 */
export async function fetchNearby(slug) {
  try {
    const res = await fetch(`/api/towns/${encodeURIComponent(slug)}/nearby`, { credentials: 'same-origin' })
    if (!res.ok) return []
    const body = await res.json()
    return Array.isArray(body.towns) ? body.towns : []
  } catch {
    return []
  }
}
