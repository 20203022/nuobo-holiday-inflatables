package com.noble.shop.repository;

import com.noble.shop.model.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {
    long countByCreatedAtAfter(LocalDateTime since);
    long countByEventNameAndCreatedAtAfter(String eventName, LocalDateTime since);
}
