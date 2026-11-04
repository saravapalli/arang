package com.arang.intake.config;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Passcode gate for the PRODUCER surface, so the public consumer site (arangly.com) does not let just
 * anyone post events. Guards the producer screen ({@code /add}), the direct web form ({@code /submit}), the
 * producer console ({@code /console}), and the producer API ({@code /api/producer/**}).
 *
 * <p>Prototype-grade: a single shared passcode (env {@code ARANG_PRODUCER_KEY}). A visitor without a
 * valid {@code producer_key} cookie gets a small passcode page; submitting the correct passcode sets
 * the cookie and lets them through. The consumer app (/, /api/discovery, /api/interests, /tiles,
 * /media, /review, /towns, /sitemap.xml) is unaffected. This is NOT per-user auth — that is the later
 * user_role work.
 *
 * <p>If {@code ARANG_PRODUCER_KEY} is blank/unset, the gate is DISABLED (open) so local dev is
 * unchanged; set the env var in the deployed/tunnelled run to lock it down.
 */
@Component
public class ProducerGateFilter implements Filter {

    private static final String COOKIE = "producer_key";

    @Value("${arang.producer.key:}")
    private String producerKey;

    private static boolean isGuarded(String path) {
        return path.equals("/add") || path.startsWith("/add/")
            || path.equals("/submit") || path.startsWith("/submit/")
            || path.equals("/console") || path.startsWith("/console/")
            || path.startsWith("/api/producer/");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        HttpServletResponse w = (HttpServletResponse) res;

        // Gate disabled when no key configured (dev).
        if (producerKey == null || producerKey.isBlank()) {
            chain.doFilter(req, res);
            return;
        }

        String path = r.getRequestURI();
        if (!isGuarded(path)) {
            chain.doFilter(req, res);
            return;
        }

        // A submitted passcode (?key=...) sets the cookie then redirects clean.
        String submitted = r.getParameter("key");
        if (submitted != null && submitted.equals(producerKey)) {
            Cookie c = new Cookie(COOKIE, producerKey);
            c.setPath("/");
            c.setMaxAge(60 * 60 * 24 * 30); // 30 days
            c.setHttpOnly(true);
            w.addCookie(c);
            w.sendRedirect("/add");
            return;
        }

        // Already holding a valid cookie → allow.
        if (r.getCookies() != null) {
            for (Cookie c : r.getCookies()) {
                if (COOKIE.equals(c.getName()) && producerKey.equals(c.getValue())) {
                    chain.doFilter(req, res);
                    return;
                }
            }
        }

        // No/invalid key. API and any non-GET (e.g. POST /submit) are refused outright; a GET of a
        // producer page gets the passcode form.
        if (path.startsWith("/api/producer/") || !"GET".equalsIgnoreCase(r.getMethod())) {
            w.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            w.setContentType("application/json");
            w.getWriter().write("{\"error\":\"producer_locked\"}");
            return;
        }
        w.setStatus(HttpServletResponse.SC_OK);
        w.setContentType("text/html;charset=UTF-8");
        w.getWriter().write(passcodePage(submitted != null));
    }

    private String passcodePage(boolean wrong) {
        String err = wrong
            ? "<p style=\"color:#b23b2e;font-size:13px;margin:0 0 14px\">That passcode didn't match. Try again.</p>"
            : "";
        return """
            <!doctype html><html lang="en"><head><meta charset="UTF-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            <title>Arangly — organizers</title>
            <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;600;700&family=Manrope:wght@700;800&display=swap" rel="stylesheet"/>
            <style>
              *{box-sizing:border-box}body{margin:0;min-height:100vh;display:grid;place-items:center;
                font-family:'DM Sans',sans-serif;background:
                radial-gradient(900px 420px at 50% 18%,#fbe6d7 0,rgba(251,230,215,0) 60%),#f5f3ed;color:#22263b}
              .card{width:min(420px,calc(100% - 40px));padding:32px;border:1px solid #d9d6cc;border-radius:16px;
                background:#fbfaf6;box-shadow:0 18px 50px rgba(40,39,34,.08);text-align:center}
              .logo{display:inline-flex;align-items:center;gap:9px;font-family:Manrope;font-size:20px;font-weight:800;letter-spacing:-.05em}
              .flower{position:relative;width:24px;height:24px;transform:rotate(45deg)}
              .flower i{position:absolute;width:12px;height:12px;border-radius:7px 7px 2px 7px;background:#ff694a}
              .flower i:nth-child(2){top:0;right:0;transform:rotate(90deg);background:#4951d7}
              .flower i:nth-child(3){bottom:0;right:0;background:#f7b632}.flower i:nth-child(4){bottom:0;left:0;background:#4aa47b}
              h1{margin:22px 0 6px;font-family:Manrope;font-size:24px;letter-spacing:-.03em}
              p.sub{margin:0 0 20px;color:#6e716d;font-size:14px}
              input{width:100%;height:48px;padding:0 14px;border:1px solid #d8d5cc;border-radius:10px;font:inherit;font-size:15px;margin-bottom:12px}
              button{width:100%;height:48px;border:0;border-radius:10px;background:#252a43;color:#fff;font:inherit;font-size:14px;font-weight:700;cursor:pointer}
            </style></head><body>
            <form class="card" method="get" action="/add">
              <span class="logo"><span class="flower"><i></i><i></i><i></i><i></i></span>arangly</span>
              <h1>Organizers only</h1>
              <p class="sub">Adding events to Arangly is for approved local organizers. Enter your passcode.</p>
              %ERR%
              <input type="password" name="key" placeholder="Passcode" autofocus/>
              <button type="submit">Continue</button>
            </form></body></html>
            """.replace("%ERR%", err);
    }
}
