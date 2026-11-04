package com.arang.intake.waitlist;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.arang.intake.dao.DemandSignalDao;
import com.arang.intake.dao.UserDao;
import com.arang.intake.geo.GeoPlaceSource;
import com.arang.intake.town.TownRegistry;

/**
 * Records a waitlist demand signal (ARANG-SDD-P2-1 §4.3, task T10). Validates the email and place, finds or
 * creates the user by email (race-safe, DD-P2.1-12), inserts the signal (idempotent), and never writes an email
 * address to the log — not even in a database error message (REQ-P2.1-15).
 */
@Service
public class WaitlistService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistService.class);

    // One address: no whitespace/control chars, exactly one @, non-empty local and domain parts.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@\\x00-\\x1f]+@[^\\s@\\x00-\\x1f]+$");
    private static final Pattern SLUG_SHAPED = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*-ma");
    private static final int MAX_EMAIL = 254;
    private static final int MAX_PLACE = 80;

    /** Outcome of a save attempt, mapped to an HTTP response by the controller. */
    public enum Result { SAVED, INVALID_EMAIL, INVALID_PLACE, NOT_SAVED }

    private final UserDao users;
    private final DemandSignalDao signals;
    private final TownRegistry towns;
    private final GeoPlaceSource geo;

    @Value("${arang.waitlist.consent-version:v1}")
    private String consentVersion;

    @Value("${arang.waitlist.consent-text:We'll only email you about Arangly in <town>.}")
    private String consentTemplate;

    public WaitlistService(UserDao users, DemandSignalDao signals, TownRegistry towns, GeoPlaceSource geo) {
        this.users = users;
        this.signals = signals;
        this.towns = towns;
        this.geo = geo;
    }

    /**
     * Save a waitlist signal for the given place and email. The place is a town slug the visitor picked, or free
     * text they typed. A live town's slug is rejected (a served town has no waitlist).
     */
    @Transactional
    public Result save(String rawPlace, String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim();
        if (email.length() > MAX_EMAIL || !EMAIL.matcher(email).matches()) {
            return Result.INVALID_EMAIL;
        }
        email = email.toLowerCase();

        String place = rawPlace == null ? "" : rawPlace.trim();
        if (place.isEmpty() || place.length() > MAX_PLACE) {
            return Result.INVALID_PLACE;
        }

        PlaceRef ref = resolvePlace(place);
        if (ref == null) {
            return Result.INVALID_PLACE;
        }

        String consentText = consentTemplate.replace("<town>", ref.label());
        try {
            long userId = users.findOrCreateByEmail(email);
            signals.upsert(userId, ref.key(), ref.label(), consentVersion, consentText);
            return Result.SAVED;
        } catch (DataAccessException e) {
            // Never log the exception or its message: MySQL duplicate-key text contains the email (REQ-15).
            log.error("Waitlist save failed: {}", e.getClass().getSimpleName());
            return Result.NOT_SAVED;
        }
    }

    private record PlaceRef(String key, String label) {}

    private PlaceRef resolvePlace(String place) {
        // A live town's slug has no waitlist.
        if (towns.bySlug(place).filter(t -> t.isLive()).isPresent()) {
            return null;
        }
        // A slug in the place file → coming-soon town; use the slug and its display name.
        if (geo.isUp()) {
            var p = geo.bySlug(place);
            if (p.isPresent()) {
                return new PlaceRef(p.get().slug(), p.get().name() + ", " + p.get().state());
            }
        }
        // Place data down but the text has the slug shape → keep it as a slug so it matches when the town is added.
        if (SLUG_SHAPED.matcher(place).matches()) {
            return new PlaceRef(place, place);
        }
        // Otherwise free text: store it as typed.
        String collapsed = place.replaceAll("\\s+", " ");
        return new PlaceRef("text:" + collapsed.toLowerCase(), collapsed);
    }
}
