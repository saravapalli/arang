// DB verification helper — queries the Arang MySQL schema so E2E tests can assert that the UI
// actually persisted the right rows across event / event_metadata / user / user_role tables.
const mysql = require('mysql2/promise');

const cfg = {
  host: process.env.ARANG_DB_HOST || '127.0.0.1',
  port: Number(process.env.ARANG_DB_PORT || 3306),
  user: process.env.ARANG_DB_USER || 'root',
  password: process.env.ARANG_DB_PASSWORD || 'root',
  database: process.env.ARANG_DB_NAME || 'arang',
};

async function withConn(fn) {
  const conn = await mysql.createConnection(cfg);
  try {
    return await fn(conn);
  } finally {
    await conn.end();
  }
}

const db = {
  cfg,

  /** The event row by id (or null). */
  async event(id) {
    return withConn(async (c) => {
      const [rows] = await c.execute('SELECT * FROM event WHERE id = ?', [id]);
      return rows[0] || null;
    });
  },

  /** All metadata rows for an event. */
  async metadata(eventId) {
    return withConn(async (c) => {
      const [rows] = await c.execute(
        'SELECT `key`, `value`, kind, confidence, source FROM event_metadata WHERE event_id = ?',
        [eventId]);
      return rows;
    });
  },

  async user(id) {
    return withConn(async (c) => {
      const [rows] = await c.execute('SELECT * FROM `user` WHERE id = ?', [id]);
      return rows[0] || null;
    });
  },

  async rolesFor(userId) {
    return withConn(async (c) => {
      const [rows] = await c.execute('SELECT role FROM user_role WHERE user_id = ?', [userId]);
      return rows.map((r) => r.role);
    });
  },

  async business(id) {
    return withConn(async (c) => {
      const [rows] = await c.execute('SELECT * FROM business WHERE id = ?', [id]);
      return rows[0] || null;
    });
  },

  async eventCount() {
    return withConn(async (c) => {
      const [rows] = await c.execute('SELECT COUNT(*) AS n FROM event');
      return rows[0].n;
    });
  },

  /** Poll until the event row satisfies predicate (for async enrichment), or time out. */
  async waitForEvent(id, predicate, { timeoutMs = 8000, intervalMs = 400 } = {}) {
    const start = Date.now();
    for (;;) {
      const row = await db.event(id);
      if (row && predicate(row)) return row;
      if (Date.now() - start > timeoutMs) return row; // return last seen for a useful assertion msg
      await new Promise((r) => setTimeout(r, intervalMs));
    }
  },

  /** Confirm the schema/connection is reachable (used in a preflight). */
  async ping() {
    return withConn(async (c) => {
      await c.execute('SELECT 1');
      return true;
    });
  },

  /** How many waitlist signals exist for a given email (joins user → demand_signal). P2.1. */
  async waitlistCountForEmail(email) {
    return withConn(async (c) => {
      const [rows] = await c.execute(
        'SELECT COUNT(*) AS n FROM demand_signal s JOIN `user` u ON u.id = s.user_id WHERE u.email = ?',
        [email],
      );
      return Number(rows[0].n);
    });
  },
};

module.exports = { db };
