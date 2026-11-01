import { useState } from 'react'
import Landing from './components/Landing.jsx'
import Discovery from './components/Discovery.jsx'
import ProducerScreen from './components/ProducerScreen.jsx'

// Routing (no router lib): the producer intake screen lives at /add (or #add) — same SPA, same
// design language as the consumer, but intentionally NOT linked from the consumer landing.
function isProducerRoute() {
  const p = window.location.pathname
  return p === '/add' || p.startsWith('/add/') || window.location.hash === '#add'
}

// Two-screen consumer flow:
//   Screen 1 (Landing)   — attractive, town-first; the one job is "choose your town".
//   Screen 2 (Discovery) — appears ONLY after a town is chosen: the list + OSM map.
export default function App() {
  const [locationId, setLocationId] = useState(null)

  if (isProducerRoute()) {
    return <ProducerScreen />
  }
  if (!locationId) {
    return <Landing onChoose={setLocationId} />
  }
  return <Discovery locationId={locationId} onChangeTown={() => setLocationId(null)} onPickTown={setLocationId} />
}
