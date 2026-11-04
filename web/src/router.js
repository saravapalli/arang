// Tiny path router (P2.1 T14, DD-P2.1-10). No router library.
// Parses window.location.pathname into one of three screens:
//   "/" or ""            => { screen: 'landing' }
//   "/towns/<slug>"      => { screen: 'town', slug }
//   "/add"               => { screen: 'producer' }
// Anything else falls back to the landing screen.

// Parse a pathname string into a route descriptor.
export function parsePath(pathname) {
  const path = (pathname || '/').replace(/\/+$/, '') || '/' // strip trailing slash
  if (path === '/' || path === '') return { screen: 'landing' }
  if (path === '/add') return { screen: 'producer' }
  const town = path.match(/^\/towns\/([^/]+)$/)
  if (town) return { screen: 'town', slug: decodeURIComponent(town[1]) }
  return { screen: 'landing' } // unknown path: show the landing
}

// Current route from the live location.
export function currentRoute() {
  return parsePath(typeof window !== 'undefined' ? window.location.pathname : '/')
}

// Navigate to a path with history.pushState, then notify subscribers.
// Does nothing if the path is already current (avoids duplicate history entries).
export function navigate(path) {
  if (typeof window === 'undefined') return
  if (window.location.pathname !== path) {
    window.history.pushState({}, '', path)
  }
  notify()
}

// --- Subscription: let App re-render on navigate() and browser back/forward. ---

const listeners = new Set()

function notify() {
  const route = currentRoute()
  listeners.forEach((cb) => cb(route))
}

// Subscribe to route changes. Returns an unsubscribe function.
export function subscribe(cb) {
  listeners.add(cb)
  return () => listeners.delete(cb)
}

// Browser back/forward fires popstate — forward it to subscribers.
if (typeof window !== 'undefined') {
  window.addEventListener('popstate', notify)
}
