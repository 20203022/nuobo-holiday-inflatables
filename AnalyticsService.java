package com.noble.shop.service;

import com.noble.shop.model.AnalyticsEvent;
import com.noble.shop.repository.AnalyticsEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AnalyticsService {
    private final AnalyticsEventRepository repository;
    public AnalyticsService(AnalyticsEventRepository repository) { this.repository = repository; }

    @Transactional
    public void record(String eventName, String path, Long productId, String value) {
        if (eventName == null || !eventName.matches("[a-z0-9_.-]{2,80}")) return;
        if (path == null || path.isBlank()) path = "/";
        repository.save(new AnalyticsEvent(eventName, path.substring(0, Math.min(path.length(), 500)), productId,
            value == null ? null : value.substring(0, Math.min(value.length(), 120))));
    }

    public Map<String, Long> todaySummary() {
        LocalDateTime since = LocalDateTime.now().toLocalDate().atStartOfDay();
        return Map.of(
            "events", repository.countByCreatedAtAfter(since),
            "productViews", repository.countByEventNameAndCreatedAtAfter("product_view", since),
            "cartAdds", repository.countByEventNameAndCreatedAtAfter("cart_add", since),
            "inquiries", repository.countByEventNameAndCreatedAtAfter("inquiry_submit", since));
    }
}
