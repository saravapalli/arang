import { useEffect, useMemo, useState, type CSSProperties, type ReactNode } from 'react'

type Category = 'All' | 'Music' | 'Food' | 'Family' | 'Arts' | 'Outdoors' | 'Wellness'

type EventItem = {
  id: number
  title: string
  category: Exclude<Category, 'All'>
  day: string
  date: string
  time: string
  place: string
  distance: string
  price: string
  x: number
  y: number
  color: string
  image: string
  featured?: boolean
}

const photos = {
  concert:
    'https://images.unsplash.com/photo-1777238096424-5f111ba9004c?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=1400',
  market:
    'https://images.unsplash.com/photo-1604200657090-ae45994b2451?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=1000',
  pottery:
    'https://images.unsplash.com/photo-1764507768733-667e135d8cb9?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=1000',
  forest:
    'https://images.unsplash.com/photo-1530563937443-1f02f662fa5c?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=1000',
  carousel:
    'https://images.unsplash.com/photo-1599143139935-65676c0ddf8f?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=1000',
}

const events: EventItem[] = [
  {
    id: 1,
    title: 'Blue Door Jazz Night',
    category: 'Music',
    day: 'SAT',
    date: '24',
    time: '8:00 PM',
    place: 'The Blue Door',
    distance: '0.8 mi',
    price: '$18',
    x: 67,
    y: 69,
    color: '#ff694a',
    image: photos.concert,
    featured: true,
  },
  {
    id: 2,
    title: 'Riverside Farmers Market',
    category: 'Food',
    day: 'SAT',
    date: '24',
    time: '9:00 AM',
    place: 'Lithia Way',
    distance: '0.3 mi',
    price: 'Free',
    x: 23,
    y: 31,
    color: '#43a875',
    image: photos.market,
  },
  {
    id: 3,
    title: 'Family Movie Under the Stars',
    category: 'Family',
    day: 'SAT',
    date: '24',
    time: '7:30 PM',
    place: 'Lithia Park',
    distance: '0.6 mi',
    price: 'Free',
    x: 31,
    y: 20,
    color: '#f2ac3c',
    image: photos.carousel,
  },
  {
    id: 4,
    title: 'Open-Air Art Fair',
    category: 'Arts',
    day: 'SUN',
    date: '25',
    time: '10:00 AM',
    place: 'Downtown Plaza',
    distance: '0.4 mi',
    price: 'Free',
    x: 74,
    y: 19,
    color: '#a86de0',
    image: photos.pottery,
  },
  {
    id: 5,
    title: 'Sunrise Trail Hike',
    category: 'Outdoors',
    day: 'SUN',
    date: '25',
    time: '6:00 AM',
    place: 'White Rabbit Trail',
    distance: '2.1 mi',
    price: 'Free',
    x: 13,
    y: 72,
    color: '#3189aa',
    image: photos.forest,
  },
  {
    id: 6,
    title: 'Slow Sunday Yoga',
    category: 'Wellness',
    day: 'SUN',
    date: '25',
    time: '8:30 AM',
    place: 'The Grove Studio',
    distance: '0.9 mi',
    price: '$12',
    x: 59,
    y: 34,
    color: '#496ee8',
    image: photos.forest,
  },
  {
    id: 7,
    title: 'Pottery Taster Class',
    category: 'Arts',
    day: 'SUN',
    date: '25',
    time: '2:00 PM',
    place: 'Gather Studio',
    distance: '0.7 mi',
    price: '$35',
    x: 47,
    y: 51,
    color: '#a86de0',
    image: photos.pottery,
  },
]

const categories: Category[] = ['All', 'Music', 'Food', 'Family', 'Arts', 'Outdoors', 'Wellness']

type IconName =
  | 'arrow'
  | 'calendar'
  | 'check'
  | 'clock'
  | 'close'
  | 'heart'
  | 'list'
  | 'map'
  | 'pin'
  | 'search'
  | 'sliders'
  | 'spark'
  | 'user'

