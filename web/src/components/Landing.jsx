import { useEffect, useRef, useState } from 'react'
import Icon from './Icon.jsx'
import { photos } from '../data/events.js'
import { joinWaitlist, searchPlaces } from '../api/discoveryApi.js'
import { townsOf, nearMeMilesOf, consentOf, creditsOf } from '../siteConfig.js'

// Great-circle distance in miles between two [lat,lng] points.
function miles([lat1, lng1], [lat2, lng2]) {
  const rad = (d) => (d * Math.PI) / 180
  const a = Math.sin(rad(lat2 - lat1) / 2) ** 2 +
    Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(rad(lng2 - lng1) / 2) ** 2
  return 3958.8 * 2 * Math.asin(Math.sqrt(a))
}

// Fill the consent template: replace "<town>" with the place label.
function consentText(consent, place) {
  return (consent.text || '').replace(/<town>/g, place || 'your town')
}

// SCREEN 1 — the landing. One job: pick your town. The town SET comes from site-config;
// search + waitlist go to the backend (P2.1 T16).
// States: resting (popular chips) → typing (suggestions) → uncovered (coming soon + email) → captured.
export default function Landing({ config, onChoose }) {
  const [query, setQuery] = useState('')
  const [locating, setLocating] = useState(false)
  const [locateError, setLocateError] = useState('')
  const [email, setEmail] = useState('')
  const [waitState, setWaitState] = useState('idle') // idle | sending | error | done
  const [waitError, setWaitError] = useState('')
  const [waitPlaceLabel, setWaitPlaceLabel] = useState('')

  // Search results tracked with a request id so stale responses are ignored.
  const [results, setResults] = useState([]) // [{slug,name,state,live,exact}]
  const [hint, setHint] = useState('')        // 'type_town_name' when digits were typed
  const [searchDown, setSearchDown] = useState(false) // /api/places returned 503
  // The waitlist "place" value sent to the server: a slug for a known place, or free text.
  const [waitTarget, setWaitTarget] = useState('')
  const reqSeq = useRef(0)

  const towns = townsOf(config)
  const nearMeMiles = nearMeMilesOf(config)
  const consent = consentOf(config)
  const credits = creditsOf(config)

  const typed = query.trim()
  // Coming-soon (free text) applies once 3+ non-digit chars were typed and nothing matched.
  const hasDigitsOnly = typed.length > 0 && /^\d+$/.test(typed)
  const uncoveredFreeText =
    typed.length >= 3 && !hasDigitsOnly && !searchDown && hint === '' && results.length === 0

  // Debounced place search (~300ms). Digits-only and empty inputs are handled by the backend.
  useEffect(() => {
    if (typed.length === 0) { setResults([]); setHint(''); setSearchDown(false); return }
    const myId = ++reqSeq.current
    const t = setTimeout(async () => {
      const body = await searchPlaces(typed)
      if (myId !== reqSeq.current) return // a newer keystroke superseded this one
      if (body.error === 'geo_unavailable') {
        setSearchDown(true); setResults([]); setHint('')
        return
      }
      setSearchDown(false)
      if (body.error === 'q_invalid') { setResults([]); setHint(''); return }
      setResults(Array.isArray(body.results) ? body.results : [])
      setHint(body.hint || '')
    }, 300)
    return () => clearTimeout(t)
  }, [typed])

  const onType = (e) => {
    setQuery(e.target.value)
    // Reset any coming-soon form that was showing for a previous query.
    setWaitState('idle')
    setWaitError('')
    setEmail('')
    setWaitTarget('')
    setWaitPlaceLabel('')
  }

  // Open a place suggestion: live -> go to its town; not-live -> show coming-soon form.
  const openSuggestion = (place) => {
    if (place.live) {
      onChoose(place.slug)
    } else {
      // Coming-soon for a known-but-not-live town: use its slug as the waitlist place.
      setWaitPlaceLabel(`${place.name}, ${place.state}`)
      setWaitTarget(place.slug)
      setWaitState('idle')
    }
  }

  // The waitlist "place" value sent to the server: a slug for a known place, or free text.
  // (declared above alongside the other search state)

  // Enter with exactly one suggestion opens it.
  const onKeyDown = (e) => {
    if (e.key === 'Enter' && results.length === 1) {
      e.preventDefault()
      openSuggestion(results[0])
    }
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
        // Coordinates are used only locally to pick the nearest live town. Never sent anywhere.
        const here = [pos.coords.latitude, pos.coords.longitude]
        const nearest = towns
          .map((t) => ({ t, d: miles(here, [t.lat, t.lng]) }))
          .sort((a, b) => a.d - b.d)[0]
        if (nearest && nearest.d <= nearMeMiles) onChoose(nearest.t.slug)
        else setLocateError("Arangly isn't near you yet. Search your town to get a heads-up when it is.")
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
    setWaitState('sending')
    setWaitError('')
    const { ok, status } = await joinWaitlist(waitlistPlace, email.trim())
    if (ok) {
      setWaitState('done')
      return
    }
    // Keep the entered email on any failure; show a message matched to the outcome.
    setWaitState('error')
    if (status && status.errors) setWaitError('Check the email address.')
    else if (status && typeof status.retryAfterSeconds === 'number') setWaitError('Please try again shortly.')
    else setWaitError("We couldn't save that right now. Please try again.")
  }

  // Popular towns: those with a popularRank, sorted by rank, labelled without ", MA".
  const popular = towns
    .filter((t) => t.popularRank != null)
    .sort((a, b) => a.popularRank - b.popularRank)

  // Whether to show the coming-soon email form: either a selected not-live place or free text.
  const showComingSoon = (waitTarget !== '' || uncoveredFreeText)
  // Label shown in the form. A selected not-live place uses its "Name, ST"; free text uses itself.
  const comingSoonLabel = waitPlaceLabel || typed
  // The exact "place" value POSTed: the selected slug, else the typed free text.
  const waitlistPlace = waitTarget || typed

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
          Every other app sends you to the big city. Arangly is for right where you live—markets,
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
                onKeyDown={onKeyDown}
                placeholder="Type a town name…  (e.g. Ashland)"
                aria-label="Find your town"
              />
            </div>

            {/* Digits typed: a gentle hint, NOT a not-found message. */}
            {hint === 'type_town_name' && (
              <p className="search-hint" role="status">Type a town name</p>
            )}

            {/* Live suggestions. Each is "Name, ST". */}
            {!searchDown && results.length > 0 && (
              <div className="suggest">
                {results.slice(0, 5).map((place) => (
                  <button key={place.slug} type="button" onClick={() => openSuggestion(place)}>
                    <span className="pin"><Icon name="pin" size={16} /></span>
                    {place.name}, {place.state}
                    {!place.live && <span className="zip">coming soon</span>}
                  </button>
                ))}
              </div>
            )}

            {/* Place search is down (503): offer the embedded live towns as buttons. */}
            {searchDown && (
              <div className="suggest" role="alert">
                <p className="search-hint">Search is unavailable right now. Pick a town:</p>
                {towns.slice(0, 12).map((t) => (
                  <button key={t.slug} type="button" onClick={() => onChoose(t.slug)}>
                    <span className="pin"><Icon name="pin" size={16} /></span>
                    {t.name}, {t.state}
                  </button>
                ))}
              </div>
            )}

            {/* Free-text uncovered town: show the message AND the email form together. */}
            {uncoveredFreeText && (
              <p className="search-hint" role="status">
                We’re not live in {typed} — Arangly covers Massachusetts for now.
              </p>
            )}

            {showComingSoon && waitState !== 'done' && (
              <form className="soon" onSubmit={submitWaitlist}>
                <div className="soon-head">
                  <Icon name="spark" size={16} />
                  Coming soon to {comingSoonLabel}
                </div>
                <p>{consentText(consent, comingSoonLabel)}</p>
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
                  <p className="soon-error" role="alert">{waitError}</p>
                )}
              </form>
            )}

            {showComingSoon && waitState === 'done' && (
              <div className="soon" role="status">
                <div className="soon-ok">
                  <Icon name="check" size={16} />
                  You’re on the list for {comingSoonLabel}.
                </div>
                <p>
                  We’ll email you when Arangly goes live near you. Want to help it happen sooner?
                  Tell a local organizer to add their event.
                </p>
              </div>
            )}
          </div>

          {typed.length === 0 && popular.length > 0 && (
            <div className="popular">
              <p>Popular near you</p>
              <div className="chips">
                {popular.map((t, i) => (
                  <button key={t.slug} type="button" className="chip" onClick={() => onChoose(t.slug)}>
                    {i === 0 && <Icon name="pin" size={14} />}
                    {t.name}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </section>

      <footer className="landing-foot">
        {credits.length > 0
          ? <span>{credits.join(' · ')}</span>
          : <span>Made for towns, by locals</span>}
      </footer>
    </main>
  )
}
