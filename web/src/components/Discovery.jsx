import { useEffect, useMemo, useState } from 'react'
import Icon from './Icon.jsx'
import MapView from './MapView.jsx'
import { locations, categories } from '../data/events.js'
import { fetchRealEvents, saveInterests, getInterests, toVocab, googleMapsUrl, WINDOW_PRESETS, windowFor } from '../api/discoveryApi.js'

// SCREEN 2 — discovery. Only rendered once a town is chosen on the landing.
export default function Discovery({ locationId, onChangeTown, onPickTown }) {
  const [category, setCategory] = useState('All')
  const [activeId, setActiveId] = useState(null)
  const [saved, setSaved] = useState([])
  const [selected, setSelected] = useState(null)
  const [view, setView] = useState('map')
  const [townOpen, setTownOpen] = useState(false)
  const [realEvents, setRealEvents] = useState([])
  const [savedCategories, setSavedCategories] = useState([])
  const [windowPreset, setWindowPreset] = useState('this-week')

  // Load any previously saved interests (cookie identity) once.
  useEffect(() => {
    getInterests().then(setSavedCategories)
  }, [])

  // Pull REAL database events for the chosen town + selected time window (UF-P2-1).
  useEffect(() => {
    const town = locations.find((l) => l.id === locationId)
    fetchRealEvents(town?.postal, windowFor(windowPreset)).then(setRealEvents)
  }, [locationId, windowPreset])

  const location = useMemo(
    () => locations.find((l) => l.id === locationId) ?? locations[0],
    [locationId],
  )

  // Real DB events already come filtered to the chosen town's postal by /api/discovery.
  // Merge them real-first so they show at the top of the list.
  const events = useMemo(() => {
    const base = location.events
    if (realEvents.length) {
      const extraIds = new Set(realEvents.map((e) => e.id))
      return [...realEvents, ...base.filter((e) => !extraIds.has(e.id))]
    }
    return base
  }, [location, realEvents])

  const filtered = useMemo(
    () => (category === 'All' ? events : events.filter((e) => e.category === category)),
    [category, events],
  )
  const activeEvent = events.find((e) => e.id === activeId) ?? events[0]

  useEffect(() => {
    setCategory('All')
    setActiveId(events[0]?.id ?? null)
  }, [locationId]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!filtered.some((e) => e.id === activeId)) setActiveId(filtered[0]?.id ?? null)
  }, [filtered, activeId])

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
        <a className="logo" href="#" aria-label="Arang home" onClick={(e) => { e.preventDefault(); onChangeTown() }}>
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
            <button className="location-button" type="button" onClick={() => setTownOpen((o) => !o)} aria-haspopup="listbox" aria-expanded={townOpen}>
              <Icon name="pin" size={16} /> {location.label}
              <span className="loc-caret">⌄</span>
            </button>
            {townOpen && (
              <div className="location-menu" role="listbox" aria-label="Change your town">
                <p className="location-menu-head">Change your town</p>
                {locations.map((loc) => (
                  <button key={loc.id} type="button" role="option" aria-selected={loc.id === locationId}
                    className={loc.id === locationId ? 'active' : ''}
                    onClick={() => { onPickTown(loc.id); setTownOpen(false) }}>
                    <Icon name="pin" size={14} /><span>{loc.label}</span>
                    {loc.id === locationId && <Icon name="check" size={15} />}
                  </button>
                ))}
                <button type="button" className="town-reset" onClick={() => { setTownOpen(false); onChangeTown() }}>
                  ← Back to all towns
                </button>
              </div>
            )}
          </div>
          <button className="avatar" type="button" aria-label="Profile"><Icon name="user" size={17} /></button>
        </div>
      </header>

      <section className="explore solo" id="explore">
        <div className="section-heading">
          <div>
            <p className="overline">Your town · {WINDOW_PRESETS.find((w) => w.id === windowPreset)?.label}</p>
            <h2>What’s on in {location.label}.</h2>
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
              <p><strong>{filtered.length} places</strong> around {location.label}</p>
              <button type="button">Recommended <span>⌄</span></button>
            </div>
            <div className="cards">
              {filtered.length === 0 && (
                <div className="cards-empty">
                  <Icon name="calendar" size={20} />
                  <p><strong>Nothing listed here yet</strong>
                  Be the first — add an event for {location.label}, or try another time window.</p>
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
