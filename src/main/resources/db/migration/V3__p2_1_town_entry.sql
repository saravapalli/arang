-- =====================================================================
-- Arangly — V3 (P2.1 Town Entry & Coverage). Realizes ARANG-SDD-P2-1 §1.1 / task T3.
-- Additive only: one column on `user` and three new tables. No change to event or any
-- other P1/P2 table. H2 (MySQL mode, test profile) and MySQL compatible.
-- =====================================================================

-- ---------- Coverage registry (SDD DD-P2.1-2) ------------------------
-- Which towns Arangly serves. A town going live is an UPDATE, not a release.
-- 'coming soon' is NOT stored: it is derived (a resolvable place with no live town row).
CREATE TABLE town (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  slug          VARCHAR(80)  NOT NULL UNIQUE,
  name          VARCHAR(80)  NOT NULL,
  state         CHAR(2)      NOT NULL,
  postal_code   VARCHAR(16)  NOT NULL,
  latitude      DECIMAL(9,6) NOT NULL,
  longitude     DECIMAL(9,6) NOT NULL,
  map_zoom      TINYINT      NOT NULL DEFAULT 14,
  status        VARCHAR(16)  NOT NULL DEFAULT 'live',
  popular_rank  INT          NULL,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE INDEX idx_town_status ON town (status);

-- Old slugs keep working after a rename or merge: /towns/<alias> -> 301 -> /towns/<slug>.
CREATE TABLE town_slug_alias (
  alias    VARCHAR(80) PRIMARY KEY,
  town_id  BIGINT NOT NULL,
  FOREIGN KEY (town_id) REFERENCES town(id) ON DELETE CASCADE
);

-- ---------- Email as identity (SDD DD-P2.1-1) ------------------------
-- Contact identity, parallel to phone_number. Stored lowercase. Nullable: most users have none.
ALTER TABLE `user` ADD COLUMN email VARCHAR(254) NULL UNIQUE;

-- ---------- Demand signal (SDD DD-P2.1-1, DD-P2.1-14) ----------------
-- A recorded wish, pointing at the person. 'kind' is data: a new signal type is a new value.
-- verified_at NULL = unverified (always NULL in P2.1).
CREATE TABLE demand_signal (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id          BIGINT       NOT NULL,
  kind             VARCHAR(32)  NOT NULL DEFAULT 'town_waitlist',
  place_key        VARCHAR(120) NOT NULL,
  place_label      VARCHAR(80)  NOT NULL,
  consent_version  VARCHAR(16)  NOT NULL,
  consent_text     VARCHAR(500) NOT NULL,
  verified_at      TIMESTAMP    NULL,
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_demand UNIQUE (user_id, kind, place_key),
  FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);
CREATE INDEX idx_demand_place   ON demand_signal (kind, place_key);
CREATE INDEX idx_demand_created ON demand_signal (created_at);

-- ---------- Seed: the 28 live towns (all Massachusetts) --------------
-- Copied once from the SPA's former hard-coded list (web/src/data/events.js, commit 4eb964b).
-- popular_rank 1-6 = the current landing chips. After this, the SPA reads towns from the server.
INSERT INTO town (slug, name, state, postal_code, latitude, longitude, map_zoom, status, popular_rank) VALUES
  ('ashland-ma', 'Ashland', 'MA', '01721', 42.253900, -71.458300, 14, 'live', 1),
  ('framingham-ma', 'Framingham', 'MA', '01701', 42.300700, -71.425500, 13, 'live', 2),
  ('holliston-ma', 'Holliston', 'MA', '01746', 42.202600, -71.436100, 14, 'live', NULL),
  ('hopkinton-ma', 'Hopkinton', 'MA', '01748', 42.219000, -71.530200, 14, 'live', NULL),
  ('natick-ma', 'Natick', 'MA', '01760', 42.287500, -71.357400, 14, 'live', 3),
  ('sherborn-ma', 'Sherborn', 'MA', '01770', 42.233100, -71.378700, 14, 'live', NULL),
  ('wellesley-ma', 'Wellesley', 'MA', '02481', 42.310600, -71.274700, 14, 'live', NULL),
  ('marlborough-ma', 'Marlborough', 'MA', '01752', 42.350900, -71.543400, 13, 'live', NULL),
  ('sudbury-ma', 'Sudbury', 'MA', '01776', 42.383700, -71.428200, 14, 'live', NULL),
  ('boston-ma', 'Boston', 'MA', '02108', 42.357600, -71.068400, 13, 'live', 4),
  ('cambridge-ma', 'Cambridge', 'MA', '02139', 42.364700, -71.104200, 14, 'live', 5),
  ('somerville-ma', 'Somerville', 'MA', '02143', 42.382900, -71.102800, 14, 'live', NULL),
  ('brookline-ma', 'Brookline', 'MA', '02445', 42.325900, -71.134100, 14, 'live', NULL),
  ('newton-ma', 'Newton', 'MA', '02458', 42.352800, -71.187500, 13, 'live', 6),
  ('waltham-ma', 'Waltham', 'MA', '02451', 42.398600, -71.245100, 14, 'live', NULL),
  ('watertown-ma', 'Watertown', 'MA', '02472', 42.370000, -71.177300, 14, 'live', NULL),
  ('quincy-ma', 'Quincy', 'MA', '02169', 42.249100, -70.997800, 13, 'live', NULL),
  ('dedham-ma', 'Dedham', 'MA', '02026', 42.243700, -71.163700, 14, 'live', NULL),
  ('needham-ma', 'Needham', 'MA', '02492', 42.279800, -71.250100, 14, 'live', NULL),
  ('lexington-ma', 'Lexington', 'MA', '02421', 42.442600, -71.226500, 14, 'live', NULL),
  ('arlington-ma', 'Arlington', 'MA', '02474', 42.420200, -71.156500, 14, 'live', NULL),
  ('medford-ma', 'Medford', 'MA', '02155', 42.417300, -71.108700, 14, 'live', NULL),
  ('malden-ma', 'Malden', 'MA', '02148', 42.429100, -71.060500, 14, 'live', NULL),
  ('milton-ma', 'Milton', 'MA', '02186', 42.253700, -71.077100, 14, 'live', NULL),
  ('braintree-ma', 'Braintree', 'MA', '02184', 42.209300, -70.996300, 14, 'live', NULL),
  ('woburn-ma', 'Woburn', 'MA', '01801', 42.482900, -71.157400, 14, 'live', NULL),
  ('burlington-ma', 'Burlington', 'MA', '01803', 42.508900, -71.200400, 14, 'live', NULL),
  ('peabody-ma', 'Peabody', 'MA', '01960', 42.532600, -70.961200, 13, 'live', NULL);
