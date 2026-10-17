package com.noble.shop.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String orderNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false) private String shippingName;
    @Column(nullable = false) private String shippingPhone;
    @Column(nullable = false) private String shippingAddress;
    @Column(nullable = false) private String shippingCity;
    @Column(nullable = false) private String shippingState;
    @Column(nullable = false) private String shippingZip;
    @Column(nullable = false) private double subtotal;
    @Builder.Default private double discountAmount = 0;
    @Builder.Default private double shippingFee = 0;
    @Column(nullable = false) private double total;
    private String couponCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus paymentStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private OrderStatus status;
    private String tradeNo;
    private String trackingCompany;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    @PrePersist protected void onCreate() {
        createdAt = LocalDateTime.now();
        paymentStatus = PaymentStatus.UNPAID;
        status = OrderStatus.PENDING_PAYMENT;
    }
    public enum PaymentStatus { UNPAID, PAID, REFUNDED }
    public enum OrderStatus { PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED, REFUNDED }
}
