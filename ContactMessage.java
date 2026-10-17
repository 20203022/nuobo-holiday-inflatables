package com.noble.shop.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    private String company;
    private String country;
    private String phone;
    @Column(columnDefinition = "TEXT", nullable = false) private String message;
    @Builder.Default private boolean productInquiry = false;
    private Long productId;
    private String productName;
    private String productModel;
    private Integer inquiryQuantity;
    @Column(length = 1000) private String pageUrl;
    @Builder.Default private String source = "CONTACT";
    @Builder.Default private String status = "NEW";
    @Column(columnDefinition = "TEXT") private String internalNote;
    @Column(name = "is_read")
    @Builder.Default private boolean read = false;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
