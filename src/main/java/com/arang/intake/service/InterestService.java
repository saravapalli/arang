package com.arang.intake.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.arang.intake.dao.InterestDao;

/**
 * Consumer interest capture (ARANG-SDD-P2 §2, REQ-10, REQ-11).
 * Saving validates each category against the curated vocabulary; an off-vocabulary category is
 * rejected. (Tags are open/mintable and are not handled here — REQ-16.)
 */
@Service
public class InterestService {

    /** Thrown when a saved category is not in the curated vocabulary (REQ-11) → controller 400. */
    public static class UnknownCategoryException extends RuntimeException {
        public UnknownCategoryException(String name) { super("unknown category: " + name); }
    }

    private final InterestDao interests;

    public InterestService(InterestDao interests) {
        this.interests = interests;
    }

    public List<String> get(long userId) {
        return interests.findCategoryNames(userId);
    }

    /** Validate every category against the vocabulary, then replace the consumer's interests. */
    @Transactional
    public void save(long userId, List<String> categoryNames) {
        List<Long> ids = new ArrayList<>();
        for (String name : categoryNames) {
            long id = interests.categoryIdByName(name)
                .orElseThrow(() -> new UnknownCategoryException(name)); // REQ-11
            ids.add(id);
        }
        interests.replaceCategories(userId, ids);
    }
}
