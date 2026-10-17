package com.noble.shop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analytics_events")
public class AnalyticsEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80) private String eventName;
    @Column(nullable = false, length = 500) private String path;
    private Long productId;
    @Column(length = 120) private String value;
    @Column(nullable = false) private LocalDateTime createdAt;
    public AnalyticsEvent() {}
    public AnalyticsEvent(String eventName, String path, Long productId, String value) {
        this.eventName = eventName; this.path = path; this.productId = productId; this.value = value;
    }
    public String getEventName() { return eventName; }
    public void setEventName(String value) { eventName = value; }
    public String getPath() { return path; }
    public void setPath(String value) { path = value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId = value; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
