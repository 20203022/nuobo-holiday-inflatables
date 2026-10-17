package com.noble.shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.time.Duration;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        CacheControl cache = CacheControl.maxAge(Duration.ofDays(30)).cachePublic();
        add(registry, "/css/**", "classpath:/static/css/", cache);
        add(registry, "/js/**", "classpath:/static/js/", cache);
        add(registry, "/images/**", "classpath:/static/images/", cache);
        add(registry, "/fonts/**", "classpath:/static/fonts/", cache);
        add(registry, "/videos/**", "classpath:/static/videos/", cache);
    }

    private void add(ResourceHandlerRegistry registry, String pattern, String location, CacheControl cache) {
        registry.addResourceHandler(pattern).addResourceLocations(location).setCacheControl(cache);
    }
}
