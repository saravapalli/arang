package com.arang.intake.service;

import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.arang.intake.dao.UserDao;
import com.arang.intake.model.User;

/**
 * Resolves a consumer to a {@code user} row (ARANG-SDD-P2 §2, REQ-12, DD-P2-2).
 *
 * <ul>
 *   <li>WhatsApp phone → find-or-create by {@code phone_number}.</li>
 *   <li>Web cookie id → find-or-create by {@code external_ref}.</li>
 *   <li>No cookie / no phone → <b>unrecognized</b>: no row is created until the consumer
 *       actually saves an interest (that path calls {@link #resolveWeb}).</li>
 * </ul>
 *
 * The {@code UNIQUE(external_ref)} collision from a concurrent get-or-create is handled as
 * find-or-create: catch the duplicate, re-select the existing row.
 */
@Service
public class ConsumerIdentityResolver {

    private final UserDao users;

    public ConsumerIdentityResolver(UserDao users) {
        this.users = users;
    }

    /** WhatsApp consumer: find-or-create by phone. */
    public User resolvePhone(String phoneNumber, String name) {
        return users.findByPhoneNumber(phoneNumber).orElseGet(() -> {
            User u = new User(phoneNumber, name);
            try {
                return users.insert(u);
            } catch (DuplicateKeyException race) {
                return users.findByPhoneNumber(phoneNumber).orElseThrow();
            }
        });
    }

    /** Web consumer: find-or-create by cookie id. Creates a lightweight identity row. */
    public User resolveWeb(String externalRef) {
        return users.findByExternalRef(externalRef).orElseGet(() -> {
            User u = new User();
            u.setExternalRef(externalRef);
            try {
                return users.insert(u);
            } catch (DuplicateKeyException race) {
                return users.findByExternalRef(externalRef).orElseThrow();
            }
        });
    }

    /**
     * Recognize an already-known consumer WITHOUT creating a row (REQ-12: unrecognized visitors
     * are served unfiltered and no row is created until they save an interest).
     *
     * @return the existing user, or empty if unknown / no identifier.
     */
    public Optional<User> recognize(String phoneNumber, String externalRef) {
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            return users.findByPhoneNumber(phoneNumber);
        }
        if (externalRef != null && !externalRef.isBlank()) {
            return users.findByExternalRef(externalRef);
        }
        return Optional.empty();
    }
}
