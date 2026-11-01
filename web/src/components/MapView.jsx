import { useEffect, useMemo } from 'react'
import { MapContainer, TileLayer, Marker, useMap } from 'react-leaflet'
import L from 'leaflet'

// A teardrop pin matching the mockup, rendered as a Leaflet divIcon.
function pinIcon(event, active) {
  const index = event._index + 1
  return L.divIcon({
    className: '',
    html: `<div class="leaflet-pin ${active ? 'active' : ''}" style="--pin:${event.color}"><span>${index}</span></div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 15],
  })
}

// Fly the map to a new location when the chosen place changes, and pan to the active event.
function MapSync({ location, active }) {
  const map = useMap()
  useEffect(() => {
    map.setView(location.center, location.zoom, { animate: true })
  }, [location, map])
  useEffect(() => {
    if (active) map.panTo([active.lat, active.lng], { animate: true })
  }, [active, map])
  return null
}

export default function MapView({ location, events, activeId, onActivate, onSelect }) {
  const active = useMemo(() => events.find((e) => e.id === activeId), [events, activeId])

  return (
    <MapContainer className="leaflet-map" center={location.center} zoom={location.zoom} zoomControl={false} scrollWheelZoom>
      <TileLayer
        attribution='&copy; OpenStreetMap contributors'
        url="/tiles/{z}/{x}/{y}.png"
      />
      <MapSync location={location} active={active} />
      {events.map((event, index) => (
        <Marker
          key={event.id}
          position={[event.lat, event.lng]}
          icon={pinIcon({ ...event, _index: index }, activeId === event.id)}
          eventHandlers={{
            mouseover: () => onActivate(event.id),
            click: () => onSelect(event),
          }}
        />
      ))}
    </MapContainer>
  )
}
