-- =====================================================================
-- Arang — V2 (P2 Consumer Discovery). Realizes ARANG-SDD-P2 §1 / task T1.
-- Additive only: seeds the curated category vocabulary, adds a web-identity
-- key column to `user`, and a composite index the discovery query reads.
-- No primary table is created (per design-philosophy: new use cases = new data).
-- H2 (MySQL mode, test profile) and MySQL compatible.
-- =====================================================================

-- ---------- Web consumer identity (SDD DD-P2-2) -----------------------
-- Nullable natural key for a web cookie id. WhatsApp users keep phone_number;
-- web users are keyed by external_ref. Find-or-create; UNIQUE guards duplicates.
ALTER TABLE `user` ADD COLUMN external_ref VARCHAR(128) NULL UNIQUE;

-- ---------- Discovery query index (SDD §1) ----------------------------
-- The discovery query filters published events in a time window:
--   WHERE status = 'published' AND starts_at BETWEEN ? AND ?
-- A composite (status, starts_at) serves that predicate better than the two
-- single-column P1 indexes alone.
CREATE INDEX idx_event_pub_starts ON event (status, starts_at);

-- ---------- Curated category vocabulary (SDD §1, OQ-P2-6) -------------
-- 15 curated categories = the deliberate MVP spine (PRD/TRD allow ~15-25, fixed MVP).
-- INSERT only; `category` table + its UNIQUE(name) were created in V1.
INSERT INTO category (name, description) VALUES
  ('music',     'Live music, concerts, DJ sets'),
  ('food',      'Food, drink, markets, tastings'),
  ('family',    'Family- and kid-friendly'),
  ('arts',      'Arts, crafts, exhibitions'),
  ('outdoor',   'Hikes, parks, outdoor activity'),
  ('sports',    'Sports and fitness'),
  ('workshop',  'Classes and workshops'),
  ('nightlife', 'Nightlife and bars'),
  ('theatre',   'Theatre and performance'),
  ('film',      'Film and screenings'),
  ('community', 'Community and civic'),
  ('wellness',  'Wellness and health'),
  ('market',    'Markets and fairs'),
  ('offer',     'Deals and offers'),
  ('other',     'Uncategorized / other');
