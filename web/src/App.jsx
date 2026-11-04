import { useEffect, useState } from 'react'
import Landing from './components/Landing.jsx'
import Discovery from './components/Discovery.jsx'
import ProducerScreen from './components/ProducerScreen.jsx'
import { currentRoute, navigate, subscribe } from './router.js'
import { getSiteConfig, loadSiteConfig, findTown } from './siteConfig.js'

// localStorage key that remembers the last chosen town. It is a client-only convenience:
// its value is NEVER sent to the server (REQ-P2.1-16 / AC-P2.1-16). Only the normal
// /towns and /api/discovery requests happen.
const TOWN_KEY = 'arangly.town'

function rememberedSlug() {
  try { return window.localStorage.getItem(TOWN_KEY) } catch { return null }
}
function rememberTown(slug) {
  try { window.localStorage.setItem(TOWN_KEY, slug) } catch { /* ignore */ }
}
function forgetTown() {
  try { window.localStorage.removeItem(TOWN_KEY) } catch { /* ignore */ }
}

// Routed consumer flow (P2.1 T15):
//   /            landing (or auto-forward to a remembered, still-live town)
//   /towns/<s>   discovery for the live town <s>
//   /add         producer intake screen
export default function App() {
  const [route, setRoute] = useState(() => currentRoute())
  // getSiteConfig() returns the embedded config synchronously when present.
  const [config, setConfig] = useState(() => getSiteConfig())

  // Re-render whenever the route changes (navigate() or browser back/forward).
  useEffect(() => subscribe(setRoute), [])

  // Load site-config: embedded first, async fetch as the fallback.
  useEffect(() => {
    if (config) return
    let live = true
    loadSiteConfig().then((cfg) => { if (live && cfg) setConfig(cfg) })
    return () => { live = false }
  }, [config])

  // On the landing route, auto-forward to a remembered town only if it is still a LIVE
  // town in site-config. A removed/hidden slug is ignored (and must not bounce after
  // "Change town", which clears the key before navigating to "/").
  useEffect(() => {
    if (route.screen !== 'landing' || !config) return
    const slug = rememberedSlug()
    if (slug && findTown(config, slug)) {
      navigate(`/towns/${slug}`)
    }
  }, [route.screen, config])

  if (route.screen === 'producer') {
    return <ProducerScreen />
  }

  if (route.screen === 'town') {
    const town = config ? findTown(config, route.slug) : null
    // A slug that is not a live town falls back to the landing (AC: unknown slug -> Landing).
    if (town) {
      return (
        <Discovery
          town={town}
          onChangeTown={() => { forgetTown(); navigate('/') }}
        />
      )
    }
    return <Landing config={config} onChoose={(slug) => { rememberTown(slug); navigate(`/towns/${slug}`) }} />
  }

  // Landing. If a remembered live town exists, the effect above forwards to it; render
  // Landing meanwhile (and when there is no remembered town).
  return <Landing config={config} onChoose={(slug) => { rememberTown(slug); navigate(`/towns/${slug}`) }} />
}