function Icon({ name, size = 20 }: { name: IconName; size?: number }) {
  const paths: Record<IconName, ReactNode> = {
    arrow: <><path d="M5 12h14M14 7l5 5-5 5" /></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M16 3v4M8 3v4M3 10h18" /></>,
    check: <path d="m5 12 4 4L19 6" />,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3.5 2" /></>,
    close: <><path d="m6 6 12 12M18 6 6 18" /></>,
    heart: <path d="M20.8 5.9a5.2 5.2 0 0 0-7.4 0L12 7.3l-1.4-1.4a5.2 5.2 0 0 0-7.4 7.4L12 22l8.8-8.7a5.2 5.2 0 0 0 0-7.4Z" />,
    list: <><path d="M8 6h13M8 12h13M8 18h13" /><path d="M3 6h.01M3 12h.01M3 18h.01" /></>,
    map: <><path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3V6Z" /><path d="M9 3v15M15 6v15" /></>,
    pin: <><path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="2.5" /></>,
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
    sliders: <><path d="M4 7h16M4 17h16" /><circle cx="9" cy="7" r="2" fill="currentColor" /><circle cx="15" cy="17" r="2" fill="currentColor" /></>,
    spark: <><path d="m12 2 1.5 5.5L19 9l-5.5 1.5L12 16l-1.5-5.5L5 9l5.5-1.5L12 2Z" /><path d="m19 15 .7 2.3L22 18l-2.3.7L19 21l-.7-2.3L16 18l2.3-.7L19 15Z" /></>,
    user: <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></>,
  }

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name]}
    </svg>
  )
}

