package com.noble.shop.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String nameCn;
    @Column(nullable = false) private String nameEn;
    @Column(nullable = false, unique = true) private String model;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Category category;
    @Column(nullable = false) private double originalPrice;
    private Double discountPrice;
    @Column(nullable = false) private int stock;
    @Column(columnDefinition = "TEXT") private String descriptionCn;
    @Column(columnDefinition = "TEXT") private String descriptionEn;
    private String featureTagsCn;
    private String featureTagsEn;
    private String sizeInfo;
    @Builder.Default private boolean published = true;
    private String seoTitle;
    private String seoDescription;
    @Builder.Default private int salesCount = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC") @Builder.Default
    @BatchSize(size = 50)
    private List<ProductImage> images = new ArrayList<>();
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
    @PostLoad private void onLoad() { if (images == null) images = new ArrayList<>(); }
    public double getDisplayPrice() { return discountPrice != null ? discountPrice : originalPrice; }
    public boolean hasDiscount() { return discountPrice != null && discountPrice < originalPrice; }
    public enum Category { CHRISTMAS, HALLOWEEN }
}
