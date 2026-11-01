import { useState } from 'react'
import Icon from './Icon.jsx'
import { locations, photos } from '../data/events.js'
import { joinWaitlist } from '../api/discoveryApi.js'

const POPULAR_IDS = ['ashland-ma', 'framingham-ma', 'natick-ma', 'boston-ma', 'cambridge-ma', 'newton-ma']
const MAX_NEAREST_MILES = 100 // landing footer promise: "any town within 100 mi"

function miles([lat1, lng1], [lat2, lng2]) {
  const rad = (d) => (d * Math.PI) / 180
  const a = Math.sin(rad(lat2 - lat1) / 2) ** 2 +
    Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(rad(lng2 - lng1) / 2) ** 2
  return 3958.8 * 2 * Math.asin(Math.sqrt(a))
}

// SCREEN 1 — the landing (mockup v3 + coming-soon). One job: pick your town.
// States: resting (popular chips) → typing (suggestions) → uncovered (coming soon + email) → captured.
export default function Landing({ onChoose }) {
  const [query, setQuery] = useState('')
  const [locating, setLocating] = useState(false)
  const [locateError, setLocateError] = useState('')
  const [email, setEmail] = useState('')
  const [waitState, setWaitState] = useState('idle') // idle | sending | error | done
  const [waitTown, setWaitTown] = useState('')

  const q = query.trim().toLowerCase()
  const matches = q
    ? locations.filter((l) => l.label.toLowerCase().includes(q) || l.postal.startsWith(q))
    : []
  const uncovered = q.length >= 3 && matches.length === 0
  const popular = POPULAR_IDS.map((id) => locations.find((l) => l.id === id)).filter(Boolean)
  const townName = query.trim().replace(/\s+/g, ' ')

  const onType = (e) => {
    setQuery(e.target.value)
    setWaitState('idle')
    setEmail('')
  }

  const locate = () => {
    setLocateError('')
    if (!navigator.geolocation) {
      setLocateError('Location is not available in this browser. Search your town instead.')
      return
    }
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLocating(false)
        const here = [pos.coords.latitude, pos.coords.longitude]
        const nearest = locations
          .map((l) => ({ l, d: miles(here, l.center) }))
          .sort((a, b) => a.d - b.d)[0]
        if (nearest && nearest.d <= MAX_NEAREST_MILES) onChoose(nearest.l.id)
        else setLocateError('Arangly is not near you yet. Search your town to get a heads-up when it is.')
      },
      () => {
        setLocating(false)
        setLocateError('We could not get your location. Search your town instead.')
      },
      { timeout: 10000 },
    )
  }

  const submitWaitlist = async (e) => {
    e.preventDefault()
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) {
      setWaitState('error')
      return
    }
    setWaitState('sending')
    const ok = await joinWaitlist(townName, email.trim())
    if (ok) {
      setWaitTown(townName)
      setWaitState('done')
    } else {
      setWaitState('error')
    }
  }

  return (
    <main className="landing">
      <div className="landing-bg" style={{ backgroundImage: `url(${photos.concert})` }} />
      <div className="landing-scrim" />

      <header className="landing-top">
        <a className="logo on-dark" href="#" aria-label="Arangly home">
          <span className="logo-flower"><i /><i /><i /><i /></span>
          arangly
        </a>
        <span className="landing-tagline">for your town</span>
      </header>

      <section className="landing-hero">
        <p className="overline"><span>Any town, any size</span> · your town</p>
        <h1>What’s on in<br /><em>your town?</em></h1>
        <p className="landing-sub">
          Every other app sends you to the big city. Arang is for right where you live—markets,
          library talks, trail walks, live music.
        </p>

        <div className="town-picker">
          <button type="button" className="locate-btn" onClick={locate} disabled={locating}>
            <Icon name="locate" size={18} />
            {locating ? 'Finding you…' : 'Find events near me'}
          </button>
          {locateError && <p className="locate-error" role="alert">{locateError}</p>}

          <div className="or-divider">or search your town</div>

          <div className="searchwrap">
            <div className="town-search">
              <Icon name="search" size={20} />
              <input
                value={query}
                onChange={onType}
                placeholder="Type a town or ZIP…  (e.g. Ashland, 01721)"
                aria-label="Find your town"
              />
            </div>

            {matches.length > 0 && (
              <div className="suggest">
                {matches.slice(0, 5).map((loc) => (
                  <button key={loc.id} type="button" onClick={() => onChoose(loc.id)}>
                    <span className="pin"><Icon name="pin" size={16} /></span>
                    {loc.label}
                    <span className="zip">{loc.postal}</span>
                  </button>
                ))}
              </div>
            )}

            {uncovered && waitState !== 'done' && (
              <form className="soon" onSubmit={submitWaitlist}>
                <div className="soon-head">
                  <Icon name="spark" size={16} />
                  Coming soon to {townName}
                </div>
                <p>
                  We’re not live in {townName} yet—but we’re expanding. Leave your email and we’ll
                  tell you the moment it’s on.
                </p>
                <div className="soon-row">
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => { setEmail(e.target.value); if (waitState === 'error') setWaitState('idle') }}
                    placeholder="you@email.com"
                    aria-label="Your email"
                  />
                  <button type="submit" disabled={waitState === 'sending'}>
                    {waitState === 'sending' ? 'Saving…' : 'Notify me'}
                  </button>
                </div>
                {waitState === 'error' && (
                  <p className="soon-error" role="alert">
                    We could not save that. Check the email address and try again.
                  </p>
                )}
              </form>
            )}

            {uncovered && waitState === 'done' && (
              <div className="soon" role="status">
                <div className="soon-ok">
                  <Icon name="check" size={16} />
                  You’re on the list for {waitTown}.
                </div>
                <p>
                  We’ll email you when Arangly goes live near you. Want to help it happen sooner?
                  Tell a local organizer to add their event.
                </p>
              </div>
            )}
          </div>

          {!q && (
            <div className="popular">
              <p>Popular near you</p>
              <div className="chips">
                {popular.map((loc, i) => (
                  <button key={loc.id} type="button" className="chip" onClick={() => onChoose(loc.id)}>
                    {i === 0 && <Icon name="pin" size={14} />}
                    {loc.label.replace(/, MA$/, '')}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </section>

      <footer className="landing-foot">
        <span>Made for towns, by locals</span>
        <span>·</span>
        <span>any town within 100 mi</span>
      </footer>
    </main>
  )
}
