// Fetches REAL events from the Spring Boot discovery endpoint and maps them to the
// SPA's event shape. In dev, Vite proxies /api to :8080 (vite.config.js). This lets the
// list show database events (e.g. the uploaded Garba/Dandiya flyer) alongside the examples.

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
 * Record demand for an uncovered town ("Notify me"). Returns true only if the server saved it.
 * NOTE: the backend endpoint POST /api/waitlist does not exist yet — until it does, this returns
 * false and the landing shows an error instead of claiming the email was saved.
 */
export async function joinWaitlist(town, email) {
  try {
    const res = await fetch('/api/waitlist', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ town, email }),
    })
    return res.ok
  } catch {
    return false
  }
}
