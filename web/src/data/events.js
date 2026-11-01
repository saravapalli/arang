// Mock discovery data for the SPA preview.
// Location is a CHOICE, not a fixed place — Ashland, MA is just one example.
// The user picks which location to explore; the map re-centers and events swap.
// S-B replaces this with GET /api/discovery?location=<postal> over live data.

export const photos = {
  concert: 'https://images.unsplash.com/photo-1777238096424-5f111ba9004c?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=1400',
  market: 'https://images.unsplash.com/photo-1604200657090-ae45994b2451?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=1000',
  pottery: 'https://images.unsplash.com/photo-1764507768733-667e135d8cb9?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=1000',
  forest: 'https://images.unsplash.com/photo-1530563937443-1f02f662fa5c?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=1000',
  carousel: 'https://images.unsplash.com/photo-1599143139935-65676c0ddf8f?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=85&w=1000',
}

export const categories = ['All', 'Music', 'Food', 'Family', 'Arts', 'Outdoors', 'Wellness']

// First-implementation coverage: MetroWest + Greater-Boston MA towns. Events come ONLY from
// GET /api/discovery (merged by postal) — no placeholder/dummy events. Centroids are real (GeoNames).
export const locations = [
  { id: 'ashland-ma', label: 'Ashland, MA', postal: '01721', center: [42.2539, -71.4583], zoom: 14, events: [] },
  { id: 'framingham-ma',   label: 'Framingham, MA',   postal: '01701', center: [42.3007, -71.4255], zoom: 13, events: [] },
  { id: 'holliston-ma',    label: 'Holliston, MA',    postal: '01746', center: [42.2026, -71.4361], zoom: 14, events: [] },
  { id: 'hopkinton-ma',    label: 'Hopkinton, MA',    postal: '01748', center: [42.2190, -71.5302], zoom: 14, events: [] },
  { id: 'natick-ma',       label: 'Natick, MA',       postal: '01760', center: [42.2875, -71.3574], zoom: 14, events: [] },
  { id: 'sherborn-ma',     label: 'Sherborn, MA',     postal: '01770', center: [42.2331, -71.3787], zoom: 14, events: [] },
  { id: 'wellesley-ma',    label: 'Wellesley, MA',    postal: '02481', center: [42.3106, -71.2747], zoom: 14, events: [] },
  { id: 'marlborough-ma',  label: 'Marlborough, MA',  postal: '01752', center: [42.3509, -71.5434], zoom: 13, events: [] },
  { id: 'sudbury-ma',      label: 'Sudbury, MA',      postal: '01776', center: [42.3837, -71.4282], zoom: 14, events: [] },
  // Boston + towns inside the I-95 / Route 128 belt.
  { id: 'boston-ma',       label: 'Boston, MA',       postal: '02108', center: [42.3576, -71.0684], zoom: 13, events: [] },
  { id: 'cambridge-ma',    label: 'Cambridge, MA',    postal: '02139', center: [42.3647, -71.1042], zoom: 14, events: [] },
  { id: 'somerville-ma',   label: 'Somerville, MA',   postal: '02143', center: [42.3829, -71.1028], zoom: 14, events: [] },
  { id: 'brookline-ma',    label: 'Brookline, MA',    postal: '02445', center: [42.3259, -71.1341], zoom: 14, events: [] },
  { id: 'newton-ma',       label: 'Newton, MA',       postal: '02458', center: [42.3528, -71.1875], zoom: 13, events: [] },
  { id: 'waltham-ma',      label: 'Waltham, MA',      postal: '02451', center: [42.3986, -71.2451], zoom: 14, events: [] },
  { id: 'watertown-ma',    label: 'Watertown, MA',    postal: '02472', center: [42.3700, -71.1773], zoom: 14, events: [] },
  { id: 'quincy-ma',       label: 'Quincy, MA',       postal: '02169', center: [42.2491, -70.9978], zoom: 13, events: [] },
  { id: 'dedham-ma',       label: 'Dedham, MA',       postal: '02026', center: [42.2437, -71.1637], zoom: 14, events: [] },
  { id: 'needham-ma',      label: 'Needham, MA',      postal: '02492', center: [42.2798, -71.2501], zoom: 14, events: [] },
  { id: 'lexington-ma',    label: 'Lexington, MA',    postal: '02421', center: [42.4426, -71.2265], zoom: 14, events: [] },
  { id: 'arlington-ma',    label: 'Arlington, MA',    postal: '02474', center: [42.4202, -71.1565], zoom: 14, events: [] },
  { id: 'medford-ma',      label: 'Medford, MA',      postal: '02155', center: [42.4173, -71.1087], zoom: 14, events: [] },
  { id: 'malden-ma',       label: 'Malden, MA',       postal: '02148', center: [42.4291, -71.0605], zoom: 14, events: [] },
  { id: 'milton-ma',       label: 'Milton, MA',       postal: '02186', center: [42.2537, -71.0771], zoom: 14, events: [] },
  { id: 'braintree-ma',    label: 'Braintree, MA',    postal: '02184', center: [42.2093, -70.9963], zoom: 14, events: [] },
  { id: 'woburn-ma',       label: 'Woburn, MA',       postal: '01801', center: [42.4829, -71.1574], zoom: 14, events: [] },
  { id: 'burlington-ma',   label: 'Burlington, MA',   postal: '01803', center: [42.5089, -71.2004], zoom: 14, events: [] },
  { id: 'peabody-ma',      label: 'Peabody, MA',      postal: '01960', center: [42.5326, -70.9612], zoom: 13, events: [] },
]

// Default selection — Ashland, MA is just the example we open on.
export const DEFAULT_LOCATION_ID = 'ashland-ma'
