package com.arang.intake.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.dao.UserDao;

/**
 * TC-P2.1-033 (ARANG-TTP-P2-1, task T9): the atomic find-or-create by email is race-safe. This must run on real
 * MySQL (REPEATABLE READ), not H2 — a look-up/insert/re-read approach can fail there (DD-P2.1-12). Tagged
 * {@code mysql-it}; run with the {@code mysqlIntegrationTest} Gradle task against {@code arang_dev} (TTP-D3).
 */
@Tag("mysql-it")
@SpringBootTest
@ActiveProfiles("mysql-it")
class WaitlistConcurrencyMySqlIT {

    @Autowired
    UserDao users;
    @Autowired
    JdbcTemplate jdbc;

    private static final String EMAIL = "concurrency-it@example.com";

    @BeforeEach
    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM `user` WHERE email = ?", EMAIL);
    }

    @Test
    void parallelFindOrCreateByEmailMakesExactlyOneUser() throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> users.findOrCreateByEmail(EMAIL));
        }
        List<Future<Long>> results = pool.invokeAll(tasks);
        pool.shutdown();

        List<Long> ids = new ArrayList<>();
        for (Future<Long> f : results) {
            ids.add(f.get());
        }
        // All callers see the same id, and the table holds exactly one row.
        assertThat(ids).allMatch(id -> id.equals(ids.get(0)));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user` WHERE email = ?", Integer.class, EMAIL))
            .isEqualTo(1);
    }
}
