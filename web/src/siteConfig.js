// Site-config access (P2.1 T14). The server embeds the config in every HTML page as
//   <script id="site-config" type="application/json">...</script>
// Prefer that embedded element (no network round-trip, works offline for the shell).
// Fall back to GET /api/site-config when the element is absent — e.g. under the Vite dev
// server, which serves a bare index.html with no server shell.
//
// Shape (from the backend):
//   { towns:[{slug,name,state,postal,lat,lng,zoom,popularRank}], nearMeMiles,
//     consent:{version,text}, credits:[...], geo:"UP"|"UNAVAILABLE", publicUrl }

let cached = null // last successfully resolved config (embedded or fetched)

// Read and parse the embedded #site-config element. Returns the config or null.
function readEmbedded() {
  const el = typeof document !== 'undefined' && document.getElementById('site-config')
  if (!el) return null
  try {
    const cfg = JSON.parse(el.textContent)
    return cfg && typeof cfg === 'object' ? cfg : null
  } catch {
    return null // malformed embed: treat as absent, let the async loader fetch instead
  }
}

// Synchronous accessor. Returns the embedded config, or the cached one, or null.
// Null means "not available yet" — the caller should await loadSiteConfig().
export function getSiteConfig() {
  if (cached) return cached
  const embedded = readEmbedded()
  if (embedded) cached = embedded
  return cached
}

// Async accessor. Returns the embedded config if present, else fetches /api/site-config.
// Caches the first success so later calls do not refetch.
export async function loadSiteConfig() {
  if (cached) return cached
  const embedded = readEmbedded()
  if (embedded) { cached = embedded; return cached }
  try {
    const res = await fetch('/api/site-config', { credentials: 'same-origin' })
    if (!res.ok) return null
    const cfg = await res.json()
    cached = cfg
    return cached
  } catch {
    return null // backend unreachable: callers degrade gracefully
  }
}

// --- Convenience selectors. Each tolerates a null/partial config. ---

export function townsOf(cfg) {
  return (cfg && Array.isArray(cfg.towns)) ? cfg.towns : []
}

export function nearMeMilesOf(cfg) {
  return (cfg && typeof cfg.nearMeMiles === 'number') ? cfg.nearMeMiles : 100
}

export function consentOf(cfg) {
  return (cfg && cfg.consent) ? cfg.consent : { version: '', text: '' }
}

export function creditsOf(cfg) {
  return (cfg && Array.isArray(cfg.credits)) ? cfg.credits : []
}

export function geoOf(cfg) {
  return (cfg && cfg.geo) ? cfg.geo : 'UNAVAILABLE'
}

// Find a live town by slug in the config. Returns the town object or null.
export function findTown(cfg, slug) {
  if (!slug) return null
  return townsOf(cfg).find((t) => t.slug === slug) || null
}