export default function App() {
  const [category, setCategory] = useState<Category>('All')
  const [activeId, setActiveId] = useState(1)
  const [saved, setSaved] = useState<number[]>([])
  const [selected, setSelected] = useState<EventItem | null>(null)
  const [view, setView] = useState<'map' | 'list'>('map')
  const [searchOpen, setSearchOpen] = useState(false)

  const filtered = useMemo(
    () => (category === 'All' ? events : events.filter((event) => event.category === category)),
    [category],
  )

  const activeEvent = events.find((event) => event.id === activeId) ?? events[0]

  useEffect(() => {
    if (!filtered.some((event) => event.id === activeId)) setActiveId(filtered[0]?.id ?? 1)
  }, [filtered, activeId])

  useEffect(() => {
    if (!selected) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setSelected(null)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [selected])

  const toggleSave = (id: number) => {
    setSaved((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id])
  }

  return (
    <main className="app">
      <header className="topbar">
        <a className="logo" href="#" aria-label="Arang home">
          <span className="logo-flower"><i /><i /><i /><i /></span>
          arang
        </a>
        <nav className="nav-links" aria-label="Main navigation">
          <a className="active" href="#discover">Discover</a>
          <a href="#weekend">This weekend</a>
          <a href="#collections">Collections</a>
        </nav>
        <div className="top-actions">
          <button className="location-button" type="button">
            <Icon name="pin" size={16} />
            Ashland, OR
          </button>
          <button className="circle-button" type="button" onClick={() => setSearchOpen(!searchOpen)} aria-label="Search">
            <Icon name="search" size={18} />
          </button>
          <button className="avatar" type="button" aria-label="Profile">
            <Icon name="user" size={17} />
          </button>
        </div>
      </header>

      {searchOpen && (
        <div className="search-panel">
          <Icon name="search" size={19} />
          <input autoFocus aria-label="Search events" placeholder="Try “live music” or “Sunday morning”" />
          <kbd>ESC</kbd>
        </div>
      )}

      <section className="hero" id="discover">
        <div className="hero-copy">
          <p className="overline"><span>Live local</span> · May 24–25</p>
          <h1>Make a weekend<br />of <em>right here.</em></h1>
          <p className="hero-subtitle">A handpicked guide to what’s happening in and around Ashland—good food, live music, and reasons to wander.</p>
          <div className="hero-actions">
            <a className="primary-cta" href="#explore">
              Explore the map <Icon name="arrow" size={18} />
            </a>
            <div className="people">
              <div className="faces">
                <span>JL</span><span>MK</span><span>AR</span>
              </div>
              <p><strong>1,200+</strong><br />locals exploring</p>
            </div>
          </div>
        </div>

        <article className="hero-feature" onClick={() => setSelected(events[0])}>
          <img src={photos.concert} alt="People enjoying an outdoor concert in a park" />
          <div className="hero-image-shade" />
          <div className="feature-top">
            <span className="live-badge"><i /> Editor’s pick</span>
            <button
              type="button"
              className={`glass-save ${saved.includes(1) ? 'saved' : ''}`}
              onClick={(event) => { event.stopPropagation(); toggleSave(1) }}
              aria-label="Save Blue Door Jazz Night"
            >
              <Icon name="heart" size={18} />
            </button>
          </div>
          <div className="feature-content">
            <span className="feature-category">Live music · Saturday</span>
            <h2>Blue Door<br />Jazz Night</h2>
            <div className="feature-meta">
              <span><Icon name="clock" size={15} /> 8:00 PM</span>
              <span><Icon name="pin" size={15} /> Railroad District</span>
            </div>
          </div>
          <button className="feature-arrow" type="button" aria-label="View event">
            <Icon name="arrow" size={20} />
          </button>
        </article>

        <aside className="weekend-card" id="weekend">
          <div className="weekend-heading">
            <div>
              <p>This weekend</p>
              <strong>May 24–25</strong>
            </div>
            <Icon name="calendar" size={21} />
          </div>
          <div className="day-row">
            <div className="day-date"><span>SAT</span><strong>24</strong></div>
            <div><strong>16 things to do</strong><span>Sunny · 72°</span></div>
            <button type="button" aria-label="See Saturday events"><Icon name="arrow" size={16} /></button>
          </div>
          <div className="day-row">
            <div className="day-date coral"><span>SUN</span><strong>25</strong></div>
            <div><strong>12 things to do</strong><span>Clear · 75°</span></div>
            <button type="button" aria-label="See Sunday events"><Icon name="arrow" size={16} /></button>
          </div>
          <div className="weekend-note">
            <Icon name="spark" size={18} />
            <p><strong>Good to know</strong> Most downtown spots are within a 15-minute walk.</p>
          </div>
        </aside>
      </section>

      <section className="explore" id="explore">
        <div className="section-heading">
          <div>
            <p className="overline">Explore nearby</p>
            <h2>Find your next favorite thing.</h2>
          </div>
          <div className="view-toggle" aria-label="Change view">
            <button className={view === 'map' ? 'active' : ''} type="button" onClick={() => setView('map')}>
              <Icon name="map" size={16} /> Map
            </button>
            <button className={view === 'list' ? 'active' : ''} type="button" onClick={() => setView('list')}>
              <Icon name="list" size={16} /> List
            </button>
          </div>
        </div>

        <div className="category-bar">
          <div className="category-scroll">
            {categories.map((item) => (
              <button
                key={item}
                type="button"
                className={category === item ? 'active' : ''}
                onClick={() => setCategory(item)}
              >
                {item}
                {item === 'All' && <span>{events.length}</span>}
              </button>
            ))}
          </div>
          <button className="filter-button" type="button">
            <Icon name="sliders" size={16} /> Filters <span>2</span>
          </button>
        </div>

        <div className={`discovery-shell ${view === 'list' ? 'list-view' : ''}`}>
          <div className="event-rail">
            <div className="rail-head">
              <p><strong>{filtered.length} places</strong> around Ashland</p>
              <button type="button">Recommended <span>⌄</span></button>
            </div>
            <div className="cards">
              {filtered.map((event) => (
                <article
                  key={event.id}
                  className={`event-card ${activeId === event.id ? 'active' : ''}`}
                  onMouseEnter={() => setActiveId(event.id)}
                  onClick={() => setSelected(event)}
                >
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
                    <button
                      className={`card-save ${saved.includes(event.id) ? 'saved' : ''}`}
                      type="button"
                      onClick={(click) => { click.stopPropagation(); toggleSave(event.id) }}
                      aria-label={`Save ${event.title}`}
                    >
                      <Icon name="heart" size={16} />
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </div>

          <div className="map-wrap">
            <div className="map-canvas">
              <div className="map-tint" />
              <div className="park-shape park-a">Lithia Park</div>
              <div className="park-shape park-b" />
              <div className="water-line" />
              <div className="map-road road-one" /><div className="map-road road-two" />
              <div className="map-road road-three" /><div className="map-road road-four" />
              <span className="road-label label-one">Siskiyou Blvd</span>
              <span className="road-label label-two">E Main Street</span>
              <span className="map-place place-one">Downtown</span>
              <span className="map-place place-two">Railroad District</span>

              {filtered.map((event, index) => (
                <button
                  key={event.id}
                  type="button"
                  className={`rich-pin ${activeId === event.id ? 'active' : ''}`}
                  style={{ left: `${event.x}%`, top: `${event.y}%`, '--pin': event.color } as CSSProperties}
                  onMouseEnter={() => setActiveId(event.id)}
                  onClick={() => setSelected(event)}
                  aria-label={event.title}
                >
                  <span>{index + 1}</span>
                </button>
              ))}

              <div className="active-map-card" style={{ left: `${activeEvent.x}%`, top: `${activeEvent.y}%` }}>
                <img src={activeEvent.image} alt="" />
                <div>
                  <span>{activeEvent.category}</span>
                  <strong>{activeEvent.title}</strong>
                  <p>{activeEvent.time} · {activeEvent.price}</p>
                </div>
              </div>

              <div className="map-controls">
                <button type="button" aria-label="Zoom in">+</button>
                <button type="button" aria-label="Zoom out">−</button>
              </div>
              <div className="map-caption"><i /> Approximate event areas</div>
            </div>
          </div>
        </div>
      </section>

      <section className="local-strip" id="collections">
        <div>
          <Icon name="spark" size={24} />
          <p><span>Made for locals, by locals</span><strong>Small-town energy. Very good plans.</strong></p>
        </div>
        <button type="button">Get the weekly edit <Icon name="arrow" size={17} /></button>
      </section>

      {selected && (
        <div className="modal-overlay" onMouseDown={() => setSelected(null)}>
          <article className="detail-modal" role="dialog" aria-modal="true" onMouseDown={(e) => e.stopPropagation()}>
            <div className="modal-image">
              <img src={selected.image} alt="" />
              <button type="button" onClick={() => setSelected(null)} aria-label="Close">
                <Icon name="close" size={19} />
              </button>
              <span>{selected.category}</span>
            </div>
            <div className="modal-content">
              <div className="modal-date"><span>{selected.day}</span><strong>{selected.date}</strong></div>
              <div>
                <h2>{selected.title}</h2>
                <p><Icon name="clock" size={16} /> {selected.time}</p>
                <p><Icon name="pin" size={16} /> {selected.place} · {selected.distance} away</p>
              </div>
              <div className="modal-note">
                <Icon name="check" size={17} />
                <p><strong>A local favorite</strong><span>Details are verified, but map locations are approximate.</span></p>
              </div>
              <div className="modal-actions">
                <button className="book-button" type="button">Get details · {selected.price}<Icon name="arrow" size={18} /></button>
                <button
                  className={`modal-save ${saved.includes(selected.id) ? 'saved' : ''}`}
                  type="button"
                  onClick={() => toggleSave(selected.id)}
                >
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
