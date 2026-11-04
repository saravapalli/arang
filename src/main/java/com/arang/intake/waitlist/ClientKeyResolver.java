package com.arang.intake.waitlist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Picks the throttle key for a request (ARANG-SDD-P2-1 DD-P2.1-7, task T11). Behind a proxy or tunnel every request
 * has the proxy's address, so the real client address comes from a trusted header named per deployment. The AWS
 * front door is not assumed: {@code arang.site.client-ip-header} is empty by default (use the connection address).
 */
@Component
public class ClientKeyResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientKeyResolver.class);

    @Value("${arang.site.client-ip-header:}")
    private String header;

    @Value("${arang.site.public-url:http://localhost:8080}")
    private String publicUrl;

    @PostConstruct
    void warnIfPublicWithoutHeader() {
        boolean localhost = publicUrl.contains("localhost") || publicUrl.contains("127.0.0.1");
        if (!localhost && (header == null || header.isBlank())) {
            log.warn("Public site URL {} is set but no client-ip header is configured: all visitors may share one "
                + "throttle bucket. Set arang.site.client-ip-header to the trusted proxy header.", publicUrl);
        }
    }

    /** The client key: the trusted header value when configured and present, else the connection address. */
    public String keyFor(HttpServletRequest req) {
        if (header != null && !header.isBlank()) {
            String value = req.getHeader(header);
            if (value != null && !value.isBlank()) {
                int comma = value.indexOf(',');   // take the first hop if the header is a list
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        String remote = req.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
