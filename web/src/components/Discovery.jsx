import { useEffect, useMemo, useState } from 'react'
import Icon from './Icon.jsx'
import MapView from './MapView.jsx'
import { categories } from '../data/events.js'
import {
  fetchRealEvents, fetchNearby, saveInterests, getInterests, toVocab,
  googleMapsUrl, WINDOW_PRESETS, windowFor,
} from '../api/discoveryApi.js'
import { navigate } from '../router.js'

// Adapt a site-config town {slug,name,state,postal,lat,lng,zoom} to the shape MapView wants
// (it reads location.center + location.zoom + location.label).
function toMapLocation(town) {
  return {
    center: [town.lat, town.lng],
    zoom: town.zoom || 13,
    label: `${town.name}, ${town.state}`,
  }
}

// SCREEN 2 — discovery. Rendered for a LIVE town (P2.1 T17). The town comes from site-config.
export default function Discovery({ town, onChangeTown }) {
  const [category, setCategory] = useState('All')
  const [activeId, setActiveId] = useState(null)
  const [saved, setSaved] = useState([])
  const [selected, setSelected] = useState(null)
  const [view, setView] = useState('map')
  const [realEvents, setRealEvents] = useState([])
  const [savedCategories, setSavedCategories] = useState([])
  const [windowPreset, setWindowPreset] = useState('this-week')
  const [nearby, setNearby] = useState([]) // nearby towns for the empty state

  const location = useMemo(() => toMapLocation(town), [town])
  const townLabel = location.label

  // Load any previously saved interests (cookie identity) once.
  useEffect(() => {
    getInterests().then(setSavedCategories)
  }, [])

  // Pull REAL database events for the chosen town + selected time window (UF-P2-1).
  useEffect(() => {
    fetchRealEvents(town.postal, windowFor(windowPreset)).then(setRealEvents)
  }, [town.postal, windowPreset])

  // Real DB events already come filtered to the chosen town by /api/discovery.
  const events = realEvents
  const filtered = useMemo(
    () => (category === 'All' ? events : events.filter((e) => e.category === category)),
    [category, events],
  )
  const activeEvent = events.find((e) => e.id === activeId) ?? events[0]

  // Reset category + active event when the town changes.
  useEffect(() => {
    setCategory('All')
    setActiveId(events[0]?.id ?? null)
  }, [town.slug]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!filtered.some((e) => e.id === activeId)) setActiveId(filtered[0]?.id ?? null)
  }, [filtered, activeId])

  // Empty window: fetch up to 3 nearby towns to offer instead (AC "Nothing on this week").
  const windowEmpty = events.length === 0
  useEffect(() => {
    if (!windowEmpty) { setNearby([]); return }
    let live = true
    fetchNearby(town.slug).then((rows) => { if (live) setNearby(rows) })
    return () => { live = false }
  }, [windowEmpty, town.slug])

  useEffect(() => {
    if (!selected) return
    const onKey = (e) => { if (e.key === 'Escape') setSelected(null) }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [selected])

  const toggleSave = (id) => {
    setSaved((cur) => (cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id]))
    // Persist the event's category as a real interest (REQ-10). Union of current + this.
    const ev = events.find((e) => e.id === id)
    if (!ev) return
    const vocab = toVocab(ev.category)
    const next = savedCategories.includes(vocab)
      ? savedCategories.filter((c) => c !== vocab)
      : [...savedCategories, vocab]
    saveInterests(next).then((result) => {
      if (result) setSavedCategories(result)
    })
  }

  return (
    <main className="app discovery-screen">
      <header className="topbar">
        <a className="logo" href="/" aria-label="Arangly home" onClick={(e) => { e.preventDefault(); onChangeTown() }}>
          <span className="logo-flower"><i /><i /><i /><i /></span>
          arangly
        </a>
        <nav className="nav-links" aria-label="Main navigation">
          <a className="active" href="#discover">Discover</a>
          <a href="#weekend">This weekend</a>
          <a href="#collections">Collections</a>
        </nav>
        <div className="top-actions">
          <div className="location-picker">
            <button className="location-button" type="button" onClick={onChangeTown} aria-label="Change town">
              <Icon name="pin" size={16} /> {townLabel}
              <span className="loc-caret">⌄</span>
            </button>
          </div>
          <button className="avatar" type="button" aria-label="Profile"><Icon name="user" size={17} /></button>
        </div>
      </header>

      <section className="explore solo" id="explore">
        <div className="section-heading">
          <div>
            <p className="overline">Your town · {WINDOW_PRESETS.find((w) => w.id === windowPreset)?.label}</p>
            <h2>What’s on in {townLabel}.</h2>
          </div>
          <div className="view-toggle" aria-label="Change view">
            <button className={view === 'map' ? 'active' : ''} type="button" onClick={() => setView('map')}><Icon name="map" size={16} /> Map</button>
            <button className={view === 'list' ? 'active' : ''} type="button" onClick={() => setView('list')}><Icon name="list" size={16} /> List</button>
          </div>
        </div>

        <div className="window-bar" aria-label="Choose a time window">
          <Icon name="calendar" size={16} />
          {WINDOW_PRESETS.map((w) => (
            <button key={w.id} type="button"
              className={windowPreset === w.id ? 'active' : ''}
              onClick={() => setWindowPreset(w.id)}>
              {w.label}
            </button>
          ))}
        </div>

        <div className="category-bar">
          <div className="category-scroll">
            {categories.map((item) => (
              <button key={item} type="button" className={category === item ? 'active' : ''} onClick={() => setCategory(item)}>
                {item}{item === 'All' && <span>{events.length}</span>}
              </button>
            ))}
          </div>
          <button className="filter-button" type="button"><Icon name="sliders" size={16} /> Filters <span>2</span></button>
        </div>

        <div className={`discovery-shell view-${view} ${view === 'list' ? 'list-view' : ''}`}>
          <div className="event-rail">
            <div className="rail-head">
              <p><strong>{filtered.length} places</strong> around {townLabel}</p>
              <button type="button">Recommended <span>⌄</span></button>
            </div>
            <div className="cards">
              {/* Empty window: "Nothing on this week" + up to 3 nearby towns + add-event link. */}
              {events.length === 0 && (
                <div className="cards-empty nothing-on">
                  <Icon name="calendar" size={20} />
                  <p><strong>Nothing on this week</strong></p>
                  {nearby.length > 0 && (
                    <>
                      <p className="nearby-head">Nearby towns</p>
                      <div className="nearby-towns">
                        {nearby.map((n) => (
                          <button key={n.slug} type="button" className="chip"
                            onClick={() => navigate(`/towns/${n.slug}`)}>
                            <Icon name="pin" size={14} /> {n.name}, {n.state}
                            {typeof n.eventCount === 'number' && <span className="zip">{n.eventCount}</span>}
                          </button>
                        ))}
                      </div>
                    </>
                  )}
                  <a className="add-event-link" href="/submit">Have an event? Add it</a>
                </div>
              )}
              {/* Non-empty window but the chosen category has no matches. */}
              {events.length > 0 && filtered.length === 0 && (
                <div className="cards-empty">
                  <Icon name="calendar" size={20} />
                  <p><strong>Nothing in this category</strong>
                  Try another category or time window.</p>
                </div>
              )}
              {filtered.map((event) => (
                <article key={event.id} className={`event-card ${activeId === event.id ? 'active' : ''}`}
                  onMouseEnter={() => setActiveId(event.id)} onClick={() => setSelected(event)}>
                  <div className="card-image">
                    <img src={event.image} alt="" />
                    <span className="price-tag">{event.price}</span>
                  </div>
                  <div className="card-body">
                    <div className="mini-date"><span>{event.day}</span><strong>{event.date}</strong></div>
                    <div className="card-copy">
                      <span className="category-label" style={{ color: event.color }}>{event.category}</span>
                      <h3>{event.title}</h3>
                      <p>{event.time} · {event.place}</p>
                      <span className="distance"><Icon name="pin" size={12} /> {event.distance} away</span>
                    </div>
                    <button className={`card-save ${saved.includes(event.id) ? 'saved' : ''}`} type="button"
                      onClick={(c) => { c.stopPropagation(); toggleSave(event.id) }} aria-label={`Save ${event.title}`}>
                      <Icon name="heart" size={16} />
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </div>

          <div className="map-wrap">
            <MapView location={location} events={filtered} activeId={activeId} onActivate={setActiveId} onSelect={setSelected} />
            {activeEvent && (
              <div className="floating-active-card">
                <img src={activeEvent.image} alt="" />
                <div>
                  <span>{activeEvent.category}</span>
                  <strong>{activeEvent.title}</strong>
                  <p>{activeEvent.time} · {activeEvent.price}</p>
                </div>
              </div>
            )}
            <div className="map-caption"><i /> Approximate event areas</div>
          </div>
        </div>
      </section>

      {selected && (
        <div className="modal-overlay" onMouseDown={() => setSelected(null)}>
          <article className="detail-modal" role="dialog" aria-modal="true" onMouseDown={(e) => e.stopPropagation()}>
            <div className="modal-image">
              <img src={selected.image} alt="" />
              <button type="button" onClick={() => setSelected(null)} aria-label="Close"><Icon name="close" size={19} /></button>
              <span>{selected.category}</span>
            </div>
            <div className="modal-content">
              <div className="modal-date"><span>{selected.day}</span><strong>{selected.date}</strong></div>
              <div>
                <h2>{selected.title}</h2>
                <p><Icon name="clock" size={16} /> {selected.time}</p>
                <p><Icon name="pin" size={16} /> {selected.place} · {selected.distance} away</p>
                <a className="maps-link" href={googleMapsUrl(selected)} target="_blank" rel="noopener noreferrer">
                  <Icon name="map" size={15} /> Open in Google Maps
                </a>
              </div>
              <div className="modal-note">
                <Icon name="check" size={17} />
                <p><strong>A local favorite</strong><span>Details are verified, but map locations are approximate.</span></p>
              </div>
              <div className="modal-actions">
                <button className="book-button" type="button">Get details · {selected.price}<Icon name="arrow" size={18} /></button>
                <button className={`modal-save ${saved.includes(selected.id) ? 'saved' : ''}`} type="button" onClick={() => toggleSave(selected.id)}>
                  <Icon name="heart" size={18} /> {saved.includes(selected.id) ? 'Saved' : 'Save'}
                </button>
              </div>
            </div>
          </article>
        </div>
      )}
    </main>
  )
}
