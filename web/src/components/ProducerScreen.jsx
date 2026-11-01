import { useRef, useState } from 'react'
import Icon from './Icon.jsx'

// PRODUCER screen (lives at /add, not linked from the consumer landing). Same Arang design
// language as the consumer SPA. Submits a typed event or a flyer image through the real producer
// pipeline (/api/producer/*), polls enrichment status, and shows the deciphered fields + review link.
export default function ProducerScreen() {
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState(null)   // { reviewPath, note }
  const [fields, setFields] = useState(null)    // { enriched, title, startsAt, location, notEvent }
  const [error, setError] = useState(null)
  const fileRef = useRef(null)

  async function pollStatus(eventId) {
    for (let i = 0; i < 20; i++) {
      await new Promise((r) => setTimeout(r, 700))
      try {
        const res = await fetch(`/api/producer/message/${eventId}/status`)
        const j = await res.json()
        if (j.ok && (j.enriched || j.notEvent)) { setFields(j); if (j.enriched) return }
        if (j.ok) setFields(j)
      } catch { /* keep polling */ }
    }
  }

  async function submitText() {
    if (!message.trim() || busy) return
    setBusy(true); setError(null); setResult(null); setFields(null)
    try {
      const res = await fetch('/api/producer/message', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message }),
      })
      const j = await res.json()
      if (!j.ok) { setError(j.error || 'Something went wrong.'); return }
      setResult({ reviewPath: j.reviewPath, note: j.reply })
      setMessage('')
      pollStatus(j.eventId)
    } catch { setError('Could not reach the server.') } finally { setBusy(false) }
  }

  async function submitImage(file) {
    if (!file || busy) return
    setBusy(true); setError(null); setResult(null); setFields(null)
    try {
      const fd = new FormData()
      fd.append('file', file)
      const res = await fetch('/api/producer/image', { method: 'POST', body: fd })
      const j = await res.json()
      if (!j.ok) { setError(j.error || 'Could not read the image.'); return }
      setResult({ reviewPath: j.reviewPath, note: j.message || 'Reading your flyer…' })
      pollStatus(j.eventId)
    } catch { setError('Could not reach the server.') } finally { setBusy(false) }
  }

  return (
    <main className="app producer-screen">
      <header className="topbar">
        <a className="logo" href="/" aria-label="Arang home">
          <span className="logo-flower"><i /><i /><i /><i /></span>
          arangly
        </a>
        <nav className="nav-links" aria-label="Main navigation">
          <a className="active" href="#add">Add an event</a>
        </nav>
        <div className="top-actions">
          <span className="producer-badge">Producer</span>
        </div>
      </header>

      <section className="explore solo producer-body" id="add">
        <div className="section-heading">
          <div>
            <p className="overline">For organizers · your town</p>
            <h2>Add your event to Arang.</h2>
          </div>
        </div>

        <div className="producer-grid">
          <div className="producer-card">
            <h3><Icon name="spark" size={18} /> Describe it in a sentence</h3>
            <p className="producer-hint">
              Tell us what’s happening — title, date, time, place. Arang reads it and fills in the details.
            </p>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="e.g. Garba/Dandiya Night, Fri Oct 30 8pm at Sri Lakshmi Temple, Ashland MA. $5, free under 9."
              rows={4}
            />
            <button className="producer-cta" type="button" onClick={submitText} disabled={busy || !message.trim()}>
              {busy ? 'Submitting…' : <>Add event <Icon name="arrow" size={17} /></>}
            </button>
          </div>

          <div className="producer-card">
            <h3><Icon name="pin" size={18} /> Or upload a flyer</h3>
            <p className="producer-hint">
              Got a poster or flyer image? Upload it and Arang will read the details off it.
            </p>
            <input ref={fileRef} type="file" accept="image/*" hidden
              onChange={(e) => submitImage(e.target.files?.[0])} />
            <button className="producer-upload" type="button" onClick={() => fileRef.current?.click()} disabled={busy}>
              <Icon name="calendar" size={17} /> Choose a flyer image
            </button>
          </div>
        </div>

        {error && <div className="producer-error">{error}</div>}

        {result && (
          <div className="producer-result">
            <div className="producer-result-head">
              <Icon name="check" size={18} />
              <strong>Got it — your event is in review.</strong>
            </div>
            {fields && fields.notEvent && (
              <p className="producer-muted">We couldn’t read this as an event yet. You can still edit it on the review page.</p>
            )}
            {fields && fields.enriched && (
              <ul className="producer-fields">
                {fields.title && <li><span>Title</span><strong>{fields.title}</strong></li>}
                {fields.startsAt && <li><span>Starts</span><strong>{fields.startsAt}</strong></li>}
                {fields.location && <li><span>Location</span><strong>{fields.location}</strong></li>}
              </ul>
            )}
            {!fields && <p className="producer-muted">Reading the details…</p>}
            <a className="producer-review" href={result.reviewPath} target="_blank" rel="noopener noreferrer">
              Open the review page <Icon name="arrow" size={16} />
            </a>
          </div>
        )}
      </section>
    </main>
  )
}
