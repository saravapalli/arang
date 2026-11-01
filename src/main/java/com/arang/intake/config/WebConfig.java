package com.arang.intake.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.arang.intake.media.MediaStore;

/** Serves stored media at {@code /media/**} and forwards SPA client routes to the SPA (DD-P2-6). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final MediaStore mediaStore;

    public WebConfig(MediaStore mediaStore) {
        this.mediaStore = mediaStore;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
            .addResourceLocations(mediaStore.directory().toUri().toString());
    }

    /**
     * SPA client-side routes served by the same index.html (the React app routes internally).
     * {@code /add} is the producer intake screen. API, /console, /review, /submit, /media, and
     * static assets are handled by their own mappings/handlers and are unaffected.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/add").setViewName("forward:/index.html");
    }
}
