package com.arang.intake.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * OSM tile proxy + on-disk cache (deploy hardening for DD-P2-4). The SPA requests tiles from our
 * own origin ({@code /tiles/{z}/{x}/{y}.png}) instead of hitting the public OSM servers from every
 * visitor's browser. We:
 *   - cache each tile on disk (so repeat views of Ashland + surrounding towns don't re-fetch),
 *   - fetch cache-misses from OSM ONCE with a proper identifying User-Agent (policy compliance),
 *   - send long browser cache headers so the browser caches too.
 * This removes per-visitor OSM rate-limit exposure and cuts load/lag. A keyed provider or a
 * pre-seeded regional cache can replace the upstream without changing the SPA.
 */
@RestController
public class TileProxyController {

    private static final Logger log = LoggerFactory.getLogger(TileProxyController.class);
    private static final String UA = "Arang/0.1 (https://arang.local; contact: ops@arang.local)";

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5)).build();
    private final Path cacheDir;

    @Value("${arang.tiles.upstream:https://tile.openstreetmap.org}")
    private String upstream;

    @Value("${arang.tiles.enabled:true}")
    private boolean enabled;

    public TileProxyController(@Value("${arang.tiles.cache-dir:#{null}}") String dir) {
        this.cacheDir = (dir != null && !dir.isBlank())
            ? Path.of(dir)
            : Path.of(System.getProperty("java.io.tmpdir"), "arang-tiles");
    }

    @GetMapping(value = "/tiles/{z}/{x}/{y}.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> tile(@PathVariable int z, @PathVariable int x, @PathVariable int y) {
        // Guard against absurd coordinates.
        if (z < 0 || z > 19 || x < 0 || y < 0) {
            return ResponseEntity.badRequest().build();
        }
        try {
            byte[] bytes = cachedOrFetch(z, x, y);
            if (bytes == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("tile {}/{}/{} failed: {}", z, x, y, e.toString());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    private byte[] cachedOrFetch(int z, int x, int y) throws IOException, InterruptedException {
        Path file = cacheDir.resolve(z + "/" + x + "/" + y + ".png");
        if (Files.exists(file)) {
            return Files.readAllBytes(file); // cache hit — no OSM call
        }
        String url = upstream + "/" + z + "/" + x + "/" + y + ".png";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", UA)       // OSM policy: identify the client
            .timeout(Duration.ofSeconds(8))
            .GET().build();
        HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            log.warn("upstream tile {} returned {}", url, res.statusCode());
            return null;
        }
        byte[] body = res.body();
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, body);         // populate the on-disk cache
        } catch (IOException ignore) {
            // caching is best-effort; still serve the bytes
        }
        return body;
    }
}
