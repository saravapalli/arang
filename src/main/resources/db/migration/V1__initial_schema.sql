-- =====================================================================
-- Arang — FULL SCHEMA (V1). Authored once, up front, to avoid phase-driven
-- migrations to primary tables (per .kiro/steering/design-philosophy.md:
-- "the schema is the deliberate, long-lived artifact").
-- Mirrors docs/producer-intake/sdd.md §1. Grouped by concern.
-- [P1] used now · [P2]/[P3] structure now, populated when that phase lands.
-- H2 MySQL-mode compatible for the test profile.
-- =====================================================================

-- ---------- IDENTITY & ROLE -------------------------------------------
CREATE TABLE `user` (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone_number  VARCHAR(32) UNIQUE,
  name          VARCHAR(255),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_role (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  role          VARCHAR(32) NOT NULL,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, role),
  FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);

CREATE TABLE user_metadata (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  `key`         VARCHAR(100) NOT NULL,
  `value`       TEXT,
  kind          VARCHAR(16),
  confidence    DECIMAL(3,2),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);

-- ---------- BUSINESS (primary entity) ---------------------------------
CREATE TABLE business (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(255) NOT NULL,
  owner_user_id BIGINT,
  location      VARCHAR(255),
  postal_code   VARCHAR(16),
  country_code  CHAR(2) DEFAULT 'US',
  latitude      DECIMAL(9,6),
  longitude     DECIMAL(9,6),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (owner_user_id) REFERENCES `user`(id)
);

CREATE TABLE business_metadata (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_id   BIGINT NOT NULL,
  `key`         VARCHAR(100) NOT NULL,
  `value`       TEXT,
  kind          VARCHAR(16),
  confidence    DECIMAL(3,2),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE
);

-- ---------- PROVENANCE / ORIGIN ---------------------------------------
CREATE TABLE ingestion_source (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  name             VARCHAR(255) NOT NULL,
  connector_shape  VARCHAR(32) NOT NULL,
  location         VARCHAR(255),
  url_or_reference VARCHAR(512),
  enabled          BOOLEAN NOT NULL DEFAULT TRUE,
  last_run_at      DATETIME,
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE ingestion_source_metadata (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  ingestion_source_id BIGINT NOT NULL,
  `key`               VARCHAR(100) NOT NULL,
  `value`             TEXT,
  kind                VARCHAR(16),
  created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (ingestion_source_id) REFERENCES ingestion_source(id) ON DELETE CASCADE
);

CREATE TABLE api_client (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(255) NOT NULL,
  api_key_hash  VARCHAR(128),
  enabled       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------- EVENT (the universal unit) --------------------------------
CREATE TABLE event_type (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(50) NOT NULL UNIQUE,
  description   VARCHAR(255)
);

CREATE TABLE event (
  id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_type_id         BIGINT,
  business_id           BIGINT,
  title                 VARCHAR(255),
  starts_at             DATETIME,
  ends_at               DATETIME,
  location              VARCHAR(255),
  postal_code           VARCHAR(16),
  country_code          CHAR(2) NOT NULL DEFAULT 'US',
  latitude              DECIMAL(9,6),
  longitude             DECIMAL(9,6),
  source_type           VARCHAR(16) NOT NULL,
  raw_message           TEXT,
  media_ref             VARCHAR(512),
  extracted_confidence  DECIMAL(3,2),
  status                VARCHAR(16) NOT NULL DEFAULT 'draft',
  origin_type           VARCHAR(16) NOT NULL,
  submitted_by          BIGINT,
  ingestion_source_id   BIGINT,
  api_client_id         BIGINT,
  edit_token            CHAR(43) UNIQUE,
  edit_token_expires_at DATETIME,
  created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (event_type_id)       REFERENCES event_type(id),
  FOREIGN KEY (business_id)         REFERENCES business(id),
  FOREIGN KEY (submitted_by)        REFERENCES `user`(id),
  FOREIGN KEY (ingestion_source_id) REFERENCES ingestion_source(id),
  FOREIGN KEY (api_client_id)       REFERENCES api_client(id)
);
CREATE INDEX idx_event_status     ON event(status);
CREATE INDEX idx_event_starts_at  ON event(starts_at);
CREATE INDEX idx_event_postal     ON event(postal_code, country_code);
CREATE INDEX idx_event_edit_token ON event(edit_token);
CREATE INDEX idx_event_business   ON event(business_id);
CREATE INDEX idx_event_type       ON event(event_type_id);
CREATE INDEX idx_event_origin     ON event(origin_type);

CREATE TABLE event_metadata (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id      BIGINT NOT NULL,
  `key`         VARCHAR(100) NOT NULL,
  `value`       TEXT,
  kind          VARCHAR(16),
  confidence    DECIMAL(3,2),
  source        VARCHAR(16),                        -- ocr|text|url (per-field provenance; resolves OQ-P1-6)
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);
CREATE INDEX idx_evmeta_event ON event_metadata(event_id);
CREATE INDEX idx_evmeta_key   ON event_metadata(`key`);

-- typed-core per-field confidence (DD-1 option a: no core-table bloat)
CREATE TABLE event_field_confidence (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id      BIGINT NOT NULL,
  field         VARCHAR(50) NOT NULL,
  confidence    DECIMAL(3,2),
  UNIQUE (event_id, field),
  FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);

-- ---------- DISCOVERY VOCABULARY [P2] ---------------------------------
CREATE TABLE category (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(80) NOT NULL UNIQUE,
  description   VARCHAR(255)
);

CREATE TABLE tag (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE event_category (
  event_id      BIGINT NOT NULL,
  category_id   BIGINT NOT NULL,
  PRIMARY KEY (event_id, category_id),
  FOREIGN KEY (event_id)    REFERENCES event(id) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
);

CREATE TABLE event_tag (
  event_id      BIGINT NOT NULL,
  tag_id        BIGINT NOT NULL,
  PRIMARY KEY (event_id, tag_id),
  FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id)   REFERENCES tag(id) ON DELETE CASCADE
);

CREATE TABLE interest (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  category_id   BIGINT,
  tag_id        BIGINT,
  postal_code   VARCHAR(16),
  weight        DECIMAL(4,3) DEFAULT 1.000,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id)     REFERENCES `user`(id) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES category(id),
  FOREIGN KEY (tag_id)      REFERENCES tag(id)
);

-- ---------- FEEDBACK [P2] ---------------------------------------------
CREATE TABLE feedback (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT,
  event_id      BIGINT,
  `signal`      VARCHAR(32) NOT NULL,              -- reserved word in MySQL → quoted
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id)  REFERENCES `user`(id) ON DELETE SET NULL,
  FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);

-- ---------- CONVERSATION / MESSAGE TRAIL [P2 conversational intake] ----
CREATE TABLE conversation (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT,
  event_id      BIGINT,
  state         VARCHAR(32),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id)  REFERENCES `user`(id),
  FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE SET NULL
);

CREATE TABLE message (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id     BIGINT,
  direction           VARCHAR(8) NOT NULL,
  body                TEXT,
  provider_message_id VARCHAR(128),
  created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE
);

-- ---------- IDEMPOTENCY [P1] ------------------------------------------
CREATE TABLE processed_message (
  provider_message_id VARCHAR(128) PRIMARY KEY,
  event_id            BIGINT,
  processed_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
